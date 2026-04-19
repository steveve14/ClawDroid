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
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

public class OllamaProvider implements AiProvider {

    private final OkHttpClient client;
    private final Gson gson;
    private final SettingsRepository settingsRepository;

    @Inject
    public OllamaProvider(OkHttpClient client, Gson gson,
                          SettingsRepository settingsRepository) {
        this.client = client.newBuilder()
                .readTimeout(300, TimeUnit.SECONDS)
                .build();
        this.gson = gson;
        this.settingsRepository = settingsRepository;
    }

    @Override
    public String getId() { return "ollama"; }

    @Override
    public String getName() { return "Ollama"; }

    @Override
    public ProviderType getType() { return ProviderType.OLLAMA; }

    @Override
    public Single<Boolean> isAvailable() {
        return Single.fromCallable(() -> {
            String endpoint = getEndpoint();
            if (endpoint == null || endpoint.isEmpty()) return false;
            try {
                Request request = new Request.Builder()
                        .url(endpoint + "/api/tags")
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
            String modelId = request.getModelId() != null ? request.getModelId() : "gemma3";

            JsonObject body = buildRequestBody(request, modelId, false);

            Request httpRequest = new Request.Builder()
                    .url(endpoint + "/api/chat")
                    .post(RequestBody.create(gson.toJson(body),
                            MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new AiProviderException("Ollama error: " + response.code());
                }
                String responseBody = response.body().string();
                JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                String content = json.getAsJsonObject("message")
                        .get("content").getAsString();
                return new AiResponse(content, "ollama", modelId,
                        null, null, null, null, "stop");
            }
        });
    }

    @Override
    public Observable<String> generateStream(AiRequest request) {
        return Observable.create(emitter -> {
            String endpoint = getEndpoint();
            String modelId = request.getModelId() != null ? request.getModelId() : "gemma3";

            JsonObject body = buildRequestBody(request, modelId, true);

            Request httpRequest = new Request.Builder()
                    .url(endpoint + "/api/chat")
                    .post(RequestBody.create(gson.toJson(body),
                            MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    emitter.onError(new AiProviderException(
                            "Ollama stream error: " + response.code()));
                    return;
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body().byteStream()));
                String line;
                while ((line = reader.readLine()) != null && !emitter.isDisposed()) {
                    if (!line.trim().isEmpty()) {
                        JsonObject json = gson.fromJson(line, JsonObject.class);
                        if (json.has("message")) {
                            String content = json.getAsJsonObject("message")
                                    .get("content").getAsString();
                            if (!content.isEmpty()) {
                                emitter.onNext(content);
                            }
                        }
                        if (json.has("done") && json.get("done").getAsBoolean()) {
                            break;
                        }
                    }
                }
                if (!emitter.isDisposed()) {
                    emitter.onComplete();
                }
            } catch (IOException e) {
                if (!emitter.isDisposed()) {
                    emitter.onError(new AiProviderException("Ollama stream failed", e));
                }
            }
        });
    }

    @Override
    public Single<List<ModelInfo>> listModels() {
        return Single.fromCallable(() -> {
            String endpoint = getEndpoint();
            Request request = new Request.Builder()
                    .url(endpoint + "/api/tags")
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    JsonObject json = gson.fromJson(response.body().string(), JsonObject.class);
                    JsonArray models = json.getAsJsonArray("models");
                    List<ModelInfo> result = new java.util.ArrayList<>();
                    for (int i = 0; i < models.size(); i++) {
                        JsonObject m = models.get(i).getAsJsonObject();
                        String name = m.get("name").getAsString();
                        result.add(new ModelInfo(name, name, "Ollama local model"));
                    }
                    return result;
                }
            }
            return Collections.<ModelInfo>emptyList();
        });
    }

    private String getEndpoint() {
        // Default Ollama endpoint
        return settingsRepository.getApiKey("ollama_endpoint");
    }

    private JsonObject buildRequestBody(AiRequest request, String modelId, boolean stream) {
        JsonObject body = new JsonObject();
        body.addProperty("model", modelId);
        body.addProperty("stream", stream);

        JsonArray messages = new JsonArray();
        for (AiMessage msg : request.getMessages()) {
            JsonObject message = new JsonObject();
            message.addProperty("role", msg.getRole());
            message.addProperty("content", msg.getContent());
            messages.add(message);
        }
        body.add("messages", messages);

        JsonObject options = new JsonObject();
        options.addProperty("temperature", request.getConfig().getTemperature());
        options.addProperty("top_p", request.getConfig().getTopP());
        options.addProperty("top_k", request.getConfig().getTopK());
        body.add("options", options);

        return body;
    }
}
