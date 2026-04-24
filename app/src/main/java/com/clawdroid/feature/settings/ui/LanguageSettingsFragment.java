package com.clawdroid.feature.settings.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.clawdroid.app.databinding.FragmentLanguageSettingsBinding;
import com.clawdroid.core.data.repository.SettingsRepository;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LanguageSettingsFragment extends Fragment {

    private FragmentLanguageSettingsBinding binding;

    @Inject
    SettingsRepository settingsRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLanguageSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());

        updateRadioState(settingsRepository.getAppLanguage());

        binding.itemKorean.setOnClickListener(v -> applyLanguage("ko"));
        binding.itemEnglish.setOnClickListener(v -> applyLanguage("en"));
    }

    private void applyLanguage(String languageTag) {
        String current = settingsRepository.getAppLanguage();
        if (current.equals(languageTag)) return;

        settingsRepository.setAppLanguage(languageTag);
        updateRadioState(languageTag);

        LocaleListCompat locales = languageTag.isEmpty()
                ? LocaleListCompat.getEmptyLocaleList()
                : LocaleListCompat.forLanguageTags(languageTag);
        AppCompatDelegate.setApplicationLocales(locales);
    }

    private void updateRadioState(String languageTag) {
        boolean isKorean = "ko".equals(languageTag) || languageTag.isEmpty();
        binding.radioKorean.setChecked(isKorean);
        binding.radioEnglish.setChecked(!isKorean);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
