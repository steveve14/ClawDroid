package com.clawdroid.core.model;

import java.util.List;

public class AiRequest {
    private final List<AiMessage> messages;
    private final String modelId;
    private final AiConfig config;
    private final List<ToolDefinition> tools;

    public AiRequest(List<AiMessage> messages, String modelId,
                     AiConfig config, List<ToolDefinition> tools) {
        this.messages = messages;
        this.modelId = modelId;
        this.config = config != null ? config : new AiConfig();
        this.tools = tools;
    }

    public List<AiMessage> getMessages() { return messages; }
    public String getModelId() { return modelId; }
    public AiConfig getConfig() { return config; }
    public List<ToolDefinition> getTools() { return tools; }
}
