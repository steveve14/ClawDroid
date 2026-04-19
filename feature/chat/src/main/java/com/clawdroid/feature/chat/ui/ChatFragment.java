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

import com.clawdroid.core.data.db.entity.MessageEntity;
import com.clawdroid.feature.chat.adapter.MessageAdapter;
import com.clawdroid.feature.chat.databinding.FragmentChatBinding;
import com.clawdroid.feature.chat.viewmodel.ChatViewModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
    private ChatViewModel viewModel;
    private MessageAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        String convId = getArguments() != null
                ? getArguments().getString("conversationId") : null;
        if (convId != null) {
            viewModel.loadConversation(convId);
        }

        setupToolbar();
        setupRecyclerView();
        setupInputBar();
        setupObservers();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());

        binding.tvModelName.setOnClickListener(v -> {
            // TODO: Show model selection dialog
        });
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter();
        binding.recyclerMessages.setAdapter(adapter);
    }

    private void setupInputBar() {
        binding.btnSend.setOnClickListener(v -> {
            String text = binding.etMessage.getText() != null
                    ? binding.etMessage.getText().toString() : "";
            if (!text.trim().isEmpty()) {
                viewModel.sendMessage(text);
                binding.etMessage.setText("");
            }
        });

        binding.btnAttach.setOnClickListener(v -> {
            // TODO: Show attach options (image, file)
        });

        binding.btnVoiceInput.setOnClickListener(v -> {
            // TODO: Start voice input
        });
    }

    private void setupObservers() {
        viewModel.getConversation().observe(getViewLifecycleOwner(), conv -> {
            if (conv != null) {
                binding.tvTitle.setText(conv.getTitle() != null ? conv.getTitle() : "새 대화");
                binding.tvModelName.setText(
                        conv.getModelProvider() != null ? conv.getModelProvider() : "기본 모델");
            }
        });

        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            updateMessageList(messages, viewModel.getStreamingText().getValue());
        });

        viewModel.getStreamingText().observe(getViewLifecycleOwner(), streamText -> {
            List<MessageEntity> msgs = viewModel.getMessages().getValue();
            updateMessageList(msgs, streamText);
        });

        viewModel.getIsGenerating().observe(getViewLifecycleOwner(), generating -> {
            binding.typingIndicator.setVisibility(generating ? View.VISIBLE : View.GONE);
            binding.btnSend.setEnabled(!generating);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                com.google.android.material.snackbar.Snackbar.make(
                        binding.getRoot(), err, com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show();
            }
        });
    }

    private void updateMessageList(@Nullable List<MessageEntity> messages, @Nullable String streamText) {
        List<MessageAdapter.MessageItem> items = new ArrayList<>();
        if (messages != null) {
            for (MessageEntity msg : messages) {
                items.add(new MessageAdapter.MessageItem(msg));
            }
        }
        if (streamText != null && !streamText.isEmpty()) {
            items.add(new MessageAdapter.MessageItem(streamText));
        }
        adapter.submitList(items);
        if (!items.isEmpty()) {
            binding.recyclerMessages.scrollToPosition(items.size() - 1);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
