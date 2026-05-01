package com.clawdroid.feature.tools.tool;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ToolParameterValidator {

    private static final int DEFAULT_MAX_STRING_LENGTH = 2048;

    @Inject
    public ToolParameterValidator() {}

    public String validate(String toolName, JsonObject schema, JsonObject params) {
        if (params == null) {
            return "파라미터는 JSON 객체여야 합니다.";
        }

        JsonObject properties = getObject(schema, "properties");
        String requiredError = validateRequired(schema, params);
        if (requiredError != null) return requiredError;

        for (Map.Entry<String, JsonElement> entry : params.entrySet()) {
            String name = entry.getKey();
            JsonElement value = entry.getValue();

            if (!properties.has(name)) {
                return "허용되지 않은 파라미터입니다: " + name;
            }
            if (value == null || value.isJsonNull()) {
                return name + " 파라미터는 null일 수 없습니다.";
            }

            JsonObject propertySchema = getObject(properties, name);
            String typeError = validateType(name, value, propertySchema);
            if (typeError != null) return typeError;

            String enumError = validateEnum(name, value, propertySchema);
            if (enumError != null) return enumError;

            String boundsError = validateBounds(name, value, propertySchema);
            if (boundsError != null) return boundsError;
        }

        return null;
    }

    private String validateRequired(JsonObject schema, JsonObject params) {
        if (schema == null || !schema.has("required") || !schema.get("required").isJsonArray()) {
            return null;
        }
        JsonArray required = schema.getAsJsonArray("required");
        for (JsonElement element : required) {
            String name = element.getAsString();
            if (!params.has(name) || params.get(name).isJsonNull()) {
                return "필수 파라미터가 누락되었습니다: " + name;
            }
        }
        return null;
    }

    private String validateType(String name, JsonElement value, JsonObject propertySchema) {
        if (propertySchema == null || !propertySchema.has("type")) return null;
        String type = propertySchema.get("type").getAsString();

        switch (type) {
            case "string":
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                    return name + " 파라미터는 문자열이어야 합니다.";
                }
                return null;
            case "integer":
                if (!isFiniteNumber(value) || Math.rint(value.getAsDouble()) != value.getAsDouble()) {
                    return name + " 파라미터는 정수여야 합니다.";
                }
                return null;
            case "number":
                if (!isFiniteNumber(value)) {
                    return name + " 파라미터는 숫자여야 합니다.";
                }
                return null;
            case "boolean":
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
                    return name + " 파라미터는 boolean이어야 합니다.";
                }
                return null;
            case "object":
                if (!value.isJsonObject()) {
                    return name + " 파라미터는 객체여야 합니다.";
                }
                return null;
            case "array":
                if (!value.isJsonArray()) {
                    return name + " 파라미터는 배열이어야 합니다.";
                }
                return null;
            default:
                return null;
        }
    }

    private String validateEnum(String name, JsonElement value, JsonObject propertySchema) {
        if (propertySchema == null || !propertySchema.has("enum")) return null;
        JsonArray enumValues = propertySchema.getAsJsonArray("enum");
        String actual = value.isJsonPrimitive() ? value.getAsString() : value.toString();
        for (JsonElement allowed : enumValues) {
            if (allowed.getAsString().equals(actual)) {
                return null;
            }
        }
        return name + " 파라미터 값이 허용 목록에 없습니다: " + actual;
    }

    private String validateBounds(String name, JsonElement value, JsonObject propertySchema) {
        if (propertySchema == null) return null;
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            int maxLength = propertySchema.has("maxLength")
                    ? propertySchema.get("maxLength").getAsInt()
                    : DEFAULT_MAX_STRING_LENGTH;
            if (value.getAsString().length() > maxLength) {
                return name + " 파라미터가 너무 깁니다 (최대 " + maxLength + "자).";
            }
        }

        if (isFiniteNumber(value)) {
            double numericValue = value.getAsDouble();
            if (propertySchema.has("minimum")
                    && numericValue < propertySchema.get("minimum").getAsDouble()) {
                return name + " 파라미터가 최소값보다 작습니다: "
                        + propertySchema.get("minimum").getAsString();
            }
            if (propertySchema.has("maximum")
                    && numericValue > propertySchema.get("maximum").getAsDouble()) {
                return name + " 파라미터가 최대값보다 큽니다: "
                        + propertySchema.get("maximum").getAsString();
            }
        }

        return null;
    }

    private boolean isFiniteNumber(JsonElement value) {
        if (!value.isJsonPrimitive()) return false;
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (!primitive.isNumber()) return false;
        try {
            double number = primitive.getAsDouble();
            return !Double.isNaN(number) && !Double.isInfinite(number);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private JsonObject getObject(JsonObject object, String name) {
        if (object == null || !object.has(name) || !object.get(name).isJsonObject()) {
            return new JsonObject();
        }
        return object.getAsJsonObject(name);
    }
}