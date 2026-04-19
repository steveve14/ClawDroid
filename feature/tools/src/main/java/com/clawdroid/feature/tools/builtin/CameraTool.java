package com.clawdroid.feature.tools.builtin;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import com.clawdroid.core.model.ToolResult;
import com.clawdroid.feature.tools.tool.Tool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.rxjava3.core.Single;

public class CameraTool implements Tool {

    private final Context context;

    @Inject
    public CameraTool(@ApplicationContext Context context) {
        this.context = context;
    }

    @Override public String getName() { return "camera"; }

    @Override
    public String getDescription() {
        return "카메라를 열어 사진을 촬영합니다.";
    }

    @Override
    public List<String> getRequiredPermissions() {
        return Collections.singletonList(Manifest.permission.CAMERA);
    }

    @Override
    public JsonObject getParameters() {
        JsonObject params = new JsonObject();
        params.addProperty("type", "object");
        JsonObject properties = new JsonObject();

        JsonObject mode = new JsonObject();
        mode.addProperty("type", "string");
        JsonArray enumValues = new JsonArray();
        enumValues.add("photo");
        enumValues.add("video");
        mode.add("enum", enumValues);
        mode.addProperty("description", "photo: 사진 촬영, video: 동영상 촬영");
        properties.add("mode", mode);

        params.add("properties", properties);
        return params;
    }

    @Override
    public Single<ToolResult> execute(JsonObject params) {
        return Single.fromCallable(() -> {
            try {
                String mode = params.has("mode") ? params.get("mode").getAsString() : "photo";

                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(new Date());
                File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                File photoFile = File.createTempFile("PHOTO_" + timeStamp + "_", ".jpg", storageDir);

                Uri photoUri = FileProvider.getUriForFile(context,
                        context.getPackageName() + ".fileprovider", photoFile);

                Intent intent;
                if ("video".equals(mode)) {
                    intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                } else {
                    intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                context.startActivity(intent);

                return new ToolResult("camera", true,
                        "카메라가 열렸습니다. 파일: " + photoFile.getAbsolutePath());
            } catch (Exception e) {
                return new ToolResult("camera", false, "카메라 실행 오류: " + e.getMessage());
            }
        });
    }
}
