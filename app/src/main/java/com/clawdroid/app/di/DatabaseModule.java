package com.clawdroid.app.di;

import android.content.Context;

import androidx.room.Room;

import com.clawdroid.core.data.db.ClawDroidDatabase;
import com.clawdroid.core.data.db.DatabaseKeyManager;
import com.clawdroid.core.data.db.DatabaseMigrationHelper;
import com.clawdroid.core.data.db.migration.Migrations;
import com.clawdroid.core.data.db.dao.AiProviderDao;
import com.clawdroid.core.data.db.dao.ChannelDao;
import com.clawdroid.core.data.db.dao.ConversationDao;
import com.clawdroid.core.data.db.dao.MessageDao;
import com.clawdroid.core.data.db.dao.PersonaDao;
import com.clawdroid.core.data.db.dao.ToolCallDao;

import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    static {
        // SQLCipher 네이티브 라이브러리 로드 (한 번만)
        System.loadLibrary("sqlcipher");
    }

    @Provides
    @Singleton
    public ClawDroidDatabase provideDatabase(@ApplicationContext Context context) {
        // SQLCipher 도입 이전의 평문 DB가 기기에 남아 있으면 삭제 (HMAC 실패 방지)
        DatabaseMigrationHelper.deleteIfPlainSQLite(context, "clawdroid.db");

        // SEC-H1: Room + SQLCipher 통합 — 데이터베이스 전체를 AES-256으로 암호화
        char[] passphrase = DatabaseKeyManager.getOrCreatePassphrase(context);
        byte[] passBytes = new String(passphrase).getBytes(StandardCharsets.UTF_8);
        // 원본 char[] 제로화 (메모리 상주 축소)
        Arrays.fill(passphrase, '0');

        SupportOpenHelperFactory factory = new SupportOpenHelperFactory(passBytes);

        return Room.databaseBuilder(
                context,
                ClawDroidDatabase.class,
                "clawdroid.db"
        )
        .openHelperFactory(factory)
        .addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3, Migrations.MIGRATION_3_4)
        .fallbackToDestructiveMigration()
        .build();
    }

    @Provides
    public ConversationDao provideConversationDao(ClawDroidDatabase db) {
        return db.conversationDao();
    }

    @Provides
    public MessageDao provideMessageDao(ClawDroidDatabase db) {
        return db.messageDao();
    }

    @Provides
    public ChannelDao provideChannelDao(ClawDroidDatabase db) {
        return db.channelDao();
    }

    @Provides
    public ToolCallDao provideToolCallDao(ClawDroidDatabase db) {
        return db.toolCallDao();
    }

    @Provides
    public AiProviderDao provideAiProviderDao(ClawDroidDatabase db) {
        return db.aiProviderDao();
    }

    @Provides
    public PersonaDao providePersonaDao(ClawDroidDatabase db) {
        return db.personaDao();
    }
}
