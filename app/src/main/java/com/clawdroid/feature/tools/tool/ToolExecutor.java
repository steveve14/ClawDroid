package com.clawdroid.feature.tools.tool;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.clawdroid.core.data.db.dao.ToolCallDao;
import com.clawdroid.core.data.db.entity.ToolCallEntity;
import com.clawdroid.core.model.ToolCall;
import com.clawdroid.core.model.ToolResult;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.time.Instant;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Singleton
public class ToolExecutor {

    private final ToolRegistry registry;
    private final Gson gson;
    private final Context context;
    private final ToolParameterValidator parameterValidator;
    private final ToolCallDao toolCallDao;

    @Inject
    public ToolExecutor(ToolRegistry registry,
                        Gson gson,
                        @ApplicationContext Context context,
                        ToolParameterValidator parameterValidator,
                        ToolCallDao toolCallDao) {
        this.registry = registry;
        this.gson = gson;
        this.context = context;
        this.parameterValidator = parameterValidator;
        this.toolCallDao = toolCallDao;
    }

    public Single<ToolResult> execute(ToolCall toolCall) {
        return execute(toolCall, null);
    }

    public Single<ToolResult> execute(ToolCall toolCall, @Nullable String auditMessageId) {
        long startTime = System.currentTimeMillis();
        if (toolCall == null || toolCall.getName() == null || toolCall.getName().isEmpty()) {
            return Single.just(new ToolResult("unknown", false, "도구 이름이 필요합니다."));
        }

        Tool tool = registry.getTool(toolCall.getName());
        if (tool == null) {
            return recordAndReturn(auditMessageId, toolCall, null, new ToolResult(
                    toolCall.getName(), false, "도구를 찾을 수 없습니다: " + toolCall.getName()), startTime);
        }

        if (!registry.isEnabled(toolCall.getName())) {
            return recordAndReturn(auditMessageId, toolCall, null, new ToolResult(
                    toolCall.getName(), false, "도구가 비활성화되어 있습니다: " + toolCall.getName()), startTime);
        }

        JsonObject params;
        try {
            params = toJsonObject(toolCall);
            String validationError = parameterValidator.validate(
                    tool.getName(), tool.getParameters(), params);
            if (validationError != null) {
                return recordAndReturn(auditMessageId, toolCall, params,
                        new ToolResult(tool.getName(), false, validationError), startTime);
            }

            ToolResult permissionError = validatePermissions(tool);
            if (permissionError != null) {
                return recordAndReturn(auditMessageId, toolCall, params, permissionError, startTime);
            }
        } catch (Exception e) {
            return recordAndReturn(auditMessageId, toolCall, null, new ToolResult(
                    toolCall.getName(), false, "파라미터 변환 오류: " + e.getMessage()), startTime);
        }

        ToolCallEntity audit = createAudit(auditMessageId, toolCall, params);
        return tool.execute(params)
                        .onErrorReturn(e -> new ToolResult(
                                toolCall.getName(), false,
                    "도구 실행 오류: " + e.getMessage()))
                .flatMap(result -> finishAudit(audit, result, startTime)
                        .andThen(Single.just(result)));
    }

    private JsonObject toJsonObject(ToolCall toolCall) {
        if (toolCall.getParams() == null) {
            return new JsonObject();
        }
        JsonElement element = gson.toJsonTree(toolCall.getParams());
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("파라미터는 JSON 객체여야 합니다.");
        }
        return element.getAsJsonObject();
    }

    private ToolResult validatePermissions(Tool tool) {
        for (String permission : tool.getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return new ToolResult(tool.getName(), false,
                        "필수 권한이 허용되지 않았습니다: " + permission);
            }
        }
        return null;
    }

    private Single<ToolResult> recordAndReturn(@Nullable String auditMessageId,
                                                ToolCall toolCall,
                                                @Nullable JsonObject params,
                                                ToolResult result,
                                                long startTime) {
        ToolCallEntity audit = createAudit(auditMessageId, toolCall, params);
        return finishAudit(audit, result, startTime).andThen(Single.just(result));
    }

    private ToolCallEntity createAudit(@Nullable String auditMessageId,
                                        ToolCall toolCall,
                                        @Nullable JsonObject params) {
        if (auditMessageId == null || auditMessageId.isEmpty()) return null;
        ToolCallEntity entity = new ToolCallEntity();
        entity.setMessageId(auditMessageId);
        entity.setToolName(toolCall.getName());
        entity.setToolParams(params != null ? params.toString() : gson.toJson(toolCall.getParams()));
        entity.setStatus("pending");
        entity.setCreatedAt(Instant.now().toString());
        return entity;
    }

    private Completable finishAudit(@Nullable ToolCallEntity audit,
                                     ToolResult result,
                                     long startTime) {
        if (audit == null) return Completable.complete();
        int durationMs = (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - startTime);
        audit.setStatus(result.isSuccess() ? "success" : "error");
        audit.setToolResult(result.getResult());
        audit.setDurationMs(durationMs);
        return toolCallDao.insert(audit).onErrorComplete();
    }
}
