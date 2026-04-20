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
    private List<ConversationEntity> conversationList = new ArrayList<>();

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

        // 공통 헤더 설정
        View headerView = binding.commonHeader.getRoot();
        android.widget.TextView tvTitle = headerView.findViewById(R.id.tvHeaderTitle);
        if (tvTitle != null) tvTitle.setText("음성 채팅");
        com.google.android.material.appbar.MaterialToolbar toolbar =
                headerView.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
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
    }

    private void showConversationPicker() {
        if (conversationList.isEmpty()) {
            binding.tvStatus.setText("활성 채팅방이 없습니다");
            return;
        }

        String[] titles = new String[conversationList.size() + 1];
        titles[0] = "최근 채팅방 (자동 연결)";
        for (int i = 0; i < conversationList.size(); i++) {
            ConversationEntity c = conversationList.get(i);
            titles[i + 1] = c.getTitle() != null ? c.getTitle() : "채팅방 " + (i + 1);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("채팅방 선택")
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
                binding.tvStatus.setText("말씀해 주세요");
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) {
                binding.tvStatus.setText(err);
            }
        });

        viewModel.getConversations().observe(getViewLifecycleOwner(), list -> {
            conversationList = list != null ? list : new ArrayList<>();
        });

        viewModel.getSelectedConversation().observe(getViewLifecycleOwner(), conv -> {
            if (conv != null) {
                String title = conv.getTitle() != null ? conv.getTitle() : "채팅방";
                binding.tvSelectedConversation.setText("연결된 채팅방: " + title);
            } else {
                binding.tvSelectedConversation.setText("최근 채팅방 (자동 연결)");
            }
        });

        ttsManager.setCallback(new TtsManager.Callback() {
            @Override public void onStart() {
                if (isAdded()) binding.tvStatus.setText("응답 재생 중...");
            }
            @Override public void onDone() {
                if (isAdded()) binding.tvStatus.setText("말씀해 주세요");
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
        binding.tvStatus.setText("말씀해 주세요");
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