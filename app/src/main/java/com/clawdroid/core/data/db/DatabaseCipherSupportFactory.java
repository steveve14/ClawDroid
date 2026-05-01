package com.clawdroid.core.data.db;

import net.zetetic.database.sqlcipher.SQLiteConnection;
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook;
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;

public final class DatabaseCipherSupportFactory {

    private static final SQLiteDatabaseHook MEMORY_SECURITY_HOOK = new SQLiteDatabaseHook() {
        @Override
        public void preKey(SQLiteConnection database) {
            disableMemorySecurity(database);
        }

        @Override
        public void postKey(SQLiteConnection database) {
            disableMemorySecurity(database);
        }
    };

    private DatabaseCipherSupportFactory() {}

    public static SupportOpenHelperFactory create(byte[] passphrase) {
        return new SupportOpenHelperFactory(passphrase, MEMORY_SECURITY_HOOK, true);
    }

    private static void disableMemorySecurity(SQLiteConnection database) {
        try {
            database.execute("PRAGMA cipher_memory_security = OFF;", null, null);
        } catch (RuntimeException ignored) {
            // Older SQLCipher builds may not support this PRAGMA; DB open should continue.
        }
    }
}