package com.clawdroid.core.data.db;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.security.SecureRandom;

/**
 * SEC-H1: SQLCipher passphrase 관리자.
 *
 * SQLCipher 데이터베이스 암호화 키(32바이트 무작위)를 **Android Keystore로 보호되는**
 * EncryptedSharedPreferences 전용 저장소에 별도 파일로 보관한다.
 *
 * - 저장소 파일: `clawdroid_db_key` (EncryptedSharedPreferences, 마스터키는 AndroidKeyStore)
 * - 키 포맷: 32바이트 → 64 hex 문자열 (char[]로 반환해 String 상수풀 캐싱 방지)
 * - 앱 최초 실행 시 랜덤 생성, 이후 재사용. PIN/사용자 비밀번호와 독립.
 */
public final class DatabaseKeyManager {

    private static final String PREFS_FILE = "clawdroid_db_key";
    private static final String KEY_PASSPHRASE = "db_passphrase_hex";
    private static final int KEY_BYTES = 32; // 256-bit

    private DatabaseKeyManager() {}

    public static char[] getOrCreatePassphrase(Context context) {
        SharedPreferences prefs = openEncryptedPrefs(context);
        String hex = prefs.getString(KEY_PASSPHRASE, null);
        if (hex == null) {
            byte[] raw = new byte[KEY_BYTES];
            new SecureRandom().nextBytes(raw);
            hex = toHex(raw);
            // 원본 바이트 제로화
            for (int i = 0; i < raw.length; i++) raw[i] = 0;
            prefs.edit().putString(KEY_PASSPHRASE, hex).apply();
        }
        return hex.toCharArray();
    }

    private static SharedPreferences openEncryptedPrefs(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            return EncryptedSharedPreferences.create(
                    PREFS_FILE,
                    masterKeyAlias,
                    context.getApplicationContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new IllegalStateException("DB 암호화 키 저장소 초기화 실패", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }
}
