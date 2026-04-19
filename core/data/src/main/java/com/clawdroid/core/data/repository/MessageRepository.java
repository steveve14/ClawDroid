package com.clawdroid.core.data.repository;

import com.clawdroid.core.data.db.dao.MessageDao;
import com.clawdroid.core.data.db.entity.MessageEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Singleton
public class MessageRepository {

    private final MessageDao messageDao;

    @Inject
    public MessageRepository(MessageDao messageDao) {
        this.messageDao = messageDao;
    }

    public Flowable<List<MessageEntity>> getMessages(String conversationId) {
        return messageDao.getMessages(conversationId);
    }

    public Single<MessageEntity> saveUserMessage(String conversationId, String content) {
        MessageEntity entity = new MessageEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setConversationId(conversationId);
        entity.setRole("user");
        entity.setContent(content);
        entity.setCreatedAt(Instant.now().toString());

        return messageDao.insert(entity).andThen(Single.just(entity));
    }

    public Single<MessageEntity> saveAssistantMessage(String conversationId, String content,
                                                       String modelProvider, String modelId,
                                                       Integer inputTokens, Integer outputTokens,
                                                       Integer durationMs) {
        MessageEntity entity = new MessageEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setConversationId(conversationId);
        entity.setRole("assistant");
        entity.setContent(content);
        entity.setModelProvider(modelProvider);
        entity.setModelId(modelId);
        entity.setInputTokens(inputTokens);
        entity.setOutputTokens(outputTokens);
        entity.setDurationMs(durationMs);
        entity.setCreatedAt(Instant.now().toString());

        return messageDao.insert(entity).andThen(Single.just(entity));
    }

    public Completable updateMessage(MessageEntity message) {
        return messageDao.update(message);
    }

    public Completable deleteByConversation(String conversationId) {
        return messageDao.deleteByConversation(conversationId);
    }

    public Flowable<List<MessageEntity>> search(String conversationId, String query) {
        return messageDao.search(conversationId, query);
    }
}
