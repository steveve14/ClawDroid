package com.clawdroid.core.data.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.clawdroid.core.data.db.entity.ChannelEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface ChannelDao {

    @Query("SELECT * FROM channels ORDER BY created_at DESC")
    Flowable<List<ChannelEntity>> getAll();

    @Query("SELECT * FROM channels WHERE id = :id")
    Single<ChannelEntity> getById(String id);

    @Query("SELECT * FROM channels WHERE status = 'connected'")
    Flowable<List<ChannelEntity>> getConnected();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insert(ChannelEntity channel);

    @Update
    Completable update(ChannelEntity channel);

    @Delete
    Completable delete(ChannelEntity channel);

    @Query("UPDATE channels SET status = :status, updated_at = :updatedAt WHERE id = :id")
    Completable updateStatus(String id, String status, String updatedAt);
}
