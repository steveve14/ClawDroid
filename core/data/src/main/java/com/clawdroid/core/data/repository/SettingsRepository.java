package com.clawdroid.core.data.repository;

import android.content.SharedPreferences;

import com.clawdroid.core.data.db.dao.AiProviderDao;
import com.clawdroid.core.data.db.entity.AiProviderEntity;
import com.clawdroid.core.data.di.EncryptedPrefs;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Singleton
public class SettingsRepository {

    private final SharedPreferences prefs;
    private final SharedPreferences securePrefs;
    private final AiProviderDao aiProviderDao;

    @Inject
    public SettingsRepository(
            SharedPreferences prefs,
            @EncryptedPrefs SharedPreferences securePrefs,
            AiProviderDao aiProviderDao) {
        this.prefs = prefs;
        this.securePrefs = securePrefs;
        this.aiProviderDao = aiProviderDao;
    }

    // AI Provider settings
    public Single<List<AiProviderEntity>> getEnabledProviders() {
        return aiProviderDao.getEnabled();
    }

    public Flowable<List<AiProviderEntity>> getAllProviders() {
        return aiProviderDao.getAll();
    }

    public Completable saveProvider(AiProviderEntity provider) {
        return aiProviderDao.insert(provider);
    }

    public Completable setProviderEnabled(String id, boolean enabled) {
        return aiProviderDao.setEnabled(id, enabled ? 1 : 0);
    }

    // API Key management (encrypted)
    public void saveApiKey(String providerId, String apiKey) {
        securePrefs.edit().putString(providerId + "_api_key", apiKey).apply();
    }

    public String getApiKey(String providerId) {
        return securePrefs.getString(providerId + "_api_key", "");
    }

    // General settings
    public String getDefaultModelProvider() {
        return prefs.getString("default_model_provider", "gemini-nano");
    }

    public void setDefaultModelProvider(String providerId) {
        prefs.edit().putString("default_model_provider", providerId).apply();
    }

    public String getPersonaName() {
        return prefs.getString("persona_name", "ClawDroid");
    }

    public void setPersonaName(String name) {
        prefs.edit().putString("persona_name", name).apply();
    }

    public String getSystemPrompt() {
        return prefs.getString("system_prompt",
                "You are a helpful and intelligent assistant. Your goal is to provide accurate and concise information.");
    }

    public void setSystemPrompt(String prompt) {
        prefs.edit().putString("system_prompt", prompt).apply();
    }

    public String getConversationStyle() {
        return prefs.getString("conversation_style", "Professional");
    }

    public void setConversationStyle(String style) {
        prefs.edit().putString("conversation_style", style).apply();
    }
}
