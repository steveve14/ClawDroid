package com.clawdroid.feature.tools.engine;

import com.clawdroid.core.model.ToolCall;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NanoFunctionCallingParser {

    private final Gson gson;
    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
            "<tool_call>\\s*(\\{.*?\\})\\s*</tool_call>",
            Pattern.DOTALL);

    @Inject
    public NanoFunctionCallingParser(Gson gson) {
        this.gson = gson;
    }

    public ParsedResponse parse(String response) {
        Matcher matcher = TOOL_CALL_PATTERN.matcher(response);
        List<ToolCall> toolCalls = new ArrayList<>();

        while (matcher.find()) {
            try {
                JsonObject json = gson.fromJson(matcher.group(1), JsonObject.class);
                String name = json.has("name") ? json.get("name").getAsString() : null;
                if (name == null) continue;

                @SuppressWarnings("unchecked")
                Map<String, Object> params = json.has("params")
                        ? gson.fromJson(json.get("params"), Map.class)
                        : Collections.emptyMap();

                toolCalls.add(new ToolCall(name, params));
            } catch (Exception e) {
                // Skip unparseable tool calls
            }
        }

        String textContent = TOOL_CALL_PATTERN.matcher(response).replaceAll("").trim();
        return new ParsedResponse(textContent, toolCalls);
    }

    public static class ParsedResponse {
        private final String textContent;
        private final List<ToolCall> toolCalls;

        public ParsedResponse(String textContent, List<ToolCall> toolCalls) {
            this.textContent = textContent;
            this.toolCalls = toolCalls;
        }

        public String getTextContent() { return textContent; }
        public List<ToolCall> getToolCalls() { return toolCalls; }
        public boolean hasToolCalls() { return !toolCalls.isEmpty(); }
    }
}
