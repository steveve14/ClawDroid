package com.clawdroid.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.WindowManager;
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

import com.clawdroid.feature.settings.security.PinManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LockActivity extends AppCompatActivity {

    @Inject
    SharedPreferences prefs;

    @Inject
    PinManager pinManager;

    private TextView subtitle;
    private Button btnUnlock;
    private EditText pinInput;
    private final Handler lockoutHandler = new Handler(Looper.getMainLooper());
    private Runnable lockoutTicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // SEC-M2: 잠금 화면 스크린샷/오버레이 차단
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        buildPinUI();

        if (pinManager.isBiometricEnabled() && pinManager.getLockRemainingMillis() == 0) {
            showBiometricPrompt();
        }
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

        subtitle = new TextView(this);
        subtitle.setText("PIN을 입력해주세요");
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
        subtitle.setLayoutParams(subtitleParams);
        layout.addView(subtitle);

        pinInput = new EditText(this);
        pinInput.setHint("PIN (4~6자리)");
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pinInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        pinInput.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        inputParams.topMargin = (int) (24 * getResources().getDisplayMetrics().density);
        pinInput.setLayoutParams(inputParams);
        layout.addView(pinInput);

        btnUnlock = new Button(this);
        btnUnlock.setText("잠금 해제");
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
        btnUnlock.setLayoutParams(btnParams);
        layout.addView(btnUnlock);

        btnUnlock.setOnClickListener(v -> {
            long remaining = pinManager.getLockRemainingMillis();
            if (remaining > 0) {
                Toast.makeText(this,
                        "너무 많은 실패가 감지되어 잠겨있습니다.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            String pin = pinInput.getText().toString();
            if (pinManager.verifyPin(pin)) {
                setResult(RESULT_OK);
                finish();
            } else {
                pinInput.setText("");
                applyLockoutState();
            }
        });

        setContentView(layout);
        applyLockoutState();
    }

    /** 실패 누적에 따른 UI 상태 동기화. 잠금 중이면 입력을 비활성화하고 남은 시간을 표시한다. */
    private void applyLockoutState() {
        long remaining = pinManager.getLockRemainingMillis();
        if (remaining > 0) {
            btnUnlock.setEnabled(false);
            pinInput.setEnabled(false);
            scheduleLockoutTick(remaining);
        } else {
            btnUnlock.setEnabled(true);
            pinInput.setEnabled(true);
            int fails = pinManager.getFailCount();
            if (fails > 0) {
                subtitle.setText("PIN이 올바르지 않습니다. (누적 실패: " + fails + ")");
            } else {
                subtitle.setText("PIN을 입력해주세요");
            }
        }
    }

    private void scheduleLockoutTick(long remainingMs) {
        if (lockoutTicker != null) lockoutHandler.removeCallbacks(lockoutTicker);
        lockoutTicker = new Runnable() {
            @Override
            public void run() {
                long left = pinManager.getLockRemainingMillis();
                if (left <= 0) {
                    applyLockoutState();
                    return;
                }
                long sec = (left + 999) / 1000;
                subtitle.setText("잠김 — " + formatDuration(sec) + " 후 다시 시도");
                lockoutHandler.postDelayed(this, 1000L);
            }
        };
        lockoutHandler.post(lockoutTicker);
    }

    private String formatDuration(long totalSec) {
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        if (h > 0) return String.format("%d시간 %02d분 %02d초", h, m, s);
        if (m > 0) return String.format("%d분 %02d초", m, s);
        return s + "초";
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

    @Override
    protected void onDestroy() {
        if (lockoutTicker != null) lockoutHandler.removeCallbacks(lockoutTicker);
        super.onDestroy();
    }

    @SuppressWarnings("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // Prevent back button from bypassing lock — intentionally not calling super
        finishAffinity();
    }
}
