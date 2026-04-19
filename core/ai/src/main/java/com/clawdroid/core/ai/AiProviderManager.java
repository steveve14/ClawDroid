package com.clawdroid.core.ai;

import com.clawdroid.core.data.repository.SettingsRepository;
import com.clawdroid.core.data.db.entity.AiProviderEntity;
import com.clawdroid.core.model.AiRequest;
import com.clawdroid.core.model.AiResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

@Singleton
public class AiProviderManager {

    private final Map<String, AiProvider> providers;
    private final SettingsRepository settingsRepository;

    @Inject
    public AiProviderManager(Map<String, AiProvider> providers,
                             SettingsRepository settingsRepository) {
        this.providers = providers;
        this.settingsRepository = settingsRepository;
    }

    public Observable<String> generateStream(AiRequest request) {
        return buildProviderChain()
                .flatMapObservable(chain -> tryProviders(chain, request));
    }

    public Single<AiResponse> generate(AiRequest request) {
        return buildProviderChain()
                .flatMap(chain -> tryProvidersSync(chain, request));
    }

    public Single<AiProvider> getActiveProvider() {
        return buildProviderChain()
                .flatMap(chain -> {
                    if (chain.isEmpty()) {
                        return Single.error(
                                new AiProviderException("사용 가능한 AI 프로바이더가 없습니다."));
                    }
                    return Single.just(chain.get(0));
                });
    }

    private Single<List<AiProvider>> buildProviderChain() {
        return settingsRepository.getEnabledProviders()
                .map(enabledProviders -> {
                    enabledProviders.sort(Comparator.comparingInt(AiProviderEntity::getPriority));
                    List<AiProvider> chain = new ArrayList<>();
                    for (AiProviderEntity p : enabledProviders) {
                        AiProvider provider = providers.get(p.getId());
                        if (provider != null) chain.add(provider);
                    }
                    return chain;
                });
    }

    private Observable<String> tryProviders(List<AiProvider> chain, AiRequest request) {
        if (chain.isEmpty()) {
            return Observable.error(
                    new AiProviderException("사용 가능한 AI 프로바이더가 없습니다."));
        }
        AiProvider first = chain.get(0);
        List<AiProvider> rest = chain.subList(1, chain.size());
        return first.isAvailable()
                .flatMapObservable(available -> {
                    if (available) {
                        return first.generateStream(request)
                                .onErrorResumeNext(e -> {
                                    if (!rest.isEmpty()) {
                                        return tryProviders(rest, request);
                                    }
                                    return Observable.error(e);
                                });
                    } else {
                        return tryProviders(rest, request);
                    }
                });
    }

    private Single<AiResponse> tryProvidersSync(List<AiProvider> chain, AiRequest request) {
        if (chain.isEmpty()) {
            return Single.error(
                    new AiProviderException("사용 가능한 AI 프로바이더가 없습니다."));
        }
        AiProvider first = chain.get(0);
        List<AiProvider> rest = chain.subList(1, chain.size());
        return first.isAvailable()
                .flatMap(available -> {
                    if (available) {
                        return first.generate(request)
                                .onErrorResumeNext(e -> {
                                    if (!rest.isEmpty()) {
                                        return tryProvidersSync(rest, request);
                                    }
                                    return Single.error(e);
                                });
                    } else {
                        return tryProvidersSync(rest, request);
                    }
                });
    }
}
