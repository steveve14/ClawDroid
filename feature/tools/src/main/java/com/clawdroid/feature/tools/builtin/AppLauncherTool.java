package com.clawdroid.feature.tools.builtin;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.clawdroid.core.model.ToolResult;
import com.clawdroid.feature.tools.tool.Tool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.rxjava3.core.Single;

public class AppLauncherTool implements Tool {

    private final Context context;

    @Inject
    public AppLauncherTool(@ApplicationContext Context context) {
        this.context = context;
    }

    @Override public String getName() { return "app_launcher"; }

    @Override
    public String getDescription() {
        return "설치된 앱을 실행합니다. 앱 이름 또는 패키지명으로 실행할 수 있습니다.";
    }

    @Override
    public JsonObject getParameters() {
        JsonObject params = new JsonObject();
        params.addProperty("type", "object");
        JsonObject properties = new JsonObject();

        JsonObject action = new JsonObject();
        action.addProperty("type", "string");
        JsonArray enumValues = new JsonArray();
        enumValues.add("launch");
        enumValues.add("list");
        action.add("enum", enumValues);
        action.addProperty("description", "launch: 앱 실행, list: 설치된 앱 목록");
        properties.add("action", action);

        JsonObject appName = new JsonObject();
        appName.addProperty("type", "string");
        appName.addProperty("description", "실행할 앱 이름 또는 패키지명");
        properties.add("app_name", appName);

        params.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("action");
        params.add("required", required);
        return params;
    }

    @Override
    public Single<ToolResult> execute(JsonObject params) {
        return Single.fromCallable(() -> {
            String action = params.has("action") ? params.get("action").getAsString() : "launch";

            if ("list".equals(action)) {
                return listApps();
            }

            String appName = params.has("app_name") ? params.get("app_name").getAsString() : null;
            if (appName == null || appName.isEmpty()) {
                return new ToolResult("app_launcher", false, "app_name 파라미터가 필요합니다.");
            }
            return launchApp(appName);
        });
    }

    private ToolResult launchApp(String appName) {
        try {
            PackageManager pm = context.getPackageManager();

            // Try as package name first
            Intent launchIntent = pm.getLaunchIntentForPackage(appName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
                return new ToolResult("app_launcher", true, appName + " 앱이 실행되었습니다.");
            }

            // Search by app name
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);

            String lowerName = appName.toLowerCase();
            for (ResolveInfo info : apps) {
                String label = info.loadLabel(pm).toString().toLowerCase();
                if (label.contains(lowerName)) {
                    Intent intent = pm.getLaunchIntentForPackage(
                            info.activityInfo.packageName);
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                        return new ToolResult("app_launcher", true,
                                info.loadLabel(pm) + " 앱이 실행되었습니다.");
                    }
                }
            }

            return new ToolResult("app_launcher", false,
                    "'" + appName + "' 앱을 찾을 수 없습니다.");
        } catch (Exception e) {
            return new ToolResult("app_launcher", false, "앱 실행 오류: " + e.getMessage());
        }
    }

    private ToolResult listApps() {
        try {
            PackageManager pm = context.getPackageManager();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);

            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (ResolveInfo info : apps) {
                if (count >= 30) {
                    sb.append("... 외 ").append(apps.size() - 30).append("개");
                    break;
                }
                sb.append(++count).append(". ")
                        .append(info.loadLabel(pm))
                        .append(" (").append(info.activityInfo.packageName).append(")\n");
            }

            return new ToolResult("app_launcher", true, sb.toString());
        } catch (Exception e) {
            return new ToolResult("app_launcher", false, "앱 목록 오류: " + e.getMessage());
        }
    }
}
