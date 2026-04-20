package com.clawdroid.core.ai.provider;

import com.clawdroid.core.ai.AiProvider;
import com.clawdroid.core.ai.AiProviderException;
import com.clawdroid.core.data.repository.SettingsRepository;
import com.clawdroid.core.model.AiMessage;
import com.clawdroid.core.model.AiRequest;
import com.clawdroid.core.model.AiResponse;
import com.clawdroid.core.model.ModelInfo;
import com.clawdroid.core.model.ProviderType;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CustomProvider implements AiProvider {

    private final OkHttpClient client;
    private final Gson gson;
    private final SettingsRepository settingsRepository;

    @Inject
    public CustomProvider(OkHttpClient client, Gson gson,
                          SettingsRepository settingsRepository) {
        this.client = client.newBuilder()
                .readTimeout(180, TimeUnit.SECONDS)
                .build();
        this.gson = gson;
        this.settingsRepository = settingsRepository;
    }

    @Override
    public String getId() { return "custom"; }

    @Override
    public String getName() { return "Custom Endpoint"; }

    @Override
    public ProviderType getType() { return ProviderType.CUSTOM; }

    @Override
    public Single<Boolean> isAvailable() {
        return Single.fromCallable(() -> {
            String endpoint = getEndpoint();
            if (endpoint == null || endpoint.isEmpty()) return false;
            try {
                Request request = new Request.Builder()
                        .url(endpoint + "/v1/models")
                        .addHeader("Authorization", "Bearer " + getApiKey())
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    return response.isSuccessful();
                }
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Override
    public Single<AiResponse> generate(AiRequest request) {
        return Single.fromCallable(() -> {
            String endpoint = getEndpoint();
            String modelId = request.getModelId() != null ? request.getModelId() : "default";

            JsonObject body = buildRequestBody(request, modelId, false);

            Request httpRequest = new Request.Builder()
                    .url(endpoint + "/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(gson.toJson(body),
                            MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new AiProviderException("Custom endpoint error: " + response.code());
                }
                String responseBody = response.body().string();
                JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                String content = json.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
                return new AiResponse(content, "custom", modelId,
                        null, null, null, null, "stop");
            }
        });
    }

    @Override
    public Observable<String> generateStream(AiRequest request) {
        return Observable.create(emitter -> {
            String endpoint = getEndpoint();
            String modelId = request.getModelId() != null ? request.getModelId() : "default";

            JsonObject body = buildRequestBody(request, modelId, true);

            Request httpRequest = new Request.Builder()
                    .url(endpoint + "/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(gson.toJson(body),
                            MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    emitter.onError(new AiProviderException(
                            "Custom stream error: " + response.code()));
                    return;
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body().byteStream()));
                String line;
                while ((line = reader.readLine()) != null && !emitter.isDisposed()) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) break;
                        try {
                            JsonObject json = gson.fromJson(data, JsonObject.class);
                            JsonArray choices = json.getAsJsonArray("choices");
                            if (choices != null && choices.size() > 0) {
                                JsonObject delta = choices.get(0).getAsJsonObject()
                                        .getAsJsonObject("delta");
                                if (delta != null && delta.has("content")) {
                                    JsonElement contentEl = delta.get("content");
                                    if (!contentEl.isJsonNull()) {
                                        String content = contentEl.getAsString();
                                        if (!content.isEmpty()) {
                                            emitter.onNext(content);
                                        }
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
                if (!emitter.isDisposed()) {
                    emitter.onComplete();
                }
            } catch (IOException e) {
                if (!emitter.isDisposed()) {
                    emitter.onError(new AiProviderException("Custom stream failed", e));
                }
            }
        });
    }

    @Override
    public Single<List<ModelInfo>> listModels() {
        return Single.fromCallable(() -> {
            String endpoint = getEndpoint();
            if (endpoint == null || endpoint.isEmpty()) return Collections.<ModelInfo>emptyList();

            Request request = new Request.Builder()
                    .url(endpoint + "/v1/models")
                    .addHeader("Authorization", "Bearer " + getApiKey())
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    JsonObject json = gson.fromJson(response.body().string(), JsonObject.class);
                    JsonArray data = json.getAsJsonArray("data");
                    List<ModelInfo> result = new ArrayList<>();
                    if (data != null) {
                        for (int i = 0; i < data.size(); i++) {
                            JsonObject m = data.get(i).getAsJsonObject();
                            String id = m.get("id").getAsString();
                            result.add(new ModelInfo(id, id, "Custom model"));
                        }
                    }
                    return result;
                }
            } catch (Exception ignored) {}
            return Collections.<ModelInfo>emptyList();
        });
    }

    private String getEndpoint() {
        return settingsRepository.getApiKey("custom_endpoint");
    }

    private String getApiKey() {
        return settingsRepository.getApiKey("custom");
    }

    private JsonObject buildRequestBody(AiRequest request, String modelId, boolean stream) {
        JsonObject body = new JsonObject();
        body.addProperty("model", modelId);
        body.addProperty("stream", stream);
        if (request.getConfig() != null) {
            body.addProperty("temperature", request.getConfig().getTemperature());
            body.addProperty("top_p", request.getConfig().getTopP());
            body.addProperty("max_tokens", request.getConfig().getMaxOutputTokens());
        }

        JsonArray messages = new JsonArray();
        for (AiMessage msg : request.getMessages()) {
            JsonObject message = new JsonObject();
            message.addProperty("role", msg.getRole());
            message.addProperty("content", msg.getContent());
            messages.add(message);
        }
        body.add("messages", messages);

        return body;
    }
}
