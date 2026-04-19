package com.clawdroid.core.model;

import java.util.Map;

public class ToolCall {
    private final String name;
    private final Map<String, Object> params;

    public ToolCall(String name, Map<String, Object> params) {
        this.name = name;
        this.params = params;
    }

    public String getName() { return name; }
    public Map<String, Object> getParams() { return params; }
}
