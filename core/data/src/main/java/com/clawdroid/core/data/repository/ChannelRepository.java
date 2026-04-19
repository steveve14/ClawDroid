package com.clawdroid.core.data.repository;

import com.clawdroid.core.data.db.dao.ChannelDao;
import com.clawdroid.core.data.db.entity.ChannelEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Singleton
public class ChannelRepository {

    private final ChannelDao channelDao;

    @Inject
    public ChannelRepository(ChannelDao channelDao) {
        this.channelDao = channelDao;
    }

    public Flowable<List<ChannelEntity>> getAllChannels() {
        return channelDao.getAll();
    }

    public Single<ChannelEntity> getById(String id) {
        return channelDao.getById(id);
    }

    public Flowable<List<ChannelEntity>> getConnectedChannels() {
        return channelDao.getConnected();
    }

    public Single<ChannelEntity> createChannel(String type, String name, String config,
                                                String dmPolicy) {
        ChannelEntity entity = new ChannelEntity();
        String now = Instant.now().toString();
        entity.setId(UUID.randomUUID().toString());
        entity.setType(type);
        entity.setName(name);
        entity.setConfig(config);
        entity.setDmPolicy(dmPolicy);
        entity.setStatus("disconnected");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        return channelDao.insert(entity).andThen(Single.just(entity));
    }

    public Completable updateChannel(ChannelEntity channel) {
        return channelDao.update(channel);
    }

    public Completable deleteChannel(ChannelEntity channel) {
        return channelDao.delete(channel);
    }

    public Completable updateStatus(String channelId, String status) {
        String now = Instant.now().toString();
        return channelDao.updateStatus(channelId, status, now);
    }
}
