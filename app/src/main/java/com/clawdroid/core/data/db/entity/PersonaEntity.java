package com.clawdroid.core.data.db.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(tableName = "personas")
public class PersonaEntity {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    private String name;

    @Nullable
    @ColumnInfo(name = "system_prompt")
    private String systemPrompt;

    @Nullable
    @ColumnInfo(name = "conversation_style")
    private String conversationStyle;

    @ColumnInfo(name = "is_active")
    private boolean isActive;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    public PersonaEntity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
    }

    // --- Getters & Setters ---

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @NonNull
    public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }

    @Nullable
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(@Nullable String systemPrompt) { this.systemPrompt = systemPrompt; }

    @Nullable
    public String getConversationStyle() { return conversationStyle; }
    public void setConversationStyle(@Nullable String conversationStyle) { this.conversationStyle = conversationStyle; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
