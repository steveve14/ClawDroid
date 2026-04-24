package com.clawdroid.core.data.db;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * SQLCipher 전환 시 기존 평문 SQLite 데이터베이스를 감지·처리하는 유틸리티.
 *
 * SQLite 평문 파일은 파일 헤더 첫 16바이트가 "SQLite format 3\0" 이다.
 * SQLCipher로 암호화된 파일은 첫 16바이트가 랜덤 암호문이므로 이 값과 일치하지 않는다.
 *
 * 평문 DB가 감지되면 Room이 새 암호화 DB를 생성할 수 있도록 기존 파일(WAL/SHM 포함)을 삭제한다.
 */
public final class DatabaseMigrationHelper {

    private static final String TAG = "DatabaseMigHelper";

    /** SQLite 3 파일 매직 바이트 (16바이트) */
    private static final byte[] SQLITE_MAGIC =
            "SQLite format 3\0".getBytes(StandardCharsets.US_ASCII);

    private DatabaseMigrationHelper() {}

    /**
     * 지정된 이름의 데이터베이스 파일이 평문 SQLite 파일인지 확인한다.
     *
     * @param dbFile 검사할 파일
     * @return 평문 SQLite이면 {@code true}, 그 외(암호화됐거나 파일 없음)이면 {@code false}
     */
    public static boolean isPlainSQLite(File dbFile) {
        if (!dbFile.exists() || dbFile.length() < 16) {
            return false;
        }
        byte[] header = new byte[16];
        try (InputStream is = new FileInputStream(dbFile)) {
            if (is.read(header) != 16) return false;
        } catch (IOException e) {
            Log.w(TAG, "DB 헤더 읽기 실패: " + dbFile.getPath(), e);
            return false;
        }
        for (int i = 0; i < 16; i++) {
            if (header[i] != SQLITE_MAGIC[i]) return false;
        }
        return true;
    }

    /**
     * 데이터베이스가 평문 SQLite이면 삭제한다 (WAL·SHM 파일 포함).
     *
     * <p>SQLCipher 도입 이전 빌드에서 생성된 DB가 기기에 남아 있을 때
     * {@code SupportOpenHelperFactory}가 HMAC 검증 실패로 오픈을 거부하는 문제를 해결한다.
     * 삭제 후 Room이 새 암호화 DB를 자동 생성한다.
     *
     * @param context  애플리케이션 컨텍스트
     * @param dbName   데이터베이스 파일명 (예: "clawdroid.db")
     * @return 파일이 삭제됐으면 {@code true}
     */
    public static boolean deleteIfPlainSQLite(Context context, String dbName) {
        File dbFile = context.getDatabasePath(dbName);
        if (!isPlainSQLite(dbFile)) {
            return false;
        }
        Log.w(TAG, "평문 SQLite DB 감지: " + dbFile.getPath()
                + " — SQLCipher 암호화 DB 재생성을 위해 삭제합니다.");

        boolean deleted = context.deleteDatabase(dbName);
        if (deleted) {
            Log.i(TAG, "평문 DB 삭제 완료. Room이 암호화 DB를 새로 생성합니다.");
        } else {
            Log.e(TAG, "평문 DB 삭제 실패. 앱 재시작 후에도 오류가 지속될 수 있습니다.");
        }
        return deleted;
    }
}
