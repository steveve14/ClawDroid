package com.clawdroid.feature.settings.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.clawdroid.feature.settings.databinding.FragmentSecuritySettingsBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SecuritySettingsFragment extends Fragment {

    private FragmentSecuritySettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSecuritySettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());

        binding.switchAppLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: Enable/disable biometric lock
        });

        binding.switchEncryption.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: Enable/disable database encryption
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
