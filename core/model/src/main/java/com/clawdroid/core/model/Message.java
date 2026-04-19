package com.clawdroid.core.model;

import java.util.List;

public class Message {
    private String id;
    private String conversationId;
    private String role; // "user", "assistant", "system", "tool"
    private String content;
    private String modelProvider;
    private String modelId;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer durationMs;
    private String createdAt;
    private List<byte[]> images;

    public Message() {}

    public Message(String id, String conversationId, String role, String content) {
        this.id = id;
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getModelProvider() { return modelProvider; }
    public void setModelProvider(String modelProvider) { this.modelProvider = modelProvider; }

    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }

    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }

    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }

    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public List<byte[]> getImages() { return images; }
    public void setImages(List<byte[]> images) { this.images = images; }
}
