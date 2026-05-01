package com.clawdroid.feature.settings.ui;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.clawdroid.app.R;
import com.clawdroid.app.databinding.FragmentAboutBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AboutFragment extends Fragment {

    private FragmentAboutBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());

        // Set version dynamically
        try {
            PackageInfo pInfo = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            binding.tvVersion.setText("v" + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            binding.tvVersion.setText("v1.0.0");
        }

        binding.btnTerms.setOnClickListener(v ->
        showInfoDialog(getString(R.string.about_terms),
            getString(R.string.about_terms_content)));

        binding.btnPrivacy.setOnClickListener(v ->
        showInfoDialog(getString(R.string.about_privacy),
            getString(R.string.about_privacy_content)));

        binding.btnLicenses.setOnClickListener(v ->
        showInfoDialog(getString(R.string.about_open_source),
            getString(R.string.about_open_source_content)));
    }

    private void showInfoDialog(String title, String content) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(content)
            .setPositiveButton(R.string.common_ok, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
