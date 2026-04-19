package com.clawdroid.core.ai.provider;

import android.content.Context;

import com.clawdroid.core.ai.AiProvider;
import com.clawdroid.core.model.AiRequest;
import com.clawdroid.core.model.AiResponse;
import com.clawdroid.core.model.ModelInfo;
import com.clawdroid.core.model.ProviderType;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public class GeminiNanoProvider implements AiProvider {

    private final Context context;

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
        // TODO: Check AICore availability via ML Kit GenAI
        // GenerativeModel.isAvailable() or similar API
        return Single.just(false);
    }

    @Override
    public Single<AiResponse> generate(AiRequest request) {
        // TODO: Implement via ML Kit GenAI Prompt API
        return Single.error(new UnsupportedOperationException(
                "Gemini Nano not yet implemented. Requires physical device with AICore."));
    }

    @Override
    public Observable<String> generateStream(AiRequest request) {
        // TODO: Implement streaming via ML Kit GenAI
        return Observable.error(new UnsupportedOperationException(
                "Gemini Nano streaming not yet implemented."));
    }

    @Override
    public Single<List<ModelInfo>> listModels() {
        return Single.just(Collections.singletonList(
                new ModelInfo("gemini-nano", "Gemini Nano",
                        "On-device AI model. Fast, private, and efficient.")
        ));
    }
}
