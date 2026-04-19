package com.clawdroid.core.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(
    tableName = "conversations",
    indices = {
        @Index(value = "updated_at", name = "idx_conversations_updated"),
        @Index(value = "channel_id", name = "idx_conversations_channel")
    }
)
public class ConversationEntity {

    @PrimaryKey
    @NonNull
    private String id;

    @Nullable
    private String title;

    @Nullable
    @ColumnInfo(name = "channel_id")
    private String channelId;

    @Nullable
    @ColumnInfo(name = "system_prompt")
    private String systemPrompt;

    @Nullable
    @ColumnInfo(name = "model_provider")
    private String modelProvider;

    @Nullable
    @ColumnInfo(name = "model_id")
    private String modelId;

    @ColumnInfo(name = "message_count")
    private int messageCount;

    @ColumnInfo(name = "token_count")
    private int tokenCount;

    @Nullable
    @ColumnInfo(name = "last_message_preview")
    private String lastMessagePreview;

    @ColumnInfo(name = "is_archived")
    private int isArchived;

    @NonNull
    @ColumnInfo(name = "created_at")
    private String createdAt;

    @NonNull
    @ColumnInfo(name = "updated_at")
    private String updatedAt;

    public ConversationEntity() {
        this.id = UUID.randomUUID().toString();
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @Nullable public String getTitle() { return title; }
    public void setTitle(@Nullable String title) { this.title = title; }

    @Nullable public String getChannelId() { return channelId; }
    public void setChannelId(@Nullable String channelId) { this.channelId = channelId; }

    @Nullable public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(@Nullable String systemPrompt) { this.systemPrompt = systemPrompt; }

    @Nullable public String getModelProvider() { return modelProvider; }
    public void setModelProvider(@Nullable String modelProvider) { this.modelProvider = modelProvider; }

    @Nullable public String getModelId() { return modelId; }
    public void setModelId(@Nullable String modelId) { this.modelId = modelId; }

    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }

    public int getTokenCount() { return tokenCount; }
    public void setTokenCount(int tokenCount) { this.tokenCount = tokenCount; }

    @Nullable public String getLastMessagePreview() { return lastMessagePreview; }
    public void setLastMessagePreview(@Nullable String lastMessagePreview) { this.lastMessagePreview = lastMessagePreview; }

    public int getIsArchived() { return isArchived; }
    public void setIsArchived(int isArchived) { this.isArchived = isArchived; }

    @NonNull public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(@NonNull String createdAt) { this.createdAt = createdAt; }

    @NonNull public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(@NonNull String updatedAt) { this.updatedAt = updatedAt; }
}
