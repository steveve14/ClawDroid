package com.clawdroid.core.data.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Fts4;

@Fts4(contentEntity = MessageEntity.class)
@Entity(tableName = "messages_fts")
public class MessageFtsEntity {

    @ColumnInfo(name = "content")
    public String content;

    @ColumnInfo(name = "conversation_id")
    public String conversationId;
}
