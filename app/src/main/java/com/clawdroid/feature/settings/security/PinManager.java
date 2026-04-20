package com.clawdroid.feature.settings.security;

import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PinManager {

    private static final String KEY_PIN_HASH = "app_lock_pin_hash";
    private static final String KEY_PIN_SALT = "app_lock_pin_salt";
    private static final String KEY_APP_LOCK_ENABLED = "app_lock_enabled";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_AUTO_DELETE_DAYS = "auto_delete_days";

    // SEC-M4: Brute-force protection
    private static final String KEY_FAIL_COUNT = "pin_fail_count";
    private static final String KEY_LOCKED_UNTIL = "pin_locked_until";
    /** [실패횟수 임계, 잠금 지속 ms] */
    private static final long[][] LOCKOUT_SCHEDULE = {
            {5,   60_000L},           // 5회 → 1분
            {10,  5 * 60_000L},       // 10회 → 5분
            {15,  15 * 60_000L},      // 15회 → 15분
            {20,  60 * 60_000L},      // 20회 → 1시간
            {30,  24 * 60 * 60_000L}  // 30회 → 24시간
    };

    private final SharedPreferences prefs;

    @Inject
    public PinManager(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public boolean isAppLockEnabled() {
        return prefs.getBoolean(KEY_APP_LOCK_ENABLED, false);
    }

    public void setAppLockEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply();
    }

    public boolean isBiometricEnabled() {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public void setBiometricEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public boolean isPinSet() {
        return prefs.getString(KEY_PIN_HASH, null) != null;
    }

    public boolean setPin(String pin) {
        if (pin == null || pin.length() < 4 || pin.length() > 6) {
            return false;
        }
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hash = hashPin(pin, salt);
        if (hash == null) return false;

        prefs.edit()
                .putString(KEY_PIN_SALT, saltBase64)
                .putString(KEY_PIN_HASH, hash)
                .remove(KEY_FAIL_COUNT)
                .remove(KEY_LOCKED_UNTIL)
                .apply();
        return true;
    }

    /**
     * 지수 백오프가 반영된 PIN 검증.
     * 잠금 중이면 PIN이 일치하더라도 false를 반환한다.
     * 호출 전에 {@link #getLockRemainingMillis()}로 상태를 먼저 확인할 것.
     */
    public boolean verifyPin(String pin) {
        if (getLockRemainingMillis() > 0) {
            return false;
        }
        String storedHash = prefs.getString(KEY_PIN_HASH, null);
        String saltBase64 = prefs.getString(KEY_PIN_SALT, null);
        if (storedHash == null || saltBase64 == null) return false;

        byte[] salt = Base64.getDecoder().decode(saltBase64);
        String hash = hashPin(pin, salt);
        boolean ok = storedHash.equals(hash);

        if (ok) {
            prefs.edit()
                    .remove(KEY_FAIL_COUNT)
                    .remove(KEY_LOCKED_UNTIL)
                    .apply();
        } else {
            recordFailure();
        }
        return ok;
    }

    /** 남은 잠금 시간(ms). 0이면 잠겨있지 않음. */
    public long getLockRemainingMillis() {
        long until = prefs.getLong(KEY_LOCKED_UNTIL, 0L);
        long now = System.currentTimeMillis();
        return Math.max(0L, until - now);
    }

    public int getFailCount() {
        return prefs.getInt(KEY_FAIL_COUNT, 0);
    }

    private void recordFailure() {
        int count = prefs.getInt(KEY_FAIL_COUNT, 0) + 1;
        long lockUntil = 0L;
        for (long[] entry : LOCKOUT_SCHEDULE) {
            if (count >= entry[0]) {
                lockUntil = System.currentTimeMillis() + entry[1];
            }
        }
        prefs.edit()
                .putInt(KEY_FAIL_COUNT, count)
                .putLong(KEY_LOCKED_UNTIL, lockUntil)
                .apply();
    }

    public void clearPin() {
        prefs.edit()
                .remove(KEY_PIN_HASH)
                .remove(KEY_PIN_SALT)
                .remove(KEY_APP_LOCK_ENABLED)
                .remove(KEY_BIOMETRIC_ENABLED)
                .remove(KEY_FAIL_COUNT)
                .remove(KEY_LOCKED_UNTIL)
                .apply();
    }

    public int getAutoDeleteDays() {
        return prefs.getInt(KEY_AUTO_DELETE_DAYS, 0); // 0 = disabled
    }

    public void setAutoDeleteDays(int days) {
        prefs.edit().putInt(KEY_AUTO_DELETE_DAYS, days).apply();
    }

    private String hashPin(String pin, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            byte[] hash = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
