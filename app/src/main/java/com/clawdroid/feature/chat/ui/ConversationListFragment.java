package com.clawdroid.feature.chat.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.clawdroid.core.data.db.entity.ConversationEntity;
import com.clawdroid.core.data.db.entity.PersonaEntity;
import com.clawdroid.app.R;
import com.clawdroid.feature.chat.adapter.ConversationAdapter;
import com.clawdroid.app.databinding.FragmentConversationListBinding;
import com.clawdroid.feature.chat.viewmodel.ConversationListViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ConversationListFragment extends Fragment {

    private FragmentConversationListBinding binding;
    private ConversationListViewModel viewModel;
    private ConversationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentConversationListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ConversationListViewModel.class);

        setupRecyclerView();
        setupObservers();
        setupActions();
    }

    private void setupRecyclerView() {
        adapter = new ConversationAdapter();
        binding.recyclerConversations.setAdapter(adapter);

        adapter.setOnClickListener(new ConversationAdapter.OnConversationClickListener() {
            @Override
            public void onClick(ConversationEntity conversation) {
                Bundle args = new Bundle();
                args.putString("conversationId", conversation.getId());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_conversationList_to_chat, args);
            }

            @Override
            public void onLongClick(ConversationEntity conversation) {
                showContextMenu(conversation);
            }

            @Override
            public void onDeleteClick(ConversationEntity conversation) {
                showDeleteConfirmDialog(conversation.getId());
            }

            @Override
            public void onRenameClick(ConversationEntity conversation) {
                showRenameDialog(conversation);
            }
        });
    }

    private void showContextMenu(ConversationEntity conversation) {
        String[] items = conversation.getIsPinned() == 1
                ? new String[]{"고정 해제", "보관", "이름 수정", "삭제"}
                : new String[]{"고정", "보관", "이름 수정", "삭제"};

        new AlertDialog.Builder(requireContext())
                .setTitle(conversation.getTitle() != null ? conversation.getTitle() : "대화")
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            viewModel.togglePin(conversation.getId(), conversation.getIsPinned() == 0);
                            break;
                        case 1:
                            viewModel.archiveConversation(conversation.getId());
                            break;
                        case 2:
                            showRenameDialog(conversation);
                            break;
                        case 3:
                            showDeleteConfirmDialog(conversation.getId());
                            break;
                    }
                })
                .show();
    }

    private void showRenameDialog(ConversationEntity conversation) {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(conversation.getTitle());
        input.selectAll();
        int padding = (int) (12 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(requireContext())
                .setTitle("이름 수정")
                .setView(input)
                .setPositiveButton("저장", (d, w) -> {
                    String newTitle = input.getText().toString().trim();
                    if (!newTitle.isEmpty()) {
                        viewModel.renameConversation(conversation.getId(), newTitle);
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showDeleteConfirmDialog(String conversationId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("채팅 삭제")
                .setMessage("삭제하겠습니까?")
                .setPositiveButton("삭제", (d, w) -> viewModel.deleteConversation(conversationId))
                .setNegativeButton("취소", null)
                .show();
    }

    private void setupObservers() {
        viewModel.getConversations().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            binding.emptyState.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            binding.recyclerConversations.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        });

        viewModel.getNavigateToConversationId().observe(getViewLifecycleOwner(), convId -> {
            if (convId != null) {
                viewModel.clearNavigateToConversation();
                Bundle args = new Bundle();
                args.putString("conversationId", convId);
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_conversationList_to_chat, args);
            }
        });
    }

    private void setupActions() {
        binding.fabNewConversation.setOnClickListener(v -> showNewChatDialog());

        binding.btnNewChat.setOnClickListener(v -> showNewChatDialog());

        binding.btnSearch.setOnClickListener(v -> toggleSearch());
    }

    private void showNewChatDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_new_conversation, null);

        TextInputEditText etChatName = dialogView.findViewById(R.id.etChatName);
        AutoCompleteTextView actvPersona = dialogView.findViewById(R.id.actvPersona);

        // 페르소나 목록 구성 (없음 + 등록된 페르소나들)
        java.util.List<PersonaEntity> personaList = viewModel.getPersonas().getValue();
        String[] personaNames;
        int activeIndex = 0;
        if (personaList == null || personaList.isEmpty()) {
            personaNames = new String[]{"없음"};
        } else {
            personaNames = new String[personaList.size() + 1];
            personaNames[0] = "없음";
            for (int i = 0; i < personaList.size(); i++) {
                personaNames[i + 1] = personaList.get(i).getName();
                if (personaList.get(i).isActive()) activeIndex = i + 1;
            }
        }

        ArrayAdapter<String> personaAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, personaNames);
        actvPersona.setAdapter(personaAdapter);
        actvPersona.setText(personaNames[activeIndex], false);

        etChatName.setText("새 대화");
        etChatName.selectAll();

        new AlertDialog.Builder(requireContext())
                .setTitle("새 체팅 만들기")
                .setView(dialogView)
                .setPositiveButton("시작", (d, w) -> {
                    String title = etChatName.getText() != null
                            ? etChatName.getText().toString().trim() : "";
                    if (title.isEmpty()) title = "새 대화";

                    String selected = actvPersona.getText().toString();
                    String systemPrompt = null;
                    if (!"없음".equals(selected) && personaList != null) {
                        for (PersonaEntity p : personaList) {
                            if (p.getName().equals(selected)) {
                                systemPrompt = p.getSystemPrompt();
                                break;
                            }
                        }
                    }
                    viewModel.createNewConversation(title, null, null, systemPrompt);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void toggleSearch() {
        boolean visible = binding.searchBar.getVisibility() == View.VISIBLE;
        if (visible) {
            binding.searchBar.setVisibility(View.GONE);
            viewModel.clearSearch();
        } else {
            binding.searchBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
