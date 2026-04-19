package com.clawdroid.feature.settings.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.clawdroid.core.data.repository.SettingsRepository;
import com.clawdroid.feature.settings.databinding.FragmentPersonaSettingsBinding;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PersonaSettingsFragment extends Fragment {

    private FragmentPersonaSettingsBinding binding;

    @Inject
    SettingsRepository settingsRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPersonaSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());

        // Load current values
        binding.etPersonaName.setText(settingsRepository.getPersonaName());
        binding.etSystemPrompt.setText(settingsRepository.getSystemPrompt());
        binding.actvStyle.setText(settingsRepository.getConversationStyle(), false);

        binding.btnSave.setOnClickListener(v -> {
            String name = binding.etPersonaName.getText() != null
                    ? binding.etPersonaName.getText().toString() : "";
            String prompt = binding.etSystemPrompt.getText() != null
                    ? binding.etSystemPrompt.getText().toString() : "";
            String style = binding.actvStyle.getText() != null
                    ? binding.actvStyle.getText().toString() : "";

            settingsRepository.setPersonaName(name);
            settingsRepository.setSystemPrompt(prompt);
            settingsRepository.setConversationStyle(style);

            com.google.android.material.snackbar.Snackbar.make(
                    binding.getRoot(), "저장되었습니다", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
            ).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
