package com.clawdroid.feature.chat.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.clawdroid.core.data.db.entity.ConversationEntity;
import com.clawdroid.core.data.db.entity.MessageEntity;
import com.clawdroid.feature.chat.adapter.MessageAdapter;
import com.clawdroid.feature.chat.databinding.FragmentChatBinding;
import com.clawdroid.feature.chat.util.ExportHelper;
import com.clawdroid.feature.chat.viewmodel.ChatViewModel;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
    private ChatViewModel viewModel;
    private MessageAdapter adapter;
    private byte[] pendingImage;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        loadImage(imageUri);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap bitmap = (Bitmap) extras.get("data");
                        if (bitmap != null) {
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
                            pendingImage = stream.toByteArray();
                            binding.etMessage.setHint("이미지 첨부됨 — 메시지 입력...");
                        }
                    }
                }
            });

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

        binding.tvModelName.setOnClickListener(v -> showModelSwitchDialog());
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
                if (text.trim().startsWith("/")) {
                    handleCommand(text.trim());
                } else {
                    if (pendingImage != null) {
                        viewModel.sendMessageWithImage(text, pendingImage);
                        pendingImage = null;
                        binding.etMessage.setHint("메시지 입력...");
                    } else {
                        viewModel.sendMessage(text);
                    }
                }
                binding.etMessage.setText("");
            }
        });

        binding.btnAttach.setOnClickListener(v -> showAttachOptions());

        binding.btnVoiceInput.setOnClickListener(v -> {
            // Voice input handled by Phase 2 VoiceChat
        });
    }

    private void showAttachOptions() {
        String[] options = {"갤러리에서 선택", "카메라로 촬영", "대화 내보내기 (Markdown)", "대화 내보내기 (JSON)"};
        new AlertDialog.Builder(requireContext())
                .setTitle("첨부 / 내보내기")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            pickImageFromGallery();
                            break;
                        case 1:
                            captureFromCamera();
                            break;
                        case 2:
                            exportMarkdown();
                            break;
                        case 3:
                            exportJson();
                            break;
                    }
                })
                .show();
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void captureFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private void loadImage(Uri uri) {
        try {
            InputStream input = requireContext().getContentResolver().openInputStream(uri);
            if (input != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                input.close();
                if (bitmap != null) {
                    // Scale down if needed
                    int maxDim = 1024;
                    if (bitmap.getWidth() > maxDim || bitmap.getHeight() > maxDim) {
                        float scale = Math.min(
                                (float) maxDim / bitmap.getWidth(),
                                (float) maxDim / bitmap.getHeight());
                        bitmap = Bitmap.createScaledBitmap(bitmap,
                                (int) (bitmap.getWidth() * scale),
                                (int) (bitmap.getHeight() * scale), true);
                    }
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
                    pendingImage = stream.toByteArray();
                    binding.etMessage.setHint("이미지 첨부됨 — 메시지 입력...");
                }
            }
        } catch (Exception e) {
            // Silently fail
        }
    }

    private void exportMarkdown() {
        ConversationEntity conv = viewModel.getConversation().getValue();
        List<MessageEntity> msgs = viewModel.getMessages().getValue();
        if (conv != null && msgs != null) {
            String md = ExportHelper.toMarkdown(conv, msgs);
            ExportHelper.shareText(requireContext(), md,
                    (conv.getTitle() != null ? conv.getTitle() : "conversation") + ".md",
                    "text/markdown");
        }
    }

    private void exportJson() {
        ConversationEntity conv = viewModel.getConversation().getValue();
        List<MessageEntity> msgs = viewModel.getMessages().getValue();
        if (conv != null && msgs != null) {
            String json = ExportHelper.toJson(conv, msgs);
            ExportHelper.shareText(requireContext(), json,
                    (conv.getTitle() != null ? conv.getTitle() : "conversation") + ".json",
                    "application/json");
        }
    }

    private void handleCommand(String command) {
        if (command.startsWith("/compact")) {
            viewModel.compactConversation();
        } else if (command.startsWith("/new")) {
            viewModel.resetConversation();
        } else if (command.startsWith("/reset")) {
            viewModel.resetConversation();
        } else if (command.startsWith("/status")) {
            viewModel.showStatus();
        } else if (command.startsWith("/think")) {
            String[] parts = command.split("\\s+", 2);
            String level = parts.length > 1 ? parts[1].trim() : "";
            viewModel.setThinkLevel(level);
        } else {
            viewModel.sendMessage(command);
        }
    }

    private void showModelSwitchDialog() {
        ModelSwitchBottomSheet sheet = ModelSwitchBottomSheet.newInstance();
        List<ModelSwitchBottomSheet.ModelOption> options = new ArrayList<>();
        options.add(new ModelSwitchBottomSheet.ModelOption("gemini-nano", "Gemini Nano", "gemini-nano", "Gemini Nano (온디바이스)"));
        options.add(new ModelSwitchBottomSheet.ModelOption("gemini-cloud", "Gemini Cloud", "gemini-2.5-flash", "Gemini 2.5 Flash"));
        options.add(new ModelSwitchBottomSheet.ModelOption("gemini-cloud", "Gemini Cloud", "gemini-2.5-pro", "Gemini 2.5 Pro"));
        options.add(new ModelSwitchBottomSheet.ModelOption("openai", "OpenAI", "gpt-4o", "GPT-4o"));
        options.add(new ModelSwitchBottomSheet.ModelOption("openai", "OpenAI", "gpt-4o-mini", "GPT-4o Mini"));
        options.add(new ModelSwitchBottomSheet.ModelOption("ollama", "Ollama", "gemma3", "Gemma 3 (Local)"));
        options.add(new ModelSwitchBottomSheet.ModelOption("custom", "Custom", "default", "Custom Endpoint"));

        sheet.setModels(options);
        ConversationEntity conv = viewModel.getConversation().getValue();
        if (conv != null && conv.getModelId() != null) {
            sheet.setCurrentModelId(conv.getModelId());
        }
        sheet.setOnModelSelectedListener((providerId, modelId) ->
                viewModel.switchModel(providerId, modelId));
        sheet.show(getChildFragmentManager(), "model_switch");
    }

    private void setupObservers() {
        viewModel.getConversation().observe(getViewLifecycleOwner(), conv -> {
            if (conv != null) {
                binding.tvTitle.setText(conv.getTitle() != null ? conv.getTitle() : "새 대화");
                binding.tvModelName.setText(
                        conv.getModelId() != null ? conv.getModelId() : "기본 모델");
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
