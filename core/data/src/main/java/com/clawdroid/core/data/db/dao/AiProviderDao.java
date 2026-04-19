package com.clawdroid.core.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.clawdroid.core.data.db.entity.AiProviderEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface AiProviderDao {

    @Query("SELECT * FROM ai_providers ORDER BY priority ASC")
    Flowable<List<AiProviderEntity>> getAll();

    @Query("SELECT * FROM ai_providers WHERE is_enabled = 1 ORDER BY priority ASC")
    Single<List<AiProviderEntity>> getEnabled();

    @Query("SELECT * FROM ai_providers WHERE id = :id")
    Single<AiProviderEntity> getById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insert(AiProviderEntity provider);

    @Update
    Completable update(AiProviderEntity provider);

    @Query("UPDATE ai_providers SET is_enabled = :enabled WHERE id = :id")
    Completable setEnabled(String id, int enabled);
}
