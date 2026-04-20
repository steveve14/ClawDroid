package com.clawdroid.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LockActivity extends AppCompatActivity {

    @Inject
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean biometricEnabled = prefs.getBoolean("biometric_enabled", false);

        if (biometricEnabled) {
            showBiometricPrompt();
        }

        buildPinUI();
    }

    private void buildPinUI() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        int padding = (int) (32 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("🔒 ClawDroid");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("PIN을 입력해주세요");
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
        subtitle.setLayoutParams(subtitleParams);
        layout.addView(subtitle);

        EditText pinInput = new EditText(this);
        pinInput.setHint("PIN (4~6자리)");
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pinInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        pinInput.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        inputParams.topMargin = (int) (24 * getResources().getDisplayMetrics().density);
        pinInput.setLayoutParams(inputParams);
        layout.addView(pinInput);

        Button btnUnlock = new Button(this);
        btnUnlock.setText("잠금 해제");
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
        btnUnlock.setLayoutParams(btnParams);
        layout.addView(btnUnlock);

        btnUnlock.setOnClickListener(v -> {
            String pin = pinInput.getText().toString();
            if (verifyPin(pin)) {
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "PIN이 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                pinInput.setText("");
            }
        });

        setContentView(layout);
    }

    private boolean verifyPin(String pin) {
        String storedHash = prefs.getString("app_lock_pin_hash", null);
        String saltBase64 = prefs.getString("app_lock_pin_salt", null);
        if (storedHash == null || saltBase64 == null) return false;

        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            byte[] hash = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
            String computedHash = Base64.getEncoder().encodeToString(hash);
            return storedHash.equals(computedHash);
        } catch (Exception e) {
            return false;
        }
    }

    private void showBiometricPrompt() {
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                != BiometricManager.BIOMETRIC_SUCCESS) {
            return;
        }

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("ClawDroid 잠금 해제")
                .setSubtitle("생체 인증으로 잠금을 해제합니다.")
                .setNegativeButtonText("PIN으로 입력")
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        setResult(RESULT_OK);
                        finish();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode,
                                                      @NonNull CharSequence errString) {
                        // Fall back to PIN input
                    }
                });
        biometricPrompt.authenticate(promptInfo);
    }

    @SuppressWarnings("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // Prevent back button from bypassing lock — intentionally not calling super
        finishAffinity();
    }
}
