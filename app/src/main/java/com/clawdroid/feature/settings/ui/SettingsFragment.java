package com.clawdroid.feature.settings.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.clawdroid.app.R;
import com.clawdroid.app.databinding.FragmentSettingsBinding;
import com.clawdroid.core.data.repository.SettingsRepository;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Inject
    SettingsRepository settingsRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnModelSettings.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_settings_to_modelSettings));

        binding.btnPersonaSettings.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_settings_to_personaSettings));

        binding.btnSecuritySettings.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_settings_to_securitySettings));

        binding.btnToolSettings.setOnClickListener(v -> {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_settings_to_toolSettings);
        });

        binding.btnAbout.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_settings_to_about));

        binding.btnLanguageSettings.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_settings_to_languageSettings));

        String lang = settingsRepository.getAppLanguage();
        binding.tvCurrentLanguage.setText(getString("en".equals(lang) ? R.string.language_english : R.string.language_korean));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
