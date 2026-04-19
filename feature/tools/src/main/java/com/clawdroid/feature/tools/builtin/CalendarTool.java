package com.clawdroid.feature.tools.builtin;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.CalendarContract;

import com.clawdroid.core.model.ToolResult;
import com.clawdroid.feature.tools.tool.Tool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.rxjava3.core.Single;

public class CalendarTool implements Tool {

    private final Context context;

    @Inject
    public CalendarTool(@ApplicationContext Context context) {
        this.context = context;
    }

    @Override public String getName() { return "calendar"; }

    @Override
    public String getDescription() {
        return "캘린더에서 일정을 조회하거나 새 일정을 추가합니다.";
    }

    @Override
    public List<String> getRequiredPermissions() {
        return Arrays.asList(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR);
    }

    @Override
    public JsonObject getParameters() {
        JsonObject params = new JsonObject();
        params.addProperty("type", "object");
        JsonObject properties = new JsonObject();

        JsonObject action = new JsonObject();
        action.addProperty("type", "string");
        JsonArray enumValues = new JsonArray();
        enumValues.add("read");
        enumValues.add("create");
        action.add("enum", enumValues);
        action.addProperty("description", "read: 일정 조회, create: 일정 추가");
        properties.add("action", action);

        JsonObject date = new JsonObject();
        date.addProperty("type", "string");
        date.addProperty("description", "날짜 (yyyy-MM-dd 형식)");
        properties.add("date", date);

        JsonObject title = new JsonObject();
        title.addProperty("type", "string");
        title.addProperty("description", "일정 제목 (create 시 필수)");
        properties.add("title", title);

        JsonObject duration = new JsonObject();
        duration.addProperty("type", "integer");
        duration.addProperty("description", "일정 시간(분, 기본 60)");
        properties.add("duration", duration);

        params.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("action");
        params.add("required", required);
        return params;
    }

    @Override
    public Single<ToolResult> execute(JsonObject params) {
        return Single.fromCallable(() -> {
            String action = params.has("action") ? params.get("action").getAsString() : "read";
            switch (action) {
                case "create": return createEvent(params);
                case "read":
                default: return readEvents(params);
            }
        });
    }

    private ToolResult readEvents(JsonObject params) {
        try {
            long startMillis;
            long endMillis;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            if (params.has("date")) {
                Date date = sdf.parse(params.get("date").getAsString());
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                startMillis = cal.getTimeInMillis();
                cal.add(Calendar.DAY_OF_MONTH, 1);
                endMillis = cal.getTimeInMillis();
            } else {
                startMillis = System.currentTimeMillis();
                endMillis = startMillis + 7L * 24 * 60 * 60 * 1000; // 7 days
            }

            String[] projection = {
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.DTEND,
                    CalendarContract.Events.EVENT_LOCATION
            };

            ContentResolver cr = context.getContentResolver();
            Cursor cursor = cr.query(
                    CalendarContract.Events.CONTENT_URI,
                    projection,
                    CalendarContract.Events.DTSTART + " >= ? AND " +
                            CalendarContract.Events.DTSTART + " < ?",
                    new String[]{String.valueOf(startMillis), String.valueOf(endMillis)},
                    CalendarContract.Events.DTSTART + " ASC");

            StringBuilder sb = new StringBuilder();
            SimpleDateFormat timeFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            int count = 0;
            if (cursor != null) {
                while (cursor.moveToNext() && count < 20) {
                    String eventTitle = cursor.getString(0);
                    long dtStart = cursor.getLong(1);
                    long dtEnd = cursor.getLong(2);
                    String location = cursor.getString(3);

                    sb.append(++count).append(". ").append(eventTitle).append("\n");
                    sb.append("   ").append(timeFmt.format(new Date(dtStart)));
                    if (dtEnd > 0) sb.append(" ~ ").append(timeFmt.format(new Date(dtEnd)));
                    sb.append("\n");
                    if (location != null && !location.isEmpty()) {
                        sb.append("   📍 ").append(location).append("\n");
                    }
                }
                cursor.close();
            }

            return new ToolResult("calendar", true,
                    count > 0 ? sb.toString() : "일정이 없습니다.");
        } catch (Exception e) {
            return new ToolResult("calendar", false, "일정 조회 오류: " + e.getMessage());
        }
    }

    private ToolResult createEvent(JsonObject params) {
        try {
            String title = params.has("title") ? params.get("title").getAsString() : null;
            if (title == null || title.isEmpty()) {
                return new ToolResult("calendar", false, "일정 제목이 필요합니다.");
            }

            long startMillis;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            if (params.has("date")) {
                startMillis = sdf.parse(params.get("date").getAsString()).getTime();
            } else {
                startMillis = System.currentTimeMillis();
            }

            int durationMinutes = params.has("duration") ? params.get("duration").getAsInt() : 60;
            long endMillis = startMillis + durationMinutes * 60L * 1000;

            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.TITLE, title);
            values.put(CalendarContract.Events.DTSTART, startMillis);
            values.put(CalendarContract.Events.DTEND, endMillis);
            values.put(CalendarContract.Events.CALENDAR_ID, 1);
            values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());

            context.getContentResolver().insert(CalendarContract.Events.CONTENT_URI, values);
            return new ToolResult("calendar", true,
                    "일정이 추가되었습니다: " + title + " (" + sdf.format(new Date(startMillis)) + ")");
        } catch (Exception e) {
            return new ToolResult("calendar", false, "일정 생성 오류: " + e.getMessage());
        }
    }
}
