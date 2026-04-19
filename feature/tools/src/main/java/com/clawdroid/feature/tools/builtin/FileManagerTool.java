package com.clawdroid.feature.tools.builtin;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.clawdroid.core.model.ToolResult;
import com.clawdroid.feature.tools.tool.Tool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.rxjava3.core.Single;

public class FileManagerTool implements Tool {

    private final Context context;

    @Inject
    public FileManagerTool(@ApplicationContext Context context) {
        this.context = context;
    }

    @Override public String getName() { return "file_reader"; }

    @Override
    public String getDescription() {
        return "파일의 내용을 읽거나 디렉토리의 파일 목록을 조회합니다.";
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
        enumValues.add("list");
        action.add("enum", enumValues);
        action.addProperty("description", "read: 파일 읽기, list: 디렉토리 목록");
        properties.add("action", action);

        JsonObject path = new JsonObject();
        path.addProperty("type", "string");
        path.addProperty("description", "파일 또는 디렉토리 경로");
        properties.add("path", path);

        params.add("properties", properties);
        JsonArray required = new JsonArray();
        required.add("action");
        required.add("path");
        params.add("required", required);
        return params;
    }

    @Override
    public Single<ToolResult> execute(JsonObject params) {
        return Single.fromCallable(() -> {
            String action = params.has("action") ? params.get("action").getAsString() : "read";
            String path = params.has("path") ? params.get("path").getAsString() : null;
            if (path == null || path.isEmpty()) {
                return new ToolResult("file_reader", false, "path 파라미터가 필요합니다.");
            }

            // Validate path - prevent directory traversal
            File file = new File(path).getCanonicalFile();
            File appDir = context.getExternalFilesDir(null);
            if (appDir != null && !file.getPath().startsWith(appDir.getCanonicalPath())) {
                // Only allow reading from app-specific directory for security
                File clawDroidDir = new File(android.os.Environment.getExternalStorageDirectory(), "ClawDroid");
                if (!file.getPath().startsWith(clawDroidDir.getCanonicalPath())) {
                    return new ToolResult("file_reader", false,
                            "앱 전용 디렉토리 또는 ClawDroid 디렉토리만 접근할 수 있습니다.");
                }
            }

            switch (action) {
                case "list": return listDirectory(file);
                case "read":
                default: return readFile(file);
            }
        });
    }

    private ToolResult readFile(File file) {
        try {
            if (!file.exists()) {
                return new ToolResult("file_reader", false, "파일이 존재하지 않습니다: " + file.getPath());
            }
            if (!file.isFile()) {
                return new ToolResult("file_reader", false, "디렉토리입니다. action=list를 사용하세요.");
            }
            if (file.length() > 100 * 1024) { // 100KB limit
                return new ToolResult("file_reader", false, "파일이 너무 큽니다 (최대 100KB).");
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            return new ToolResult("file_reader", true, sb.toString());
        } catch (Exception e) {
            return new ToolResult("file_reader", false, "파일 읽기 오류: " + e.getMessage());
        }
    }

    private ToolResult listDirectory(File dir) {
        try {
            if (!dir.exists()) {
                return new ToolResult("file_reader", false, "디렉토리가 존재하지 않습니다: " + dir.getPath());
            }
            if (!dir.isDirectory()) {
                return new ToolResult("file_reader", false, "파일입니다. action=read를 사용하세요.");
            }

            File[] files = dir.listFiles();
            if (files == null || files.length == 0) {
                return new ToolResult("file_reader", true, "빈 디렉토리입니다.");
            }

            StringBuilder sb = new StringBuilder();
            for (File f : files) {
                sb.append(f.isDirectory() ? "📁 " : "📄 ")
                        .append(f.getName());
                if (f.isFile()) {
                    sb.append(" (").append(formatSize(f.length())).append(")");
                }
                sb.append("\n");
            }
            return new ToolResult("file_reader", true, sb.toString());
        } catch (Exception e) {
            return new ToolResult("file_reader", false, "디렉토리 목록 오류: " + e.getMessage());
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + "KB";
        return String.format("%.1fMB", bytes / (1024.0 * 1024));
    }
}
