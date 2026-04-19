package com.clawdroid.core.model;

import java.util.List;

public class AiResponse {
    private final String content;
    private final String modelProvider;
    private final String modelId;
    private final Integer inputTokens;
    private final Integer outputTokens;
    private final Long durationMs;
    private final List<ToolCall> toolCalls;
    private final String finishReason;

    public AiResponse(String content, String modelProvider, String modelId,
                      Integer inputTokens, Integer outputTokens, Long durationMs,
                      List<ToolCall> toolCalls, String finishReason) {
        this.content = content;
        this.modelProvider = modelProvider;
        this.modelId = modelId;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.durationMs = durationMs;
        this.toolCalls = toolCalls;
        this.finishReason = finishReason;
    }

    public String getContent() { return content; }
    public String getModelProvider() { return modelProvider; }
    public String getModelId() { return modelId; }
    public Integer getInputTokens() { return inputTokens; }
    public Integer getOutputTokens() { return outputTokens; }
    public Long getDurationMs() { return durationMs; }
    public List<ToolCall> getToolCalls() { return toolCalls; }
    public String getFinishReason() { return finishReason; }
}
