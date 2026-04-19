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
import java.util.Collections;
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
    private String thinkLevel = "off"; // "off", "low", "medium", "high"

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

    public void sendMessageWithImage(String content, byte[] imageData) {
        if (conversationId == null) return;

        isGenerating.setValue(true);
        streamingText.setValue("");

        String msgText = content != null && !content.trim().isEmpty()
                ? content.trim() : "이 이미지를 설명해주세요.";

        disposables.add(
            messageRepository.saveUserMessage(conversationId, "[이미지] " + msgText)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    userMsg -> {
                        conversationRepository.updateLastMessage(conversationId, "[이미지] " + msgText)
                                .subscribeOn(Schedulers.io())
                                .subscribe();
                        generateAiResponseWithImage(msgText, imageData);
                    },
                    e -> {
                        error.setValue(e.getMessage());
                        isGenerating.setValue(false);
                    }
                )
        );
    }

    public void switchModel(String providerId, String modelId) {
        ConversationEntity conv = conversation.getValue();
        if (conv != null) {
            conv.setModelProvider(providerId);
            conv.setModelId(modelId);
            disposables.add(
                conversationRepository.updateConversation(conv)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        () -> conversation.setValue(conv),
                        e -> error.setValue(e.getMessage())
                    )
            );
        }
    }

    public void compactConversation() {
        List<MessageEntity> currentMessages = messages.getValue();
        if (currentMessages == null || currentMessages.size() < 6) {
            error.setValue("압축할 메시지가 충분하지 않습니다.");
            return;
        }

        // Build a compact summary request
        StringBuilder summaryPrompt = new StringBuilder();
        summaryPrompt.append("다음 대화를 짧게 요약해주세요. 핵심 정보만 유지:\n\n");
        for (MessageEntity msg : currentMessages) {
            summaryPrompt.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }

        isGenerating.setValue(true);
        streamingText.setValue("");

        List<AiMessage> prompt = Collections.singletonList(
                new AiMessage("user", summaryPrompt.toString()));
        AiRequest request = new AiRequest(prompt, null, new AiConfig(), null);

        StringBuilder response = new StringBuilder();
        disposables.add(
            providerManager.generateStream(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    token -> {
                        response.append(token);
                        streamingText.setValue(response.toString());
                    },
                    e -> {
                        error.setValue(e.getMessage());
                        isGenerating.setValue(false);
                    },
                    () -> {
                        // Delete old messages and save summary
                        disposables.add(
                            messageRepository.deleteByConversation(conversationId)
                                .andThen(messageRepository.saveAssistantMessage(
                                        conversationId, "[컨텍스트 요약]\n" + response,
                                        null, null, null, null, null))
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

    public void resetConversation() {
        if (conversationId == null) return;
        disposables.add(
            messageRepository.deleteByConversation(conversationId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> {},
                    e -> error.setValue(e.getMessage())
                )
        );
    }

    public void showStatus() {
        ConversationEntity conv = conversation.getValue();
        List<MessageEntity> msgs = messages.getValue();
        int msgCount = msgs != null ? msgs.size() : 0;

        StringBuilder status = new StringBuilder();
        status.append("📊 **현재 대화 상태**\n");
        status.append("- 대화 ID: `").append(conversationId != null ? conversationId.substring(0, 8) : "없음").append("`\n");
        status.append("- 메시지 수: ").append(msgCount).append("\n");
        status.append("- 모델: ").append(conv != null && conv.getModelId() != null ? conv.getModelId() : "기본").append("\n");
        status.append("- 프로바이더: ").append(conv != null && conv.getModelProvider() != null ? conv.getModelProvider() : "기본").append("\n");
        status.append("- Think 레벨: ").append(thinkLevel).append("\n");
        status.append("- Temperature: ").append(settingsRepository.getTemperature()).append("\n");
        status.append("- Max Tokens: ").append(settingsRepository.getMaxTokens()).append("\n");

        // Add as a system-style message by saving as assistant message
        disposables.add(
            messageRepository.saveAssistantMessage(conversationId, status.toString(),
                    null, null, null, null, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    msg -> {},
                    e -> error.setValue(e.getMessage())
                )
        );
    }

    public void setThinkLevel(String level) {
        if (level.isEmpty()) {
            // Cycle: off -> low -> medium -> high -> off
            switch (thinkLevel) {
                case "off": thinkLevel = "low"; break;
                case "low": thinkLevel = "medium"; break;
                case "medium": thinkLevel = "high"; break;
                default: thinkLevel = "off"; break;
            }
        } else if (level.equals("off") || level.equals("low")
                || level.equals("medium") || level.equals("high")) {
            thinkLevel = level;
        } else {
            error.setValue("유효한 think 레벨: off, low, medium, high");
            return;
        }

        String msg = "🧠 Think 레벨이 **" + thinkLevel + "**로 설정되었습니다.";
        disposables.add(
            messageRepository.saveAssistantMessage(conversationId, msg,
                    null, null, null, null, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    m -> {},
                    e -> error.setValue(e.getMessage())
                )
        );
    }

    private void generateAiResponse(String userMessage) {
        ConversationEntity conv = conversation.getValue();
        String systemPrompt = conv != null && conv.getSystemPrompt() != null
                ? conv.getSystemPrompt()
                : settingsRepository.getSystemPrompt();

        // Apply think level to system prompt
        if (!"off".equals(thinkLevel)) {
            String thinkInstruction;
            switch (thinkLevel) {
                case "high":
                    thinkInstruction = "\n\n[Thinking Mode: HIGH] Think step by step in detail. " +
                            "Show your reasoning process before giving the final answer.";
                    break;
                case "medium":
                    thinkInstruction = "\n\n[Thinking Mode: MEDIUM] Think through the problem " +
                            "briefly before answering.";
                    break;
                default:
                    thinkInstruction = "\n\n[Thinking Mode: LOW] Consider the question carefully.";
                    break;
            }
            systemPrompt += thinkInstruction;
        }

        List<AiMessage> history = buildHistory();
        List<AiMessage> prompt = promptBuilder.build(systemPrompt, history, userMessage, null);

        AiConfig config = new AiConfig(
                settingsRepository.getTemperature(),
                settingsRepository.getTopP(),
                40,
                settingsRepository.getMaxTokens());

        AiRequest request = new AiRequest(
                prompt,
                conv != null ? conv.getModelId() : null,
                config,
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

    private void generateAiResponseWithImage(String text, byte[] imageData) {
        ConversationEntity conv = conversation.getValue();
        String systemPrompt = conv != null && conv.getSystemPrompt() != null
                ? conv.getSystemPrompt()
                : settingsRepository.getSystemPrompt();

        List<AiMessage> history = buildHistory();
        AiMessage userMsg = new AiMessage("user", text, Collections.singletonList(imageData));
        history.add(userMsg);

        List<AiMessage> prompt = new ArrayList<>();
        prompt.add(new AiMessage("system", systemPrompt));
        prompt.addAll(history);

        AiConfig config = new AiConfig(
                settingsRepository.getTemperature(),
                settingsRepository.getTopP(),
                40,
                settingsRepository.getMaxTokens());

        AiRequest request = new AiRequest(
                prompt,
                conv != null ? conv.getModelId() : null,
                config,
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
