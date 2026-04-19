package com.clawdroid.app.di;

import com.clawdroid.core.ai.AiProvider;
import com.clawdroid.core.ai.provider.GeminiCloudProvider;
import com.clawdroid.core.ai.provider.GeminiNanoProvider;
import com.clawdroid.core.ai.provider.OllamaProvider;
import com.clawdroid.core.ai.provider.OpenAiProvider;

import java.util.HashMap;
import java.util.Map;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class AiModule {

    @Provides
    @Singleton
    public Map<String, AiProvider> provideAiProviders(
            GeminiNanoProvider nano,
            GeminiCloudProvider geminiCloud,
            OpenAiProvider openAi,
            OllamaProvider ollama) {
        Map<String, AiProvider> map = new HashMap<>();
        map.put(nano.getId(), nano);
        map.put(geminiCloud.getId(), geminiCloud);
        map.put(openAi.getId(), openAi);
        map.put(ollama.getId(), ollama);
        return map;
    }
}
