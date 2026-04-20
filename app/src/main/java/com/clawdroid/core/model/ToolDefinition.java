package com.clawdroid.core.model;

public class ToolDefinition {
    private final String name;
    private final String description;
    private final String parametersSchema; // JSON Schema

    public ToolDefinition(String name, String description, String parametersSchema) {
        this.name = name;
        this.description = description;
        this.parametersSchema = parametersSchema;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getParametersSchema() { return parametersSchema; }
}
