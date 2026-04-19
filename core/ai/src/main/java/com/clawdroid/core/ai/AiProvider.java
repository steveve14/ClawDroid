package com.clawdroid.core.ai;

import com.clawdroid.core.model.AiRequest;
import com.clawdroid.core.model.AiResponse;
import com.clawdroid.core.model.ModelInfo;
import com.clawdroid.core.model.ProviderType;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public interface AiProvider {

    String getId();

    String getName();

    ProviderType getType();

    Single<Boolean> isAvailable();

    Single<AiResponse> generate(AiRequest request);

    Observable<String> generateStream(AiRequest request);

    Single<List<ModelInfo>> listModels();
}
