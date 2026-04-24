package com.clawdroid.core.data.repository;

import com.clawdroid.core.data.db.dao.PersonaDao;
import com.clawdroid.core.data.db.entity.PersonaEntity;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Singleton
public class PersonaRepository {

    private final PersonaDao personaDao;

    @Inject
    public PersonaRepository(PersonaDao personaDao) {
        this.personaDao = personaDao;
    }

    public Flowable<List<PersonaEntity>> getAll() {
        return personaDao.getAll();
    }

    public Single<PersonaEntity> getActive() {
        return personaDao.getActive();
    }

    public Completable save(PersonaEntity persona) {
        return personaDao.insert(persona);
    }

    public Completable update(PersonaEntity persona) {
        return personaDao.update(persona);
    }

    public Completable delete(String id) {
        return personaDao.deleteById(id);
    }

    public Completable setActive(String id) {
        return personaDao.clearActive()
                .andThen(personaDao.setActive(id));
    }

    public Single<Integer> count() {
        return personaDao.count();
    }
}
