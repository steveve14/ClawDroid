package com.clawdroid.feature.tools.tool;

import com.clawdroid.core.model.ToolResult;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Single;

public interface Tool {
    String getName();
    String getDescription();
    JsonObject getParameters();

    default List<String> getRequiredPermissions() {
        return Collections.emptyList();
    }

    Single<ToolResult> execute(JsonObject params);
}
