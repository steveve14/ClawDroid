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
            binding.tvBiometricDesc.setText("이 기기에서 생체 인증을 사용할 수 없습니다.");
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
        pinInput.setHint("PIN 입력 (4~6자리)");
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pinInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        layout.addView(pinInput);

        EditText confirmInput = new EditText(requireContext());
        confirmInput.setHint("PIN 확인");
        confirmInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        confirmInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        layout.addView(confirmInput);

        new AlertDialog.Builder(requireContext())
                .setTitle("PIN 설정")
                .setView(layout)
                .setPositiveButton("설정", (dialog, which) -> {
                    String pin = pinInput.getText().toString();
                    String confirm = confirmInput.getText().toString();
                    if (pin.length() < 4) {
                        Toast.makeText(requireContext(), "PIN은 4자리 이상이어야 합니다.", Toast.LENGTH_SHORT).show();
                        binding.switchAppLock.setChecked(false);
                    } else if (!pin.equals(confirm)) {
                        Toast.makeText(requireContext(), "PIN이 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                        binding.switchAppLock.setChecked(false);
                    } else {
                        pinManager.setPin(pin);
                        pinManager.setAppLockEnabled(true);
                        updateBiometricVisibility();
                        Toast.makeText(requireContext(), "앱 잠금이 활성화되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("취소", (dialog, which) ->
                        binding.switchAppLock.setChecked(false))
                .setCancelable(false)
                .show();
    }

    private void showVerifyPinToDisable() {
        EditText pinInput = new EditText(requireContext());
        pinInput.setHint("현재 PIN 입력");
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pinInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        pinInput.setPadding(padding, padding, padding, 0);

        new AlertDialog.Builder(requireContext())
                .setTitle("PIN 확인")
                .setView(pinInput)
                .setPositiveButton("해제", (dialog, which) -> {
                    if (pinManager.verifyPin(pinInput.getText().toString())) {
                        pinManager.clearPin();
                        pinManager.setBiometricEnabled(false);
                        updateBiometricVisibility();
                        Toast.makeText(requireContext(), "앱 잠금이 해제되었습니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "PIN이 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                        binding.switchAppLock.setChecked(true);
                    }
                })
                .setNegativeButton("취소", (dialog, which) ->
                        binding.switchAppLock.setChecked(true))
                .setCancelable(false)
                .show();
    }

    private void enableBiometric() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("생체 인증 등록")
                .setSubtitle("생체 인증으로 앱을 잠금 해제합니다.")
                .setNegativeButtonText("취소")
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(requireContext()),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        pinManager.setBiometricEnabled(true);
                        Toast.makeText(requireContext(), "생체 인증이 활성화되었습니다.",
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
        super.onDestroyView();
        binding = null;
    }
}
