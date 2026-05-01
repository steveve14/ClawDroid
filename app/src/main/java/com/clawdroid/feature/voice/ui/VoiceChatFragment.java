package com.clawdroid.feature.voice.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.clawdroid.core.data.db.entity.ConversationEntity;
import com.clawdroid.feature.voice.adapter.VoiceLogAdapter;
import com.clawdroid.app.databinding.FragmentVoiceChatBinding;
import com.clawdroid.app.R;
import com.clawdroid.feature.voice.speech.SpeechRecognitionManager;
import com.clawdroid.feature.voice.speech.TtsManager;
import com.clawdroid.feature.voice.viewmodel.VoiceChatViewModel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VoiceChatFragment extends Fragment {

    private FragmentVoiceChatBinding binding;
    private VoiceChatViewModel viewModel;
    private VoiceLogAdapter logAdapter;

    @Inject SpeechRecognitionManager speechManager;
    @Inject TtsManager ttsManager;

    private boolean isRecording = false;
    private boolean isProcessing = false;
    private boolean hasActiveConversation = false;
    private List<ConversationEntity> conversationList = new ArrayList<>();

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startRecording();
                } else {
                    binding.tvStatus.setText(R.string.voice_permission_required);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVoiceChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(VoiceChatViewModel.class);

        logAdapter = new VoiceLogAdapter();
        binding.recyclerHistory.setAdapter(logAdapter);

        // 공통 헤더 설정
        View headerView = binding.commonHeader.getRoot();
        android.widget.TextView tvTitle = headerView.findViewById(R.id.tvHeaderTitle);
        if (tvTitle != null) tvTitle.setText(R.string.voice_title);
        com.google.android.material.appbar.MaterialToolbar toolbar =
                headerView.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
            toolbar.setNavigationIconTint(
                    getResources().getColor(R.color.md_on_surface, null));
            toolbar.setNavigationOnClickListener(v ->
                    Navigation.findNavController(requireView()).navigateUp());
        }

        binding.cardConversationSelector.setOnClickListener(v -> showConversationPicker());
        binding.btnRecord.setOnClickListener(v -> toggleRecording());
        binding.btnStop.setOnClickListener(v -> {
            stopRecording();
            ttsManager.stop();
        });
        binding.btnVoiceSettings.setOnClickListener(v -> {
            // Navigate to voice settings
        });

        setupSpeechCallback();
        setupObservers();
        setRecordingUi(false, getString(R.string.voice_prompt_speak));
    }

    private void showConversationPicker() {
        if (conversationList.isEmpty()) {
            binding.tvStatus.setText(R.string.voice_no_active_conversation);
            return;
        }

        String[] titles = new String[conversationList.size() + 1];
        titles[0] = getString(R.string.voice_recent_conversation);
        for (int i = 0; i < conversationList.size(); i++) {
            ConversationEntity c = conversationList.get(i);
            titles[i + 1] = c.getTitle() != null
                    ? c.getTitle()
                    : getString(R.string.voice_conversation_default) + " " + (i + 1);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.voice_conversation_picker_title)
                .setItems(titles, (dialog, which) -> {
                    if (which == 0) {
                        viewModel.selectConversation(
                                conversationList.isEmpty() ? null : conversationList.get(0));
                    } else {
                        viewModel.selectConversation(conversationList.get(which - 1));
                    }
                })
                .show();
    }

    private void setupSpeechCallback() {
        speechManager.setCallback(new SpeechRecognitionManager.Callback() {
            @Override
            public void onPartialResult(String text) {
                if (isAdded()) {
                    isRecording = true;
                    binding.tvStatus.setText(text);
                }
            }

            @Override
            public void onFinalResult(String text) {
                if (isAdded()) {
                    if (!hasActiveConversation) {
                        setRecordingUi(false, getString(R.string.voice_no_active_conversation));
                        return;
                    }
                    setRecordingUi(false, getString(R.string.voice_processing));
                    binding.tvStatus.setText(R.string.voice_processing);
                    viewModel.processUserSpeech(text);
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    setRecordingUi(false, message);
                }
            }

            @Override
            public void onRmsChanged(float rms) {
                if (isAdded()) {
                    float scale = 1.0f + (rms / 10f);
                    binding.visualizerArea.setScaleX(Math.min(scale, 1.5f));
                    binding.visualizerArea.setScaleY(Math.min(scale, 1.5f));
                }
            }
        });
    }

    private void setupObservers() {
        viewModel.getVoiceLog().observe(getViewLifecycleOwner(), log -> {
            logAdapter.submitList(log);
            if (log != null && !log.isEmpty()) {
                binding.recyclerHistory.scrollToPosition(log.size() - 1);
            }
        });

        viewModel.getAiResponse().observe(getViewLifecycleOwner(), response -> {
            if (response != null && !response.isEmpty()) {
                ttsManager.speak(response);
                binding.tvStatus.setText(R.string.voice_playing_response);
            }
        });

        viewModel.getIsProcessing().observe(getViewLifecycleOwner(), processing -> {
            isProcessing = Boolean.TRUE.equals(processing);
            updateRecordButtonState();
            if (!isProcessing && !isRecording) {
                binding.tvStatus.setText(hasActiveConversation
                        ? R.string.voice_prompt_speak
                        : R.string.voice_no_active_conversation);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) {
                binding.tvStatus.setText(err);
            }
        });

        viewModel.getConversations().observe(getViewLifecycleOwner(), list -> {
            conversationList = list != null ? list : new ArrayList<>();
            if (conversationList.isEmpty()) {
                hasActiveConversation = false;
                speechManager.stopListening();
                setRecordingUi(false, getString(R.string.voice_no_active_conversation));
            }
        });

        viewModel.getSelectedConversation().observe(getViewLifecycleOwner(), conv -> {
            hasActiveConversation = conv != null;
            updateRecordButtonState();
            if (conv != null) {
                String title = conv.getTitle() != null
                        ? conv.getTitle()
                        : getString(R.string.voice_conversation_default);
                binding.tvSelectedConversation.setText(
                        getString(R.string.voice_selected_conversation, title));
                if (!isRecording && !isProcessing) {
                    binding.tvStatus.setText(R.string.voice_prompt_speak);
                }
            } else {
                binding.tvSelectedConversation.setText(R.string.voice_no_active_conversation);
                if (!isRecording) {
                    binding.tvStatus.setText(R.string.voice_no_active_conversation);
                }
            }
        });

        ttsManager.setCallback(new TtsManager.Callback() {
            @Override public void onStart() {
                if (isAdded()) binding.tvStatus.setText(R.string.voice_playing_response);
            }
            @Override public void onDone() {
                if (isAdded()) binding.tvStatus.setText(hasActiveConversation
                        ? R.string.voice_prompt_speak
                        : R.string.voice_no_active_conversation);
            }
            @Override public void onError(String message) {
                if (isAdded()) binding.tvStatus.setText(message);
            }
        });
    }

    private void toggleRecording() {
        if (!hasActiveConversation) {
            setRecordingUi(false, getString(R.string.voice_no_active_conversation));
            return;
        }

        if (isRecording) {
            stopRecording();
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
        }
    }

    private void startRecording() {
        if (!hasActiveConversation) {
            setRecordingUi(false, getString(R.string.voice_no_active_conversation));
            return;
        }

        ttsManager.stop();
        setRecordingUi(true, getString(R.string.voice_listening));
        speechManager.startListening();
    }

    private void stopRecording() {
        speechManager.stopListening();
        setRecordingUi(false, getString(hasActiveConversation
                ? R.string.voice_prompt_speak
                : R.string.voice_no_active_conversation));
    }

    private void setRecordingUi(boolean recording, String statusText) {
        isRecording = recording;
        if (binding == null) return;
        binding.tvStatus.setText(statusText);
        binding.visualizerArea.setScaleX(recording ? 1.05f : 1.0f);
        binding.visualizerArea.setScaleY(recording ? 1.05f : 1.0f);
        binding.btnStop.setEnabled(recording);
        updateRecordButtonState();
    }

    private void updateRecordButtonState() {
        if (binding == null) return;
        boolean enabled = hasActiveConversation && !isProcessing;
        binding.btnRecord.setEnabled(enabled);
        binding.btnRecord.setAlpha(enabled ? 1f : 0.45f);
    }

    @Override
    public void onDestroyView() {
        speechManager.destroy();
        ttsManager.setCallback(null);
        super.onDestroyView();
        binding = null;
    }
}