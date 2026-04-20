package com.clawdroid.core.ai.provider;

import android.content.Context;

import com.clawdroid.core.ai.AiProvider;
import com.clawdroid.core.ai.AiProviderException;
import com.clawdroid.core.model.AiMessage;
import com.clawdroid.core.model.AiRequest;
import com.clawdroid.core.model.AiResponse;
import com.clawdroid.core.model.ModelInfo;
import com.clawdroid.core.model.ProviderType;
import com.google.mlkit.genai.common.FeatureStatus;
import com.google.mlkit.genai.common.StreamingCallback;
import com.google.mlkit.genai.prompt.GenerateContentRequest;
import com.google.mlkit.genai.prompt.GenerateContentResponse;
import com.google.mlkit.genai.prompt.Generation;
import com.google.mlkit.genai.prompt.GenerativeModel;
import com.google.mlkit.genai.prompt.TextPart;
import com.google.mlkit.genai.prompt.java.GenerativeModelFutures;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public class GeminiNanoProvider implements AiProvider {

    private final Context context;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private volatile GenerativeModelFutures modelFutures;

    @Inject
    public GeminiNanoProvider(@ApplicationContext Context context) {
        this.context = context;
    }

    @Override
    public String getId() { return "gemini-nano"; }

    @Override
    public String getName() { return "Gemini Nano"; }

    @Override
    public ProviderType getType() { return ProviderType.NANO; }

    @Override
    public Single<Boolean> isAvailable() {
        return Single.create(emitter -> {
            try {
                GenerativeModelFutures model = getOrCreateModel();
                Futures.addCallback(
                        model.checkStatus(),
                        new FutureCallback<Integer>() {
                            @Override
                            public void onSuccess(Integer status) {
                                if (emitter.isDisposed()) return;
                                emitter.onSuccess(status == FeatureStatus.AVAILABLE);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                if (emitter.isDisposed()) return;
                                emitter.onSuccess(false);
                            }
                        },
                        executor
                );
            } catch (Exception e) {
                if (!emitter.isDisposed()) {
                    emitter.onSuccess(false);
                }
            }
        });
    }

    @Override
    public Single<AiResponse> generate(AiRequest request) {
        return Single.create(emitter -> {
            try {
                GenerativeModelFutures model = getOrCreateModel();
                GenerateContentRequest contentRequest = buildRequest(request);

                Futures.addCallback(
                        model.generateContent(contentRequest),
                        new FutureCallback<GenerateContentResponse>() {
                            @Override
                            public void onSuccess(GenerateContentResponse result) {
                                if (emitter.isDisposed()) return;
                                String text = "";
                                if (result.getCandidates() != null
                                        && !result.getCandidates().isEmpty()) {
                                    text = result.getCandidates().get(0).getText();
                                }
                                emitter.onSuccess(new AiResponse(
                                        text != null ? text : "",
                                        "gemini-nano", "gemini-nano",
                                        null, null, null, null, "stop"));
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                if (emitter.isDisposed()) return;
                                emitter.onError(new AiProviderException(
                                        "Gemini Nano generation failed: " + t.getMessage(), t));
                            }
                        },
                        executor
                );
            } catch (Exception e) {
                if (!emitter.isDisposed()) {
                    emitter.onError(new AiProviderException(
                            "Gemini Nano not available: " + e.getMessage(), e));
                }
            }
        });
    }

    @Override
    public Observable<String> generateStream(AiRequest request) {
        return Observable.create(emitter -> {
            try {
                GenerativeModelFutures model = getOrCreateModel();
                GenerateContentRequest contentRequest = buildRequest(request);

                StreamingCallback streamingCallback = text -> {
                    if (emitter.isDisposed()) return;
                    if (text != null && !text.isEmpty()) {
                        emitter.onNext(text);
                    }
                };

                Futures.addCallback(
                        model.generateContent(contentRequest, streamingCallback),
                        new FutureCallback<GenerateContentResponse>() {
                            @Override
                            public void onSuccess(GenerateContentResponse result) {
                                if (!emitter.isDisposed()) {
                                    emitter.onComplete();
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                if (!emitter.isDisposed()) {
                                    emitter.onError(new AiProviderException(
                                            "Gemini Nano stream failed: " + t.getMessage(), t));
                                }
                            }
                        },
                        executor
                );
            } catch (Exception e) {
                if (!emitter.isDisposed()) {
                    emitter.onError(new AiProviderException(
                            "Gemini Nano streaming not available: " + e.getMessage(), e));
                }
            }
        });
    }

    @Override
    public Single<List<ModelInfo>> listModels() {
        return Single.just(Collections.singletonList(
                new ModelInfo("gemini-nano", "Gemini Nano",
                        "On-device AI model. Fast, private, and efficient.")
        ));
    }

    private synchronized GenerativeModelFutures getOrCreateModel() {
        if (modelFutures == null) {
            GenerativeModel generativeModel = Generation.INSTANCE.getClient();
            modelFutures = GenerativeModelFutures.from(generativeModel);
        }
        return modelFutures;
    }

    private GenerateContentRequest buildRequest(AiRequest request) {
        StringBuilder prompt = new StringBuilder();
        for (AiMessage msg : request.getMessages()) {
            String role = msg.getRole();
            if ("system".equals(role)) {
                prompt.append("[System] ").append(msg.getContent()).append("\n\n");
            } else if ("user".equals(role)) {
                prompt.append(msg.getContent()).append("\n");
            } else if ("assistant".equals(role)) {
                prompt.append("[Assistant] ").append(msg.getContent()).append("\n\n");
            }
        }

        GenerateContentRequest.Builder builder =
                new GenerateContentRequest.Builder(new TextPart(prompt.toString().trim()));

        if (request.getConfig() != null) {
            builder.setTemperature(request.getConfig().getTemperature());
            builder.setTopK(request.getConfig().getTopK());
            builder.setMaxOutputTokens(request.getConfig().getMaxOutputTokens());
        }

        return builder.build();
    }
}
