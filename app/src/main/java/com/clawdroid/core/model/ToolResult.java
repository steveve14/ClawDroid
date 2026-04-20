package com.clawdroid.core.model;

public class ToolResult {
    private final String toolName;
    private final boolean success;
    private final String result;

    public ToolResult(String toolName, boolean success, String result) {
        this.toolName = toolName;
        this.success = success;
        this.result = result;
    }

    public String getToolName() { return toolName; }
    public boolean isSuccess() { return success; }
    public String getResult() { return result; }
}
