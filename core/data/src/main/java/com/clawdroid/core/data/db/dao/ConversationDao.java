package com.clawdroid.core.data.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.clawdroid.core.data.db.entity.ConversationEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface ConversationDao {

    @Query("SELECT * FROM conversations WHERE is_archived = 0 ORDER BY updated_at DESC")
    Flowable<List<ConversationEntity>> getActiveConversations();

    @Query("SELECT * FROM conversations WHERE is_archived = 1 ORDER BY updated_at DESC")
    Flowable<List<ConversationEntity>> getArchivedConversations();

    @Query("SELECT * FROM conversations WHERE id = :id")
    Single<ConversationEntity> getById(String id);

    @Query("SELECT * FROM conversations WHERE channel_id = :channelId ORDER BY updated_at DESC")
    Flowable<List<ConversationEntity>> getByChannel(String channelId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insert(ConversationEntity conversation);

    @Update
    Completable update(ConversationEntity conversation);

    @Delete
    Completable delete(ConversationEntity conversation);

    @Query("DELETE FROM conversations WHERE id = :id")
    Completable deleteById(String id);

    @Query("UPDATE conversations SET is_archived = 1 WHERE id = :id")
    Completable archive(String id);

    @Query("UPDATE conversations SET last_message_preview = :preview, message_count = message_count + 1, updated_at = :updatedAt WHERE id = :id")
    Completable updateLastMessage(String id, String preview, String updatedAt);
}
