package com.clawdroid.feature.voice.ui;

import android.Manifest;
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

import com.clawdroid.feature.voice.adapter.VoiceLogAdapter;
import com.clawdroid.feature.voice.databinding.FragmentVoiceChatBinding;
import com.clawdroid.feature.voice.speech.SpeechRecognitionManager;
import com.clawdroid.feature.voice.speech.TtsManager;
import com.clawdroid.feature.voice.viewmodel.VoiceChatViewModel;

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

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startRecording();
                } else {
                    binding.tvStatus.setText("마이크 권한이 필요합니다");
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

        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());

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
    }

    private void setupSpeechCallback() {
        speechManager.setCallback(new SpeechRecognitionManager.Callback() {
            @Override
            public void onPartialResult(String text) {
                if (isAdded()) {
                    binding.tvStatus.setText(text);
                }
            }

            @Override
            public void onFinalResult(String text) {
                if (isAdded()) {
                    isRecording = false;
                    binding.tvStatus.setText("처리 중...");
                    viewModel.processUserSpeech(text);
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    isRecording = false;
                    binding.tvStatus.setText(message);
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
                binding.tvStatus.setText("응답 재생 중...");
            }
        });

        viewModel.getIsProcessing().observe(getViewLifecycleOwner(), processing -> {
            binding.btnRecord.setEnabled(!processing);
            if (!processing && !isRecording) {
                binding.tvStatus.setText("탭하여 말하기");
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) {
                binding.tvStatus.setText(err);
            }
        });

        ttsManager.setCallback(new TtsManager.Callback() {
            @Override public void onStart() {
                if (isAdded()) binding.tvStatus.setText("응답 재생 중...");
            }
            @Override public void onDone() {
                if (isAdded()) binding.tvStatus.setText("탭하여 말하기");
            }
            @Override public void onError(String message) {
                if (isAdded()) binding.tvStatus.setText(message);
            }
        });
    }

    private void toggleRecording() {
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
        isRecording = true;
        binding.tvStatus.setText("듣고 있습니다...");
        speechManager.startListening();
    }

    private void stopRecording() {
        isRecording = false;
        binding.tvStatus.setText("탭하여 말하기");
        speechManager.stopListening();
    }

    @Override
    public void onDestroyView() {
        speechManager.destroy();
        ttsManager.setCallback(null);
        super.onDestroyView();
        binding = null;
    }
}
