package com.clawdroid.core.data.db.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public final class Migrations {

    private Migrations() {}

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                "ALTER TABLE conversations ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0");
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_conversations_pinned ON conversations(is_pinned)");
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                "CREATE VIRTUAL TABLE IF NOT EXISTS messages_fts USING fts4("
                + "content, conversation_id, content=messages)");
            // Populate FTS table with existing data
            database.execSQL(
                "INSERT INTO messages_fts(rowid, content, conversation_id) "
                + "SELECT rowid, content, conversation_id FROM messages");
            // Create triggers to keep FTS in sync
            database.execSQL(
                "CREATE TRIGGER IF NOT EXISTS messages_fts_insert AFTER INSERT ON messages BEGIN "
                + "INSERT INTO messages_fts(rowid, content, conversation_id) "
                + "VALUES (NEW.rowid, NEW.content, NEW.conversation_id); END");
            database.execSQL(
                "CREATE TRIGGER IF NOT EXISTS messages_fts_delete AFTER DELETE ON messages BEGIN "
                + "INSERT INTO messages_fts(messages_fts, rowid, content, conversation_id) "
                + "VALUES ('delete', OLD.rowid, OLD.content, OLD.conversation_id); END");
            database.execSQL(
                "CREATE TRIGGER IF NOT EXISTS messages_fts_update AFTER UPDATE ON messages BEGIN "
                + "INSERT INTO messages_fts(messages_fts, rowid, content, conversation_id) "
                + "VALUES ('delete', OLD.rowid, OLD.content, OLD.conversation_id); "
                + "INSERT INTO messages_fts(rowid, content, conversation_id) "
                + "VALUES (NEW.rowid, NEW.content, NEW.conversation_id); END");
        }
    };

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS personas ("
                + "id TEXT NOT NULL PRIMARY KEY, "
                + "name TEXT NOT NULL, "
                + "system_prompt TEXT, "
                + "conversation_style TEXT, "
                + "is_active INTEGER NOT NULL DEFAULT 0, "
                + "created_at INTEGER NOT NULL DEFAULT 0)"
            );
        }
    };
}
