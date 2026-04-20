package com.clawdroid.feature.chat.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.clawdroid.core.data.db.entity.ConversationEntity;
import com.clawdroid.app.R;
import com.clawdroid.feature.chat.adapter.ConversationAdapter;
import com.clawdroid.app.databinding.FragmentConversationListBinding;
import com.clawdroid.feature.chat.viewmodel.ConversationListViewModel;
import com.google.android.material.checkbox.MaterialCheckBox;
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
        MaterialCheckBox cbPersona = dialogView.findViewById(R.id.cbPersona);

        // 페르소나 이름을 체크박스 라벨에 동적 설정
        String personaName = viewModel.getPersonaName();
        cbPersona.setText("\ud398\ub974\uc18c\ub098 \uc801\uc6a9 (" + personaName + ")");

        etChatName.setText("\uc0c8 \ub300\ud654");
        etChatName.selectAll();

        new AlertDialog.Builder(requireContext())
                .setTitle("\uc0c8 \uccb4\ud305 \ub9cc\ub4e4\uae30")
                .setView(dialogView)
                .setPositiveButton("\uc2dc\uc791", (d, w) -> {
                    String title = etChatName.getText() != null
                            ? etChatName.getText().toString().trim() : "";
                    if (title.isEmpty()) title = "\uc0c8 \ub300\ud654";
                    String systemPrompt = cbPersona.isChecked()
                            ? viewModel.getPersonaSystemPrompt() : null;
                    viewModel.createNewConversation(title, null, null, systemPrompt);
                })
                .setNegativeButton("\ucde8\uc18c", null)
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
