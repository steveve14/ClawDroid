package com.clawdroid.core.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(tableName = "channels")
public class ChannelEntity {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    private String type; // "telegram", "discord", "slack", "gateway"

    @NonNull
    private String name;

    @NonNull
    private String config; // JSON

    @NonNull
    @ColumnInfo(name = "dm_policy")
    private String dmPolicy; // "pairing", "open", "closed"

    @Nullable
    @ColumnInfo(name = "system_prompt")
    private String systemPrompt;

    @Nullable
    @ColumnInfo(name = "model_provider")
    private String modelProvider;

    @Nullable
    @ColumnInfo(name = "model_id")
    private String modelId;

    @NonNull
    private String status; // "connected", "disconnected", "error"

    @NonNull
    @ColumnInfo(name = "created_at")
    private String createdAt;

    @NonNull
    @ColumnInfo(name = "updated_at")
    private String updatedAt;

    public ChannelEntity() {
        this.id = UUID.randomUUID().toString();
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @NonNull public String getType() { return type; }
    public void setType(@NonNull String type) { this.type = type; }

    @NonNull public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }

    @NonNull public String getConfig() { return config; }
    public void setConfig(@NonNull String config) { this.config = config; }

    @NonNull public String getDmPolicy() { return dmPolicy; }
    public void setDmPolicy(@NonNull String dmPolicy) { this.dmPolicy = dmPolicy; }

    @Nullable public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(@Nullable String systemPrompt) { this.systemPrompt = systemPrompt; }

    @Nullable public String getModelProvider() { return modelProvider; }
    public void setModelProvider(@Nullable String modelProvider) { this.modelProvider = modelProvider; }

    @Nullable public String getModelId() { return modelId; }
    public void setModelId(@Nullable String modelId) { this.modelId = modelId; }

    @NonNull public String getStatus() { return status; }
    public void setStatus(@NonNull String status) { this.status = status; }

    @NonNull public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(@NonNull String createdAt) { this.createdAt = createdAt; }

    @NonNull public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(@NonNull String updatedAt) { this.updatedAt = updatedAt; }
}
