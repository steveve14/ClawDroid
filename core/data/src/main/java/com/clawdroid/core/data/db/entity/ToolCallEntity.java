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
    tableName = "tool_calls",
    foreignKeys = @ForeignKey(
        entity = MessageEntity.class,
        parentColumns = "id",
        childColumns = "message_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {
        @Index(value = "message_id", name = "idx_tool_calls_message")
    }
)
public class ToolCallEntity {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    @ColumnInfo(name = "message_id")
    private String messageId;

    @NonNull
    @ColumnInfo(name = "tool_name")
    private String toolName;

    @NonNull
    @ColumnInfo(name = "tool_params")
    private String toolParams;

    @Nullable
    @ColumnInfo(name = "tool_result")
    private String toolResult;

    @NonNull
    private String status; // "pending", "success", "error"

    @Nullable
    @ColumnInfo(name = "duration_ms")
    private Integer durationMs;

    @NonNull
    @ColumnInfo(name = "created_at")
    private String createdAt;

    public ToolCallEntity() {
        this.id = UUID.randomUUID().toString();
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @NonNull public String getMessageId() { return messageId; }
    public void setMessageId(@NonNull String messageId) { this.messageId = messageId; }

    @NonNull public String getToolName() { return toolName; }
    public void setToolName(@NonNull String toolName) { this.toolName = toolName; }

    @NonNull public String getToolParams() { return toolParams; }
    public void setToolParams(@NonNull String toolParams) { this.toolParams = toolParams; }

    @Nullable public String getToolResult() { return toolResult; }
    public void setToolResult(@Nullable String toolResult) { this.toolResult = toolResult; }

    @NonNull public String getStatus() { return status; }
    public void setStatus(@NonNull String status) { this.status = status; }

    @Nullable public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(@Nullable Integer durationMs) { this.durationMs = durationMs; }

    @NonNull public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(@NonNull String createdAt) { this.createdAt = createdAt; }
}
