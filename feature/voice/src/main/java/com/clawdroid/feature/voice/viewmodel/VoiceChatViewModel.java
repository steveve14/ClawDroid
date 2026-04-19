package com.clawdroid.feature.voice.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clawdroid.core.ai.AiProviderManager;
import com.clawdroid.core.ai.PromptBuilder;
import com.clawdroid.core.data.repository.SettingsRepository;
import com.clawdroid.core.model.AiConfig;
import com.clawdroid.core.model.AiMessage;
import com.clawdroid.core.model.AiRequest;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class VoiceChatViewModel extends ViewModel {

    public static class VoiceLogEntry {
        public final String role;
        public final String text;
        public final long timestamp;

        public VoiceLogEntry(String role, String text) {
            this.role = role;
            this.text = text;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private final AiProviderManager providerManager;
    private final PromptBuilder promptBuilder;
    private final SettingsRepository settingsRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<List<VoiceLogEntry>> voiceLog = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isProcessing = new MutableLiveData<>(false);
    private final MutableLiveData<String> aiResponse = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    @Inject
    public VoiceChatViewModel(AiProviderManager providerManager,
                              PromptBuilder promptBuilder,
                              SettingsRepository settingsRepository) {
        this.providerManager = providerManager;
        this.promptBuilder = promptBuilder;
        this.settingsRepository = settingsRepository;
    }

    public LiveData<List<VoiceLogEntry>> getVoiceLog() { return voiceLog; }
    public LiveData<Boolean> getIsProcessing() { return isProcessing; }
    public LiveData<String> getAiResponse() { return aiResponse; }
    public LiveData<String> getError() { return error; }

    public void processUserSpeech(String text) {
        if (text == null || text.trim().isEmpty()) return;

        addLogEntry("user", text);
        isProcessing.setValue(true);

        String systemPrompt = settingsRepository.getSystemPrompt();
        List<AiMessage> history = buildHistory();
        List<AiMessage> prompt = promptBuilder.build(systemPrompt, history, text, null);

        AiConfig config = new AiConfig(
                settingsRepository.getTemperature(),
                settingsRepository.getTopP(),
                40,
                settingsRepository.getMaxTokens());

        AiRequest request = new AiRequest(prompt, null, config, null);

        StringBuilder responseBuilder = new StringBuilder();
        disposables.add(
            providerManager.generateStream(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    responseBuilder::append,
                    e -> {
                        error.setValue(e.getMessage());
                        isProcessing.setValue(false);
                    },
                    () -> {
                        String response = responseBuilder.toString();
                        addLogEntry("assistant", response);
                        aiResponse.setValue(response);
                        isProcessing.setValue(false);
                    }
                )
        );
    }

    private void addLogEntry(String role, String text) {
        List<VoiceLogEntry> current = voiceLog.getValue();
        if (current == null) current = new ArrayList<>();
        List<VoiceLogEntry> updated = new ArrayList<>(current);
        updated.add(new VoiceLogEntry(role, text));
        voiceLog.setValue(updated);
    }

    private List<AiMessage> buildHistory() {
        List<AiMessage> history = new ArrayList<>();
        List<VoiceLogEntry> log = voiceLog.getValue();
        if (log != null) {
            for (VoiceLogEntry entry : log) {
                history.add(new AiMessage(entry.role, entry.text));
            }
        }
        return history;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
