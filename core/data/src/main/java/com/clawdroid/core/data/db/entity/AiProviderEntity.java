package com.clawdroid.core.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "ai_providers")
public class AiProviderEntity {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    private String type; // "nano", "gemini", "openai", "ollama", "custom"

    @NonNull
    private String name;

    @Nullable
    private String endpoint;

    @Nullable
    @ColumnInfo(name = "model_id")
    private String modelId;

    private int priority;

    @ColumnInfo(name = "is_enabled")
    private int isEnabled;

    @Nullable
    private String config; // JSON

    @NonNull
    @ColumnInfo(name = "created_at")
    private String createdAt;

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @NonNull public String getType() { return type; }
    public void setType(@NonNull String type) { this.type = type; }

    @NonNull public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }

    @Nullable public String getEndpoint() { return endpoint; }
    public void setEndpoint(@Nullable String endpoint) { this.endpoint = endpoint; }

    @Nullable public String getModelId() { return modelId; }
    public void setModelId(@Nullable String modelId) { this.modelId = modelId; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public int getIsEnabled() { return isEnabled; }
    public void setIsEnabled(int isEnabled) { this.isEnabled = isEnabled; }

    @Nullable public String getConfig() { return config; }
    public void setConfig(@Nullable String config) { this.config = config; }

    @NonNull public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(@NonNull String createdAt) { this.createdAt = createdAt; }
}
