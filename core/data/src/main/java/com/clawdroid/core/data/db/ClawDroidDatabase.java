package com.clawdroid.core.data.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.clawdroid.core.data.db.dao.AiProviderDao;
import com.clawdroid.core.data.db.dao.ChannelDao;
import com.clawdroid.core.data.db.dao.ConversationDao;
import com.clawdroid.core.data.db.dao.MessageDao;
import com.clawdroid.core.data.db.dao.ToolCallDao;
import com.clawdroid.core.data.db.entity.AiProviderEntity;
import com.clawdroid.core.data.db.entity.ChannelEntity;
import com.clawdroid.core.data.db.entity.ConversationEntity;
import com.clawdroid.core.data.db.entity.MessageEntity;
import com.clawdroid.core.data.db.entity.MessageFtsEntity;
import com.clawdroid.core.data.db.entity.ToolCallEntity;

@Database(
    entities = {
        ConversationEntity.class,
        MessageEntity.class,
        MessageFtsEntity.class,
        ToolCallEntity.class,
        ChannelEntity.class,
        AiProviderEntity.class
    },
    version = 3,
    exportSchema = true
)
public abstract class ClawDroidDatabase extends RoomDatabase {

    public abstract ConversationDao conversationDao();
    public abstract MessageDao messageDao();
    public abstract ToolCallDao toolCallDao();
    public abstract ChannelDao channelDao();
    public abstract AiProviderDao aiProviderDao();
}
