package com.clawdroid.feature.chat.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clawdroid.core.ai.AiProviderManager;
import com.clawdroid.core.ai.PromptBuilder;
import com.clawdroid.core.data.db.entity.ConversationEntity;
import com.clawdroid.core.data.db.entity.MessageEntity;
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
public class ChatViewModel extends ViewModel {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SettingsRepository settingsRepository;
    private final AiProviderManager providerManager;
    private final PromptBuilder promptBuilder;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<ConversationEntity> conversation = new MutableLiveData<>();
    private final MutableLiveData<List<MessageEntity>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isGenerating = new MutableLiveData<>(false);
    private final MutableLiveData<String> streamingText = new MutableLiveData<>("");
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private String conversationId;

    @Inject
    public ChatViewModel(ConversationRepository conversationRepository,
                         MessageRepository messageRepository,
                         SettingsRepository settingsRepository,
                         AiProviderManager providerManager,
                         PromptBuilder promptBuilder) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.settingsRepository = settingsRepository;
        this.providerManager = providerManager;
        this.promptBuilder = promptBuilder;
    }

    public LiveData<ConversationEntity> getConversation() { return conversation; }
    public LiveData<List<MessageEntity>> getMessages() { return messages; }
    public LiveData<Boolean> getIsGenerating() { return isGenerating; }
    public LiveData<String> getStreamingText() { return streamingText; }
    public LiveData<String> getError() { return error; }

    public void loadConversation(String convId) {
        this.conversationId = convId;

        disposables.add(
            conversationRepository.getById(convId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    conversation::setValue,
                    e -> error.setValue(e.getMessage())
                )
        );

        disposables.add(
            messageRepository.getMessages(convId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    messages::setValue,
                    e -> error.setValue(e.getMessage())
                )
        );
    }

    public void sendMessage(String content) {
        if (content == null || content.trim().isEmpty() || conversationId == null) return;

        isGenerating.setValue(true);
        streamingText.setValue("");

        disposables.add(
            messageRepository.saveUserMessage(conversationId, content.trim())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    userMsg -> {
                        conversationRepository.updateLastMessage(conversationId, content.trim())
                                .subscribeOn(Schedulers.io())
                                .subscribe();
                        generateAiResponse(content.trim());
                    },
                    e -> {
                        error.setValue(e.getMessage());
                        isGenerating.setValue(false);
                    }
                )
        );
    }

    private void generateAiResponse(String userMessage) {
        ConversationEntity conv = conversation.getValue();
        String systemPrompt = conv != null && conv.getSystemPrompt() != null
                ? conv.getSystemPrompt()
                : settingsRepository.getSystemPrompt();

        List<AiMessage> history = buildHistory();
        List<AiMessage> prompt = promptBuilder.build(systemPrompt, history, userMessage, null);

        AiRequest request = new AiRequest(
                prompt,
                conv != null ? conv.getModelId() : null,
                new AiConfig(),
                null
        );

        StringBuilder responseBuilder = new StringBuilder();
        long startTime = System.currentTimeMillis();

        disposables.add(
            providerManager.generateStream(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    token -> {
                        responseBuilder.append(token);
                        streamingText.setValue(responseBuilder.toString());
                    },
                    e -> {
                        error.setValue(e.getMessage());
                        isGenerating.setValue(false);
                    },
                    () -> {
                        long durationMs = System.currentTimeMillis() - startTime;
                        String response = responseBuilder.toString();

                        disposables.add(
                            messageRepository.saveAssistantMessage(
                                    conversationId, response,
                                    conv != null ? conv.getModelProvider() : null,
                                    conv != null ? conv.getModelId() : null,
                                    null, null, (int) durationMs)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(
                                    msg -> {
                                        isGenerating.setValue(false);
                                        streamingText.setValue("");
                                    },
                                    err -> {
                                        isGenerating.setValue(false);
                                        error.setValue(err.getMessage());
                                    }
                                )
                        );
                    }
                )
        );
    }

    private List<AiMessage> buildHistory() {
        List<AiMessage> history = new ArrayList<>();
        List<MessageEntity> currentMessages = messages.getValue();
        if (currentMessages != null) {
            for (MessageEntity msg : currentMessages) {
                history.add(new AiMessage(msg.getRole(), msg.getContent()));
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
