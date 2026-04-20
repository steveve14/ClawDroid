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

    // Ollama settings
    public String getOllamaEndpoint() {
        return prefs.getString("ollama_endpoint", "http://localhost:11434");
    }

    public void setOllamaEndpoint(String endpoint) {
        prefs.edit().putString("ollama_endpoint", endpoint).apply();
    }

    // Model parameter settings
    public float getTemperature() {
        return prefs.getFloat("model_temperature", 0.7f);
    }

    public void setTemperature(float temperature) {
        prefs.edit().putFloat("model_temperature", temperature).apply();
    }

    public float getTopP() {
        return prefs.getFloat("model_top_p", 0.95f);
    }

    public void setTopP(float topP) {
        prefs.edit().putFloat("model_top_p", topP).apply();
    }

    public int getMaxTokens() {
        return prefs.getInt("model_max_tokens", 4096);
    }

    public void setMaxTokens(int maxTokens) {
        prefs.edit().putInt("model_max_tokens", maxTokens).apply();
    }

    // TTS settings
    public String getTtsProvider() {
        return prefs.getString("tts_provider", "android");
    }

    public void setTtsProvider(String provider) {
        prefs.edit().putString("tts_provider", provider).apply();
    }

    public String getElevenLabsApiKey() {
        return securePrefs.getString("elevenlabs_api_key", "");
    }

    public void setElevenLabsApiKey(String apiKey) {
        securePrefs.edit().putString("elevenlabs_api_key", apiKey).apply();
    }

    public String getElevenLabsVoiceId() {
        return prefs.getString("elevenlabs_voice_id", "21m00Tcm4TlvDq8ikWAM");
    }

    public void setElevenLabsVoiceId(String voiceId) {
        prefs.edit().putString("elevenlabs_voice_id", voiceId).apply();
    }

    // Wake Word settings
    public boolean isWakeWordEnabled() {
        return prefs.getBoolean("wake_word_enabled", false);
    }

    public void setWakeWordEnabled(boolean enabled) {
        prefs.edit().putBoolean("wake_word_enabled", enabled).apply();
    }

    // Active provider shortcut
    public String getActiveProvider() {
        return getDefaultModelProvider();
    }

    public void setActiveProvider(String providerId) {
        setDefaultModelProvider(providerId);
    }

    // Fallback settings
    public boolean isFallbackEnabled() {
        return prefs.getBoolean("fallback_enabled", true);
    }

    public void setFallbackEnabled(boolean enabled) {
        prefs.edit().putBoolean("fallback_enabled", enabled).apply();
    }
}
