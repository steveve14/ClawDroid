package com.clawdroid.feature.tools.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.clawdroid.core.data.db.dao.ToolCallDao;
import com.clawdroid.core.data.db.entity.ToolCallEntity;
import com.clawdroid.core.model.ToolCall;
import com.clawdroid.core.model.ToolResult;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.Map;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

class ToolExecutorTest {

    private ToolRegistry registry;
    private ToolCallDao toolCallDao;
    private ToolExecutor executor;

    @BeforeEach
    void setUp() {
        registry = new ToolRegistry();
        toolCallDao = mock(ToolCallDao.class);
        when(toolCallDao.insert(any(ToolCallEntity.class))).thenReturn(Completable.complete());
        executor = new ToolExecutor(
                registry,
                new Gson(),
                mock(Context.class),
                new ToolParameterValidator(),
                toolCallDao);
    }

    @Test
    void execute_withAuditMessageId_persistsSuccessfulToolCall() {
        registry.register(new EchoTool());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("text", "hello");

        ToolResult result = executor.execute(new ToolCall("echo", params), "message-1")
                .blockingGet();

        assertTrue(result.isSuccess());
        ArgumentCaptor<ToolCallEntity> captor = ArgumentCaptor.forClass(ToolCallEntity.class);
        verify(toolCallDao).insert(captor.capture());
        ToolCallEntity entity = captor.getValue();
        assertEquals("message-1", entity.getMessageId());
        assertEquals("echo", entity.getToolName());
        assertEquals("success", entity.getStatus());
        assertEquals("hello", entity.getToolResult());
        assertEquals("{\"text\":\"hello\"}", entity.getToolParams());
    }

    private static class EchoTool implements Tool {

        @Override
        public String getName() {
            return "echo";
        }

        @Override
        public String getDescription() {
            return "Echoes text.";
        }

        @Override
        public JsonObject getParameters() {
            JsonObject params = new JsonObject();
            params.addProperty("type", "object");
            JsonObject properties = new JsonObject();
            JsonObject text = new JsonObject();
            text.addProperty("type", "string");
            text.addProperty("maxLength", 20);
            properties.add("text", text);
            params.add("properties", properties);
            JsonArray required = new JsonArray();
            required.add("text");
            params.add("required", required);
            return params;
        }

        @Override
        public Single<ToolResult> execute(JsonObject params) {
            return Single.just(new ToolResult("echo", true, params.get("text").getAsString()));
        }
    }
}