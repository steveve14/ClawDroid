package com.clawdroid.core.ai;

import com.clawdroid.core.model.AiMessage;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PromptBuilder {

    @Inject
    public PromptBuilder() {}

    public List<AiMessage> build(String systemPrompt, List<AiMessage> history,
                                  String userMessage, String toolResults) {
        List<AiMessage> messages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new AiMessage("system", systemPrompt));
        }

        if (history != null) {
            messages.addAll(history);
        }

        if (toolResults != null && !toolResults.isEmpty()) {
            messages.add(new AiMessage("tool", toolResults));
        }

        messages.add(new AiMessage("user", userMessage));

        return messages;
    }
}
