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
import java.util.Arrays;
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

public class GeminiCloudProvider implements AiProvider {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final OkHttpClient client;
    private final Gson gson;
    private final SettingsRepository settingsRepository;

    @Inject
    public GeminiCloudProvider(OkHttpClient client, Gson gson,
                                SettingsRepository settingsRepository) {
        this.client = client.newBuilder()
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
        this.gson = gson;
        this.settingsRepository = settingsRepository;
    }

    @Override
    public String getId() { return "gemini-cloud"; }

    @Override
    public String getName() { return "Gemini Cloud"; }

    @Override
    public ProviderType getType() { return ProviderType.GEMINI; }

    @Override
    public Single<Boolean> isAvailable() {
        String apiKey = settingsRepository.getApiKey("gemini-cloud");
        return Single.just(apiKey != null && !apiKey.isEmpty());
    }

    @Override
    public Single<AiResponse> generate(AiRequest request) {
        return Single.fromCallable(() -> {
            String apiKey = settingsRepository.getApiKey("gemini-cloud");
            String modelId = request.getModelId() != null ? request.getModelId() : "gemini-2.5-flash";

            JsonObject body = buildRequestBody(request);
            String url = BASE_URL + modelId + ":generateContent";

            Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", apiKey)
                    .post(RequestBody.create(gson.toJson(body),
                            MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new AiProviderException("Gemini Cloud error: " + response.code());
                }
                String responseBody = response.body().string();
                return parseResponse(responseBody, modelId);
            }
        });
    }

    @Override
    public Observable<String> generateStream(AiRequest request) {
        return Observable.create(emitter -> {
            String apiKey = settingsRepository.getApiKey("gemini-cloud");
            String modelId = request.getModelId() != null ? request.getModelId() : "gemini-2.5-flash";

            JsonObject body = buildRequestBody(request);
            String url = BASE_URL + modelId + ":streamGenerateContent?alt=sse";

            Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", apiKey)
                    .post(RequestBody.create(gson.toJson(body),
                            MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    emitter.onError(new AiProviderException(
                            "Gemini Cloud stream error: " + response.code()));
                    return;
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body().byteStream()));
                String line;
                while ((line = reader.readLine()) != null && !emitter.isDisposed()) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) break;
                        String text = extractTextFromSseData(data);
                        if (text != null && !text.isEmpty()) {
                            emitter.onNext(text);
                        }
                    }
                }
                if (!emitter.isDisposed()) {
                    emitter.onComplete();
                }
            } catch (IOException e) {
                if (!emitter.isDisposed()) {
                    emitter.onError(new AiProviderException("Gemini Cloud stream failed", e));
                }
            }
        });
    }

    @Override
    public Single<List<ModelInfo>> listModels() {
        return Single.just(Arrays.asList(
                new ModelInfo("gemini-2.5-pro", "Gemini 2.5 Pro",
                        "Cloud-based powerhouse for complex reasoning and coding."),
                new ModelInfo("gemini-2.5-flash", "Gemini 2.5 Flash",
                        "Fast and efficient for standard tasks.")
        ));
    }

    private JsonObject buildRequestBody(AiRequest request) {
        JsonObject body = new JsonObject();
        JsonArray contents = new JsonArray();

        for (AiMessage msg : request.getMessages()) {
            if ("system".equals(msg.getRole())) continue;
            JsonObject content = new JsonObject();
            content.addProperty("role", "user".equals(msg.getRole()) ? "user" : "model");
            JsonArray parts = new JsonArray();
            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", msg.getContent());
            parts.add(textPart);
            content.add("parts", parts);
            contents.add(content);
        }
        body.add("contents", contents);

        // System instruction
        for (AiMessage msg : request.getMessages()) {
            if ("system".equals(msg.getRole())) {
                JsonObject systemInstruction = new JsonObject();
                JsonArray parts = new JsonArray();
                JsonObject textPart = new JsonObject();
                textPart.addProperty("text", msg.getContent());
                parts.add(textPart);
                systemInstruction.add("parts", parts);
                body.add("systemInstruction", systemInstruction);
                break;
            }
        }

        // Generation config
        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", request.getConfig().getTemperature());
        genConfig.addProperty("topP", request.getConfig().getTopP());
        genConfig.addProperty("topK", request.getConfig().getTopK());
        genConfig.addProperty("maxOutputTokens", request.getConfig().getMaxOutputTokens());
        body.add("generationConfig", genConfig);

        return body;
    }

    private AiResponse parseResponse(String responseBody, String modelId) {
        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
        JsonArray candidates = json.getAsJsonArray("candidates");
        if (candidates != null && candidates.size() > 0) {
            JsonObject candidate = candidates.get(0).getAsJsonObject();
            JsonObject content = candidate.getAsJsonObject("content");
            JsonArray parts = content.getAsJsonArray("parts");
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < parts.size(); i++) {
                JsonObject part = parts.get(i).getAsJsonObject();
                if (part.has("text")) {
                    text.append(part.get("text").getAsString());
                }
            }
            return new AiResponse(text.toString(), "gemini-cloud", modelId,
                    null, null, null, null, "stop");
        }
        return new AiResponse("", "gemini-cloud", modelId,
                null, null, null, null, "error");
    }

    private String extractTextFromSseData(String data) {
        try {
            JsonObject json = gson.fromJson(data, JsonObject.class);
            JsonArray candidates = json.getAsJsonArray("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();
                JsonObject content = candidate.getAsJsonObject("content");
                if (content != null) {
                    JsonArray parts = content.getAsJsonArray("parts");
                    if (parts != null && parts.size() > 0) {
                        return parts.get(0).getAsJsonObject().get("text").getAsString();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
