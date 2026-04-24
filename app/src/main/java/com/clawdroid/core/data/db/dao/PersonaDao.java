package com.clawdroid.core.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.clawdroid.core.data.db.entity.PersonaEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface PersonaDao {

    @Query("SELECT * FROM personas ORDER BY is_active DESC, created_at ASC")
    Flowable<List<PersonaEntity>> getAll();

    @Query("SELECT * FROM personas WHERE is_active = 1 LIMIT 1")
    Single<PersonaEntity> getActive();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insert(PersonaEntity persona);

    @Update
    Completable update(PersonaEntity persona);

    @Query("DELETE FROM personas WHERE id = :id")
    Completable deleteById(String id);

    @Query("UPDATE personas SET is_active = 0")
    Completable clearActive();

    @Query("UPDATE personas SET is_active = 1 WHERE id = :id")
    Completable setActive(String id);

    @Query("SELECT COUNT(*) FROM personas")
    Single<Integer> count();
}
