package com.clawdroid.feature.tools.tool;

import com.clawdroid.core.model.ToolCall;
import com.clawdroid.core.model.ToolResult;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;

@Singleton
public class ToolExecutor {

    private final ToolRegistry registry;
    private final Gson gson;

    @Inject
    public ToolExecutor(ToolRegistry registry, Gson gson) {
        this.registry = registry;
        this.gson = gson;
    }

    public Single<ToolResult> execute(ToolCall toolCall) {
        Tool tool = registry.getTool(toolCall.getName());
        if (tool == null) {
            return Single.just(new ToolResult(
                    toolCall.getName(), false,
                    "도구를 찾을 수 없습니다: " + toolCall.getName()));
        }

        if (!registry.isEnabled(toolCall.getName())) {
            return Single.just(new ToolResult(
                    toolCall.getName(), false,
                    "도구가 비활성화되어 있습니다: " + toolCall.getName()));
        }

        try {
            JsonObject params = gson.toJsonTree(toolCall.getParams()).getAsJsonObject();
            return tool.execute(params)
                    .onErrorReturn(e -> new ToolResult(
                            toolCall.getName(), false,
                            "도구 실행 오류: " + e.getMessage()));
        } catch (Exception e) {
            return Single.just(new ToolResult(
                    toolCall.getName(), false,
                    "파라미터 변환 오류: " + e.getMessage()));
        }
    }
}
