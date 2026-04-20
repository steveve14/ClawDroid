package com.clawdroid.feature.tools.builtin;

import android.content.Context;
import android.content.Intent;
import android.provider.AlarmClock;

import com.clawdroid.core.model.ToolResult;
import com.clawdroid.feature.tools.tool.Tool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.rxjava3.core.Single;

public class AlarmTool implements Tool {

    private final Context context;

    @Inject
    public AlarmTool(@ApplicationContext Context context) {
        this.context = context;
    }

    @Override public String getName() { return "alarm"; }

    @Override
    public String getDescription() {
        return "알람 또는 타이머를 설정합니다.";
    }

    @Override
    public JsonObject getParameters() {
        JsonObject params = new JsonObject();
        params.addProperty("type", "object");
        JsonObject properties = new JsonObject();

        JsonObject action = new JsonObject();
        action.addProperty("type", "string");
        JsonArray enumValues = new JsonArray();
        enumValues.add("alarm");
        enumValues.add("timer");
        action.add("enum", enumValues);
        action.addProperty("description", "alarm: 알람 설정, timer: 타이머 설정");
        properties.add("action", action);

        JsonObject hour = new JsonObject();
        hour.addProperty("type", "integer");
        hour.addProperty("description", "알람 시간 (0-23)");
        properties.add("hour", hour);

        JsonObject minute = new JsonObject();
        minute.addProperty("type", "integer");
        minute.addProperty("description", "알람 분 (0-59)");
        properties.add("minute", minute);

        JsonObject seconds = new JsonObject();
        seconds.addProperty("type", "integer");
        seconds.addProperty("description", "타이머 시간(초)");
        properties.add("seconds", seconds);

        JsonObject message = new JsonObject();
        message.addProperty("type", "string");
        message.addProperty("description", "알람/타이머 메시지");
        properties.add("message", message);

        params.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("action");
        params.add("required", required);
        return params;
    }

    @Override
    public Single<ToolResult> execute(JsonObject params) {
        return Single.fromCallable(() -> {
            String action = params.has("action") ? params.get("action").getAsString() : "alarm";
            String message = params.has("message") ? params.get("message").getAsString() : "ClawDroid";

            try {
                if ("timer".equals(action)) {
                    int seconds = params.has("seconds") ? params.get("seconds").getAsInt() : 60;
                    Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER);
                    intent.putExtra(AlarmClock.EXTRA_LENGTH, seconds);
                    intent.putExtra(AlarmClock.EXTRA_MESSAGE, message);
                    intent.putExtra(AlarmClock.EXTRA_SKIP_UI, false);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    return new ToolResult("alarm", true,
                            seconds + "초 타이머가 설정되었습니다.");
                } else {
                    int hour = params.has("hour") ? params.get("hour").getAsInt() : 7;
                    int minute = params.has("minute") ? params.get("minute").getAsInt() : 0;
                    Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
                    intent.putExtra(AlarmClock.EXTRA_HOUR, hour);
                    intent.putExtra(AlarmClock.EXTRA_MINUTES, minute);
                    intent.putExtra(AlarmClock.EXTRA_MESSAGE, message);
                    intent.putExtra(AlarmClock.EXTRA_SKIP_UI, false);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    return new ToolResult("alarm", true,
                            String.format("%02d:%02d 알람이 설정되었습니다.", hour, minute));
                }
            } catch (Exception e) {
                return new ToolResult("alarm", false, "알람 설정 오류: " + e.getMessage());
            }
        });
    }
}
