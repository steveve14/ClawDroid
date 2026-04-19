package com.clawdroid.feature.voice.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.clawdroid.feature.voice.databinding.FragmentVoiceChatBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VoiceChatFragment extends Fragment {

    private FragmentVoiceChatBinding binding;
    private boolean isRecording = false;

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

        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());

        binding.btnRecord.setOnClickListener(v -> toggleRecording());

        binding.btnStop.setOnClickListener(v -> stopRecording());

        binding.btnVoiceSettings.setOnClickListener(v -> {
            // TODO: Show voice settings
        });
    }

    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        isRecording = true;
        binding.tvStatus.setText("듣고 있습니다...");
        // TODO: Start SpeechRecognizer / ML Kit GenAI Speech
    }

    private void stopRecording() {
        isRecording = false;
        binding.tvStatus.setText("탭하여 말하기");
        // TODO: Stop recognition, process result
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
