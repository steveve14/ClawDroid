package com.clawdroid.feature.voice.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clawdroid.core.ai.AiProviderManager;
import com.clawdroid.core.ai.PromptBuilder;
import com.clawdroid.core.data.db.entity.ConversationEntity;
import com.clawdroid.core.data.repository.ConversationRepository;
import com.clawdroid.core.data.repository.MessageRepository;
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
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<List<VoiceLogEntry>> voiceLog = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isProcessing = new MutableLiveData<>(false);
    private final MutableLiveData<String> aiResponse = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<List<ConversationEntity>> conversations = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ConversationEntity> selectedConversation = new MutableLiveData<>(null);

    @Inject
    public VoiceChatViewModel(AiProviderManager providerManager,
                              PromptBuilder promptBuilder,
                              SettingsRepository settingsRepository,
                              ConversationRepository conversationRepository,
                              MessageRepository messageRepository) {
        this.providerManager = providerManager;
        this.promptBuilder = promptBuilder;
        this.settingsRepository = settingsRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        loadConversations();
    }

    public LiveData<List<VoiceLogEntry>> getVoiceLog() { return voiceLog; }
    public LiveData<Boolean> getIsProcessing() { return isProcessing; }
    public LiveData<String> getAiResponse() { return aiResponse; }
    public LiveData<String> getError() { return error; }
    public LiveData<List<ConversationEntity>> getConversations() { return conversations; }
    public LiveData<ConversationEntity> getSelectedConversation() { return selectedConversation; }

    private void loadConversations() {
        disposables.add(
            conversationRepository.getActiveConversations()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    list -> {
                        conversations.setValue(list);
                        // ?좏깮????붾갑???놁쑝硫?媛??理쒓렐 ??붾갑 ?먮룞 ?ㅼ젙
                        if (selectedConversation.getValue() == null && !list.isEmpty()) {
                            selectedConversation.setValue(list.get(0));
                        }
                    },
                    e -> error.setValue(e.getMessage())
                )
        );
    }

    public void selectConversation(ConversationEntity conversation) {
        selectedConversation.setValue(conversation);
    }

    public void processUserSpeech(String text) {
        if (text == null || text.trim().isEmpty()) return;

        addLogEntry("user", text);
        isProcessing.setValue(true);

        // ?좏깮????붾갑???ъ슜??硫붿떆吏 ???
        ConversationEntity conv = selectedConversation.getValue();
        if (conv != null) {
            disposables.add(
                messageRepository.saveUserMessage(conv.getId(), text)
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                        msg -> conversationRepository.updateLastMessage(conv.getId(), text)
                                .subscribeOn(Schedulers.io()).subscribe(),
                        e -> { /* ????ㅽ뙣?대룄 ?뚯꽦 泥섎━??怨꾩냽 */ }
                    )
            );
        }

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

                        // ?좏깮????붾갑??AI ?묐떟 ???
                        if (conv != null) {
                            disposables.add(
                                messageRepository.saveAssistantMessage(
                                        conv.getId(), response, null, null, null, null, null)
                                    .subscribeOn(Schedulers.io())
                                    .subscribe(
                                        msg -> conversationRepository.updateLastMessage(conv.getId(), response)
                                                .subscribeOn(Schedulers.io()).subscribe(),
                                        e -> { /* ????ㅽ뙣?대룄 臾댁떆 */ }
                                    )
                            );
                        }
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
