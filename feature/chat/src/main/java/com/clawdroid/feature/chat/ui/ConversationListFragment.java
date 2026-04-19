package com.clawdroid.feature.chat.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.clawdroid.feature.chat.R;
import com.clawdroid.feature.chat.adapter.ConversationAdapter;
import com.clawdroid.feature.chat.databinding.FragmentConversationListBinding;
import com.clawdroid.feature.chat.viewmodel.ConversationListViewModel;

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
            public void onClick(com.clawdroid.core.data.db.entity.ConversationEntity conversation) {
                Bundle args = new Bundle();
                args.putString("conversationId", conversation.getId());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_conversationList_to_chat, args);
            }

            @Override
            public void onLongClick(com.clawdroid.core.data.db.entity.ConversationEntity conversation) {
                // TODO: Show context menu (rename, archive, delete)
            }
        });
    }

    private void setupObservers() {
        viewModel.getConversations().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            binding.emptyState.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            binding.recyclerConversations.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    private void setupActions() {
        binding.fabNewConversation.setOnClickListener(v -> {
            viewModel.createNewConversation("새 대화", null, null);
        });

        binding.toolbar.inflateMenu(R.menu.menu_conversation_list);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            // TODO: Handle search
            return false;
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
