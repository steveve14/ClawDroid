package com.clawdroid.core.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.clawdroid.core.data.db.entity.ToolCallEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface ToolCallDao {

    @Query("SELECT * FROM tool_calls WHERE message_id = :messageId")
    Flowable<List<ToolCallEntity>> getByMessage(String messageId);

    @Query("SELECT * FROM tool_calls WHERE id = :id")
    Single<ToolCallEntity> getById(String id);

    @Insert
    Completable insert(ToolCallEntity toolCall);

    @Update
    Completable update(ToolCallEntity toolCall);

    @Query("UPDATE tool_calls SET status = :status, tool_result = :result, duration_ms = :durationMs WHERE id = :id")
    Completable updateResult(String id, String status, String result, Integer durationMs);
}
