package com.clawdroid.app.di;

import android.content.Context;

import androidx.room.Room;

import com.clawdroid.core.data.db.ClawDroidDatabase;
import com.clawdroid.core.data.db.dao.AiProviderDao;
import com.clawdroid.core.data.db.dao.ChannelDao;
import com.clawdroid.core.data.db.dao.ConversationDao;
import com.clawdroid.core.data.db.dao.MessageDao;
import com.clawdroid.core.data.db.dao.ToolCallDao;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public ClawDroidDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(
                context,
                ClawDroidDatabase.class,
                "clawdroid.db"
        ).build();
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
}
