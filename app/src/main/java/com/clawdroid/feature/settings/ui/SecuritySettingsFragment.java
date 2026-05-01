package com.clawdroid.feature.settings.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.clawdroid.app.R;
import com.clawdroid.app.databinding.FragmentSecuritySettingsBinding;
import com.clawdroid.feature.settings.security.PinManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SecuritySettingsFragment extends Fragment {

    private FragmentSecuritySettingsBinding binding;

    @Inject
    PinManager pinManager;

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

        // SEC-M2: PIN/생체 설정 화면 스크린샷·오버레이 차단
        if (getActivity() != null) {
            getActivity().getWindow().setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SECURE,
                    android.view.WindowManager.LayoutParams.FLAG_SECURE);
        }

        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());

        // Initialize UI state
        binding.switchAppLock.setChecked(pinManager.isAppLockEnabled());
        binding.switchBiometric.setChecked(pinManager.isBiometricEnabled());
        binding.switchEncryption.setChecked(false);
        updateBiometricVisibility();
        updateAutoDeleteSpinner();

        // PIN lock toggle
        binding.switchAppLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            if (isChecked) {
                showSetPinDialog();
            } else {
                showVerifyPinToDisable();
            }
        });

        // Biometric toggle
        binding.switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            if (isChecked) {
                enableBiometric();
            } else {
                pinManager.setBiometricEnabled(false);
            }
        });

        // Encryption toggle (placeholder)
        binding.switchEncryption.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // DB encryption is handled at Room level; toggle for future use
        });

        // Auto-delete spinner
        binding.spinnerAutoDelete.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent,
                                               View v, int position, long id) {
                        int[] dayValues = {0, 7, 30, 90, 180, 365};
                        if (position >= 0 && position < dayValues.length) {
                            pinManager.setAutoDeleteDays(dayValues[position]);
                        }
                    }

                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                });
    }

    private void updateBiometricVisibility() {
        boolean lockEnabled = pinManager.isAppLockEnabled();
        binding.cardBiometric.setVisibility(lockEnabled ? View.VISIBLE : View.GONE);

        BiometricManager biometricManager = BiometricManager.from(requireContext());
        boolean biometricAvailable = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS;
        binding.switchBiometric.setEnabled(biometricAvailable);
        if (!biometricAvailable) {
            binding.tvBiometricDesc.setText(R.string.security_biometric_unavailable);
        }
    }

    private void updateAutoDeleteSpinner() {
        int currentDays = pinManager.getAutoDeleteDays();
        int[] dayValues = {0, 7, 30, 90, 180, 365};
        for (int i = 0; i < dayValues.length; i++) {
            if (dayValues[i] == currentDays) {
                binding.spinnerAutoDelete.setSelection(i);
                break;
            }
        }
    }

    private void showSetPinDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, 0);

        EditText pinInput = new EditText(requireContext());
        pinInput.setHint(R.string.security_pin_enter_hint);
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pinInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        layout.addView(pinInput);

        EditText confirmInput = new EditText(requireContext());
        confirmInput.setHint(R.string.security_pin_confirm_hint);
        confirmInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        confirmInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        layout.addView(confirmInput);

        new AlertDialog.Builder(requireContext())
        .setTitle(R.string.security_pin_set_title)
                .setView(layout)
        .setPositiveButton(R.string.common_set, (dialog, which) -> {
                    String pin = pinInput.getText().toString();
                    String confirm = confirmInput.getText().toString();
                    if (pin.length() < 4) {
                        Toast.makeText(requireContext(), R.string.security_pin_min_length, Toast.LENGTH_SHORT).show();
                        binding.switchAppLock.setChecked(false);
                    } else if (!pin.equals(confirm)) {
                        Toast.makeText(requireContext(), R.string.security_pin_mismatch, Toast.LENGTH_SHORT).show();
                        binding.switchAppLock.setChecked(false);
                    } else {
                        pinManager.setPin(pin);
                        pinManager.setAppLockEnabled(true);
                        updateBiometricVisibility();
                        Toast.makeText(requireContext(), R.string.security_app_lock_enabled, Toast.LENGTH_SHORT).show();
                    }
                })
                    .setNegativeButton(R.string.common_cancel, (dialog, which) ->
                        binding.switchAppLock.setChecked(false))
                .setCancelable(false)
                .show();
    }

    private void showVerifyPinToDisable() {
        EditText pinInput = new EditText(requireContext());
        pinInput.setHint(R.string.security_current_pin_hint);
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pinInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        pinInput.setPadding(padding, padding, padding, 0);

        new AlertDialog.Builder(requireContext())
        .setTitle(R.string.security_pin_confirm_title)
                .setView(pinInput)
        .setPositiveButton(R.string.common_disable, (dialog, which) -> {
                    if (pinManager.verifyPin(pinInput.getText().toString())) {
                        pinManager.clearPin();
                        pinManager.setBiometricEnabled(false);
                        updateBiometricVisibility();
                        Toast.makeText(requireContext(), R.string.security_app_lock_disabled, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), R.string.security_pin_invalid, Toast.LENGTH_SHORT).show();
                        binding.switchAppLock.setChecked(true);
                    }
                })
                .setNegativeButton(R.string.common_cancel, (dialog, which) ->
                        binding.switchAppLock.setChecked(true))
                .setCancelable(false)
                .show();
    }

    private void enableBiometric() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.security_biometric_register_title))
                .setSubtitle(getString(R.string.security_biometric_register_subtitle))
                .setNegativeButtonText(getString(R.string.common_cancel))
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(requireContext()),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        pinManager.setBiometricEnabled(true);
                        Toast.makeText(requireContext(), R.string.security_biometric_enabled,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode,
                                                      @NonNull CharSequence errString) {
                        binding.switchBiometric.setChecked(false);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        binding.switchBiometric.setChecked(false);
                    }
                });
        biometricPrompt.authenticate(promptInfo);
    }

    @Override
    public void onDestroyView() {
        if (getActivity() != null) {
            getActivity().getWindow().clearFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SECURE);
        }
        super.onDestroyView();
        binding = null;
    }
}
