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
                .apply();
        return true;
    }

    public boolean verifyPin(String pin) {
        String storedHash = prefs.getString(KEY_PIN_HASH, null);
        String saltBase64 = prefs.getString(KEY_PIN_SALT, null);
        if (storedHash == null || saltBase64 == null) return false;

        byte[] salt = Base64.getDecoder().decode(saltBase64);
        String hash = hashPin(pin, salt);
        return storedHash.equals(hash);
    }

    public void clearPin() {
        prefs.edit()
                .remove(KEY_PIN_HASH)
                .remove(KEY_PIN_SALT)
                .remove(KEY_APP_LOCK_ENABLED)
                .remove(KEY_BIOMETRIC_ENABLED)
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
