package com.clawdroid.core.data.repository;

import com.clawdroid.core.data.db.dao.ConversationDao;
import com.clawdroid.core.data.db.entity.ConversationEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Singleton
public class ConversationRepository {

    private final ConversationDao conversationDao;

    @Inject
    public ConversationRepository(ConversationDao conversationDao) {
        this.conversationDao = conversationDao;
    }

    public Flowable<List<ConversationEntity>> getActiveConversations() {
        return conversationDao.getActiveConversations();
    }

    public Flowable<List<ConversationEntity>> getArchivedConversations() {
        return conversationDao.getArchivedConversations();
    }

    public Single<ConversationEntity> getById(String id) {
        return conversationDao.getById(id);
    }

    public Single<ConversationEntity> createConversation(String title, String modelProvider,
                                                          String modelId, String systemPrompt) {
        ConversationEntity entity = new ConversationEntity();
        String now = Instant.now().toString();
        entity.setId(UUID.randomUUID().toString());
        entity.setTitle(title);
        entity.setModelProvider(modelProvider);
        entity.setModelId(modelId);
        entity.setSystemPrompt(systemPrompt);
        entity.setMessageCount(0);
        entity.setTokenCount(0);
        entity.setIsArchived(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        return conversationDao.insert(entity).andThen(Single.just(entity));
    }

    public Completable updateConversation(ConversationEntity conversation) {
        return conversationDao.update(conversation);
    }

    public Completable deleteConversation(String id) {
        return conversationDao.deleteById(id);
    }

    public Completable archiveConversation(String id) {
        return conversationDao.archive(id);
    }

    public Completable updateLastMessage(String conversationId, String preview) {
        String now = Instant.now().toString();
        return conversationDao.updateLastMessage(conversationId, preview, now);
    }
}
