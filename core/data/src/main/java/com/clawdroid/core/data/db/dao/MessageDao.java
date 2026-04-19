package com.clawdroid.core.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.clawdroid.core.data.db.entity.MessageEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface MessageDao {

    @Query("SELECT * FROM messages WHERE conversation_id = :convId ORDER BY created_at ASC")
    Flowable<List<MessageEntity>> getMessages(String convId);

    @Query("SELECT * FROM messages WHERE id = :id")
    Single<MessageEntity> getById(String id);

    @Insert
    Completable insert(MessageEntity message);

    @Update
    Completable update(MessageEntity message);

    @Query("DELETE FROM messages WHERE conversation_id = :convId")
    Completable deleteByConversation(String convId);

    @Query("SELECT COUNT(*) FROM messages WHERE conversation_id = :convId")
    Single<Integer> getMessageCount(String convId);

    @Query("SELECT * FROM messages WHERE conversation_id = :convId AND content LIKE '%' || :query || '%' ORDER BY created_at ASC")
    Flowable<List<MessageEntity>> search(String convId, String query);
}
