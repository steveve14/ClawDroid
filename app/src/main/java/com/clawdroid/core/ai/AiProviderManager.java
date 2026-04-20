package com.clawdroid.core.ai;

import com.clawdroid.core.data.repository.SettingsRepository;
import com.clawdroid.core.data.db.entity.AiProviderEntity;
import com.clawdroid.core.model.AiRequest;
import com.clawdroid.core.model.AiResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

@Singleton
public class AiProviderManager {

    private static final int MAX_RETRY_COUNT = 10;

    private final Map<String, AiProvider> providers;
    private final SettingsRepository settingsRepository;

    @Inject
    public AiProviderManager(Map<String, AiProvider> providers,
                             SettingsRepository settingsRepository) {
        this.providers = providers;
        this.settingsRepository = settingsRepository;
    }

    public Single<Boolean> isAnyProviderAvailable() {
        return buildProviderChain()
                .map(chain -> !chain.isEmpty());
    }

    public Observable<String> generateStream(AiRequest request) {
        return buildProviderChain()
                .flatMapObservable(chain -> {
                    if (chain.isEmpty()) {
                        return Observable.error(
                                new AiProviderException("AI 프로바이더가 연결되지 않았습니다. API 키를 설정해주세요."));
                    }
                    return tryProviders(chain, request);
                })
                .retryWhen(errors -> errors.zipWith(
                        Observable.range(1, MAX_RETRY_COUNT),
                        (error, retryCount) -> {
                            if (retryCount >= MAX_RETRY_COUNT) {
                                throw new RuntimeException(
                                        new AiProviderException("최대 재시도 횟수(" + MAX_RETRY_COUNT + "회)를 초과했습니다."));
                            }
                            if (error instanceof AiProviderException &&
                                    error.getMessage() != null &&
                                    error.getMessage().contains("연결되지 않았습니다")) {
                                throw new RuntimeException(error);
                            }
                            return retryCount;
                        })
                        .flatMap(retryCount ->
                                Observable.timer(Math.min(retryCount * 2L, 10), TimeUnit.SECONDS))
                );
    }

    public Single<AiResponse> generate(AiRequest request) {
        return buildProviderChain()
                .flatMap(chain -> {
                    if (chain.isEmpty()) {
                        return Single.error(
                                new AiProviderException("AI 프로바이더가 연결되지 않았습니다. API 키를 설정해주세요."));
                    }
                    return tryProvidersSync(chain, request);
                })
                .retryWhen(errors -> errors.zipWith(
                        io.reactivex.rxjava3.core.Flowable.range(1, MAX_RETRY_COUNT),
                        (error, retryCount) -> {
                            if (retryCount >= MAX_RETRY_COUNT) {
                                throw new RuntimeException(
                                        new AiProviderException("최대 재시도 횟수(" + MAX_RETRY_COUNT + "회)를 초과했습니다."));
                            }
                            if (error instanceof AiProviderException &&
                                    error.getMessage() != null &&
                                    error.getMessage().contains("연결되지 않았습니다")) {
                                throw new RuntimeException(error);
                            }
                            return retryCount;
                        })
                        .flatMap(retryCount ->
                                io.reactivex.rxjava3.core.Flowable.timer(Math.min(retryCount * 2L, 10), TimeUnit.SECONDS))
                );
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

    public AiProvider getProvider(String id) {
        return providers.get(id);
    }

    public List<String> getAllProviderIds() {
        return new ArrayList<>(providers.keySet());
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
