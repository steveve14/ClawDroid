package com.clawdroid.feature.tools.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ToolParameterValidatorTest {

    private ToolParameterValidator validator;
    private JsonObject schema;

    @BeforeEach
    void setUp() {
        validator = new ToolParameterValidator();
        schema = new JsonObject();
        schema.addProperty("type", "object");

        JsonObject properties = new JsonObject();
        JsonObject action = new JsonObject();
        action.addProperty("type", "string");
        JsonArray enumValues = new JsonArray();
        enumValues.add("read");
        enumValues.add("write");
        action.add("enum", enumValues);
        properties.add("action", action);

        JsonObject title = new JsonObject();
        title.addProperty("type", "string");
        title.addProperty("maxLength", 5);
        properties.add("title", title);

        JsonObject count = new JsonObject();
        count.addProperty("type", "integer");
        count.addProperty("minimum", 1);
        count.addProperty("maximum", 3);
        properties.add("count", count);

        schema.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("action");
        schema.add("required", required);
    }

    @Test
    void validate_acceptsValidParams() {
        JsonObject params = new JsonObject();
        params.addProperty("action", "read");
        params.addProperty("title", "hello");
        params.addProperty("count", 2);

        assertNull(validator.validate("test", schema, params));
    }

    @Test
    void validate_rejectsMissingRequiredParam() {
        JsonObject params = new JsonObject();

        assertEquals("필수 파라미터가 누락되었습니다: action",
                validator.validate("test", schema, params));
    }

    @Test
    void validate_rejectsUnknownParam() {
        JsonObject params = new JsonObject();
        params.addProperty("action", "read");
        params.addProperty("extra", "value");

        assertEquals("허용되지 않은 파라미터입니다: extra",
                validator.validate("test", schema, params));
    }

    @Test
    void validate_rejectsEnumValueOutsideAllowList() {
        JsonObject params = new JsonObject();
        params.addProperty("action", "delete");

        assertEquals("action 파라미터 값이 허용 목록에 없습니다: delete",
                validator.validate("test", schema, params));
    }

    @Test
    void validate_rejectsTooLongString() {
        JsonObject params = new JsonObject();
        params.addProperty("action", "read");
        params.addProperty("title", "toolong");

        assertEquals("title 파라미터가 너무 깁니다 (최대 5자).",
                validator.validate("test", schema, params));
    }

    @Test
    void validate_rejectsIntegerOutsideBounds() {
        JsonObject params = new JsonObject();
        params.addProperty("action", "read");
        params.addProperty("count", 4);

        assertEquals("count 파라미터가 최대값보다 큽니다: 3",
                validator.validate("test", schema, params));
    }
}