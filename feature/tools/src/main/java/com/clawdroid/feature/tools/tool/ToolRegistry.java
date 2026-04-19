package com.clawdroid.feature.tools.tool;

import com.clawdroid.core.model.ToolDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

public class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();
    private final Map<String, Boolean> enabledState = new LinkedHashMap<>();

    public ToolRegistry() {}

    public void register(Tool tool) {
        tools.put(tool.getName(), tool);
        enabledState.put(tool.getName(), true);
    }

    public void unregister(String name) {
        tools.remove(name);
        enabledState.remove(name);
    }

    public Tool getTool(String name) {
        return tools.get(name);
    }

    public List<Tool> getAllTools() {
        return new ArrayList<>(tools.values());
    }

    public List<Tool> getEnabledTools() {
        List<Tool> enabled = new ArrayList<>();
        for (Map.Entry<String, Tool> entry : tools.entrySet()) {
            if (Boolean.TRUE.equals(enabledState.get(entry.getKey()))) {
                enabled.add(entry.getValue());
            }
        }
        return enabled;
    }

    public void setEnabled(String name, boolean enabled) {
        enabledState.put(name, enabled);
    }

    public boolean isEnabled(String name) {
        return Boolean.TRUE.equals(enabledState.get(name));
    }

    public List<ToolDefinition> getToolDefinitions() {
        List<ToolDefinition> defs = new ArrayList<>();
        for (Tool tool : getEnabledTools()) {
            defs.add(new ToolDefinition(
                    tool.getName(),
                    tool.getDescription(),
                    tool.getParameters().toString()));
        }
        return defs;
    }
}
