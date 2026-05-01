package com.clawdroid.feature.tools.engine;

import com.clawdroid.core.ai.AiProviderManager;
import com.clawdroid.core.model.AiConfig;
import com.clawdroid.core.model.AiMessage;
import com.clawdroid.core.model.AiRequest;
import com.clawdroid.core.model.AiResponse;
import com.clawdroid.core.model.ToolCall;
import com.clawdroid.core.model.ToolDefinition;
import com.clawdroid.core.model.ToolResult;
import com.clawdroid.feature.tools.tool.ToolExecutor;
import com.clawdroid.feature.tools.tool.ToolRegistry;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

@Singleton
public class FunctionCallingEngine {

    private static final int MAX_TOOL_ROUNDS = 5;

    private final AiProviderManager aiProviderManager;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final NanoFunctionCallingParser nanoParser;

    @Inject
    public FunctionCallingEngine(AiProviderManager aiProviderManager,
                                  ToolRegistry toolRegistry,
                                  ToolExecutor toolExecutor,
                                  NanoFunctionCallingParser nanoParser) {
        this.aiProviderManager = aiProviderManager;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.nanoParser = nanoParser;
    }

    /**
     * Process a user request with function calling support.
     * Returns an Observable that emits the final response text (possibly in chunks).
     */
    public Observable<String> processWithTools(List<AiMessage> messages,
                                                String systemPrompt) {
        return processWithTools(messages, null, new AiConfig());
    }

    public Observable<String> processWithTools(List<AiMessage> messages,
                                                String modelId,
                                                AiConfig config) {
        return processWithTools(messages, modelId, config, null);
    }

    public Observable<String> processWithTools(List<AiMessage> messages,
                                                String modelId,
                                                AiConfig config,
                                                @Nullable String auditMessageId) {
        List<ToolDefinition> tools = toolRegistry.getToolDefinitions();
        if (tools.isEmpty()) {
            AiRequest request = new AiRequest(messages, modelId, config, null);
            return aiProviderManager.generateStream(request);
        }

        return processRound(withToolInstructions(messages, tools), modelId, config, tools, auditMessageId, 0);
    }

    private Observable<String> processRound(List<AiMessage> messages,
                                             String modelId,
                                             AiConfig config,
                                             List<ToolDefinition> tools,
                                             @Nullable String auditMessageId,
                                             int round) {
        if (round >= MAX_TOOL_ROUNDS) {
            return Observable.just("[최대 도구 호출 횟수에 도달했습니다]");
        }

        AiRequest request = new AiRequest(messages, modelId, config, tools);
        return aiProviderManager.generate(request)
                .subscribeOn(Schedulers.io())
                .flatMapObservable(response -> {
                    List<ToolCall> toolCalls = response.getToolCalls();

                    // If no native tool calls, try Nano-style parsing
                    if ((toolCalls == null || toolCalls.isEmpty()) && response.getContent() != null) {
                        NanoFunctionCallingParser.ParsedResponse parsed =
                                nanoParser.parse(response.getContent());
                        if (parsed.hasToolCalls()) {
                            toolCalls = parsed.getToolCalls();
                            // Add AI text response to history
                            if (!parsed.getTextContent().isEmpty()) {
                                messages.add(new AiMessage("assistant", parsed.getTextContent()));
                            }
                        }
                    }

                    if (toolCalls == null || toolCalls.isEmpty()) {
                        // No tool calls — return the final response
                        return Observable.just(response.getContent() != null
                                ? response.getContent() : "");
                    }

                    // Execute tool calls
                        return executeToolCalls(toolCalls, auditMessageId)
                            .flatMapObservable(results -> {
                                // Add assistant message and tool results to conversation
                                List<AiMessage> newMessages = new ArrayList<>(messages);
                                if (response.getContent() != null && !response.getContent().isEmpty()) {
                                    newMessages.add(new AiMessage("assistant", response.getContent()));
                                }

                                StringBuilder toolResultStr = new StringBuilder();
                                for (ToolResult result : results) {
                                    toolResultStr.append("[")
                                            .append(result.getToolName())
                                            .append("] ")
                                            .append(result.isSuccess() ? "성공" : "실패")
                                            .append(": ")
                                            .append(result.getResult())
                                            .append("\n");
                                }
                                newMessages.add(new AiMessage("tool", toolResultStr.toString()));

                                // Recursive call for next round
                                return processRound(newMessages, modelId, config, tools, auditMessageId, round + 1);
                            });
                });
    }

    private List<AiMessage> withToolInstructions(List<AiMessage> messages,
                                                  List<ToolDefinition> tools) {
        List<AiMessage> result = new ArrayList<>();
        String instructions = buildToolInstructions(tools);
        boolean applied = false;

        for (AiMessage message : messages) {
            if (!applied && "system".equals(message.getRole())) {
                result.add(new AiMessage("system", message.getContent() + "\n\n" + instructions));
                applied = true;
            } else {
                result.add(message);
            }
        }

        if (!applied) {
            result.add(0, new AiMessage("system", instructions));
        }
        return result;
    }

    private String buildToolInstructions(List<ToolDefinition> tools) {
        StringBuilder builder = new StringBuilder();
        builder.append("[도구 사용 지침]\n");
        builder.append("필요할 때만 다음 형식으로 도구 호출을 출력하세요. ");
        builder.append("<tool_call>{\"name\":\"도구명\",\"params\":{...}}</tool_call>\n");
        builder.append("사용 가능한 도구:\n");
        for (ToolDefinition tool : tools) {
            builder.append("- ")
                    .append(tool.getName())
                    .append(": ")
                    .append(tool.getDescription())
                    .append(" params=")
                    .append(tool.getParametersSchema())
                    .append("\n");
        }
        return builder.toString();
    }

    private Single<List<ToolResult>> executeToolCalls(List<ToolCall> toolCalls,
                                                       @Nullable String auditMessageId) {
        List<Single<ToolResult>> singles = new ArrayList<>();
        for (ToolCall call : toolCalls) {
            singles.add(toolExecutor.execute(call, auditMessageId).subscribeOn(Schedulers.io()));
        }
        return Single.zip(singles, results -> {
            List<ToolResult> list = new ArrayList<>();
            for (Object r : results) {
                list.add((ToolResult) r);
            }
            return list;
        });
    }
}
