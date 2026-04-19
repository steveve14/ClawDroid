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

public class OpenAiProvider implements AiProvider {

    private static final String BASE_URL = "https://api.openai.com/v1/chat/completions";

    private final OkHttpClient client;
    private final Gson gson;
    private final SettingsRepository settingsRepository;

    @Inject
    public OpenAiProvider(OkHttpClient client, Gson gson,
                          SettingsRepository settingsRepository) {
        this.client = client.newBuilder()
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
        this.gson = gson;
        this.settingsRepository = settingsRepository;
    }

    @Override
    public String getId() { return "openai"; }

    @Override
    public String getName() { return "OpenAI"; }

    @Override
    public ProviderType getType() { return ProviderType.OPENAI; }

    @Override
    public Single<Boolean> isAvailable() {
        String apiKey = settingsRepository.getApiKey("openai");
        return Single.just(apiKey != null && !apiKey.isEmpty());
    }

    @Override
    public Single<AiResponse> generate(AiRequest request) {
        return Single.fromCallable(() -> {
            String apiKey = settingsRepository.getApiKey("openai");
            String modelId = request.getModelId() != null ? request.getModelId() : "gpt-4o-mini";

            JsonObject body = buildRequestBody(request, modelId, false);

            Request httpRequest = new Request.Builder()
                    .url(BASE_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .post(RequestBody.create(gson.toJson(body),
                            MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new AiProviderException("OpenAI error: " + response.code());
                }
                String responseBody = response.body().string();
                return parseResponse(responseBody, modelId);
            }
        });
    }

    @Override
    public Observable<String> generateStream(AiRequest request) {
        return Observable.create(emitter -> {
            String apiKey = settingsRepository.getApiKey("openai");
            String modelId = request.getModelId() != null ? request.getModelId() : "gpt-4o-mini";

            JsonObject body = buildRequestBody(request, modelId, true);

            Request httpRequest = new Request.Builder()
                    .url(BASE_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .post(RequestBody.create(gson.toJson(body),
                            MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    emitter.onError(new AiProviderException(
                            "OpenAI stream error: " + response.code()));
                    return;
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body().byteStream()));
                String line;
                while ((line = reader.readLine()) != null && !emitter.isDisposed()) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) break;
                        String text = extractDelta(data);
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
                    emitter.onError(new AiProviderException("OpenAI stream failed", e));
                }
            }
        });
    }

    @Override
    public Single<List<ModelInfo>> listModels() {
        return Single.just(Arrays.asList(
                new ModelInfo("gpt-4o", "GPT-4o", "Most capable model for complex tasks."),
                new ModelInfo("gpt-4o-mini", "GPT-4o Mini", "Fast and cost-effective.")
        ));
    }

    private JsonObject buildRequestBody(AiRequest request, String modelId, boolean stream) {
        JsonObject body = new JsonObject();
        body.addProperty("model", modelId);
        body.addProperty("stream", stream);
        body.addProperty("temperature", request.getConfig().getTemperature());
        body.addProperty("top_p", request.getConfig().getTopP());
        body.addProperty("max_tokens", request.getConfig().getMaxOutputTokens());

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

    private AiResponse parseResponse(String responseBody, String modelId) {
        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
        JsonArray choices = json.getAsJsonArray("choices");
        if (choices != null && choices.size() > 0) {
            JsonObject message = choices.get(0).getAsJsonObject()
                    .getAsJsonObject("message");
            String content = message.get("content").getAsString();
            return new AiResponse(content, "openai", modelId,
                    null, null, null, null, "stop");
        }
        return new AiResponse("", "openai", modelId,
                null, null, null, null, "error");
    }

    private String extractDelta(String data) {
        try {
            JsonObject json = gson.fromJson(data, JsonObject.class);
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                JsonObject delta = choices.get(0).getAsJsonObject()
                        .getAsJsonObject("delta");
                if (delta != null && delta.has("content")) {
                    return delta.get("content").getAsString();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
