package com.clawdroid.feature.chat.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.clawdroid.core.data.db.entity.ConversationEntity;
import com.clawdroid.core.data.db.entity.MessageEntity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ExportHelper {

    private ExportHelper() {}

    public static String toMarkdown(ConversationEntity conversation, List<MessageEntity> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(conversation.getTitle() != null ? conversation.getTitle() : "대화").append("\n\n");
        sb.append("> 생성: ").append(conversation.getCreatedAt()).append("\n\n");
        sb.append("---\n\n");

        for (MessageEntity msg : messages) {
            if ("user".equals(msg.getRole())) {
                sb.append("## 👤 사용자\n\n");
            } else {
                sb.append("## 🤖 AI");
                if (msg.getModelId() != null) {
                    sb.append(" (").append(msg.getModelId()).append(")");
                }
                sb.append("\n\n");
            }
            sb.append(msg.getContent()).append("\n\n");
            sb.append("---\n\n");
        }

        return sb.toString();
    }

    public static String toJson(ConversationEntity conversation, List<MessageEntity> messages) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject root = new JsonObject();
        root.addProperty("title", conversation.getTitle());
        root.addProperty("created_at", conversation.getCreatedAt());
        root.addProperty("updated_at", conversation.getUpdatedAt());

        JsonArray msgArray = new JsonArray();
        for (MessageEntity msg : messages) {
            JsonObject obj = new JsonObject();
            obj.addProperty("role", msg.getRole());
            obj.addProperty("content", msg.getContent());
            obj.addProperty("created_at", msg.getCreatedAt());
            if (msg.getModelId() != null) {
                obj.addProperty("model_id", msg.getModelId());
            }
            if (msg.getModelProvider() != null) {
                obj.addProperty("model_provider", msg.getModelProvider());
            }
            msgArray.add(obj);
        }
        root.add("messages", msgArray);

        return gson.toJson(root);
    }

    public static void shareText(Context context, String content, String filename, String mimeType) {
        try {
            File dir = new File(context.getCacheDir(), "exports");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, filename);

            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8)) {
                writer.write(content);
            }

            Uri uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "내보내기"));
        } catch (IOException e) {
            // Silently fail
        }
    }
}
