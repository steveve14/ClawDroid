package com.clawdroid.core.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(
    tableName = "messages",
    foreignKeys = @ForeignKey(
        entity = ConversationEntity.class,
        parentColumns = "id",
        childColumns = "conversation_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {
        @Index(value = {"conversation_id", "created_at"}, name = "idx_messages_conversation")
    }
)
public class MessageEntity {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    @ColumnInfo(name = "conversation_id")
    private String conversationId;

    @NonNull
    private String role;

    @NonNull
    private String content;

    @Nullable
    @ColumnInfo(name = "model_provider")
    private String modelProvider;

    @Nullable
    @ColumnInfo(name = "model_id")
    private String modelId;

    @Nullable
    @ColumnInfo(name = "input_tokens")
    private Integer inputTokens;

    @Nullable
    @ColumnInfo(name = "output_tokens")
    private Integer outputTokens;

    @Nullable
    @ColumnInfo(name = "duration_ms")
    private Integer durationMs;

    @NonNull
    @ColumnInfo(name = "created_at")
    private String createdAt;

    public MessageEntity() {
        this.id = UUID.randomUUID().toString();
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @NonNull public String getConversationId() { return conversationId; }
    public void setConversationId(@NonNull String conversationId) { this.conversationId = conversationId; }

    @NonNull public String getRole() { return role; }
    public void setRole(@NonNull String role) { this.role = role; }

    @NonNull public String getContent() { return content; }
    public void setContent(@NonNull String content) { this.content = content; }

    @Nullable public String getModelProvider() { return modelProvider; }
    public void setModelProvider(@Nullable String modelProvider) { this.modelProvider = modelProvider; }

    @Nullable public String getModelId() { return modelId; }
    public void setModelId(@Nullable String modelId) { this.modelId = modelId; }

    @Nullable public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(@Nullable Integer inputTokens) { this.inputTokens = inputTokens; }

    @Nullable public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(@Nullable Integer outputTokens) { this.outputTokens = outputTokens; }

    @Nullable public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(@Nullable Integer durationMs) { this.durationMs = durationMs; }

    @NonNull public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(@NonNull String createdAt) { this.createdAt = createdAt; }
}
