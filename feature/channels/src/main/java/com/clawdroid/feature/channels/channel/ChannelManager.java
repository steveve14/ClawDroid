package com.clawdroid.feature.channels.channel;

import com.clawdroid.core.data.db.dao.ChannelDao;
import com.clawdroid.core.data.db.entity.ChannelEntity;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.OkHttpClient;

@Singleton
public class ChannelManager {

    private final ChannelDao channelDao;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Map<String, Channel> activeChannels = new ConcurrentHashMap<>();
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    public ChannelManager(ChannelDao channelDao, OkHttpClient httpClient, Gson gson) {
        this.channelDao = channelDao;
        this.httpClient = httpClient;
        this.gson = gson;
    }

    public Completable connectChannel(ChannelEntity entity) {
        return Completable.defer(() -> {
            Channel channel = createChannel(entity);
            if (channel == null) {
                return Completable.error(new IllegalArgumentException(
                        "Unknown channel type: " + entity.getType()));
            }
            return channel.connect()
                    .doOnComplete(() -> {
                        activeChannels.put(entity.getId(), channel);
                        channelDao.updateStatus(entity.getId(), "connected",
                                        String.valueOf(System.currentTimeMillis()))
                                .subscribeOn(Schedulers.io()).subscribe();
                    })
                    .doOnError(e ->
                        channelDao.updateStatus(entity.getId(), "error",
                                        String.valueOf(System.currentTimeMillis()))
                                .subscribeOn(Schedulers.io()).subscribe()
                    );
        }).subscribeOn(Schedulers.io());
    }

    public Completable disconnectChannel(String channelId) {
        return Completable.defer(() -> {
            Channel channel = activeChannels.remove(channelId);
            if (channel == null) return Completable.complete();
            return channel.disconnect()
                    .doOnComplete(() ->
                        channelDao.updateStatus(channelId, "disconnected",
                                        String.valueOf(System.currentTimeMillis()))
                                .subscribeOn(Schedulers.io()).subscribe()
                    );
        }).subscribeOn(Schedulers.io());
    }

    public Completable disconnectAll() {
        return Completable.merge(
                Observable.fromIterable(activeChannels.keySet())
                        .map(this::disconnectChannel)
                        .toList()
                        .blockingGet()
        );
    }

    public Channel getChannel(String channelId) {
        return activeChannels.get(channelId);
    }

    public Map<String, Channel> getActiveChannels() {
        return activeChannels;
    }

    public Observable<InboundMessage> allIncomingMessages() {
        return Observable.merge(
                Observable.fromIterable(activeChannels.values())
                        .map(Channel::incomingMessages)
        );
    }

    public void dispose() {
        disposables.clear();
        disconnectAll().subscribeOn(Schedulers.io()).subscribe();
    }

    private Channel createChannel(ChannelEntity entity) {
        JsonObject config = gson.fromJson(entity.getConfig(), JsonObject.class);
        switch (entity.getType()) {
            case "telegram": {
                String token = config.has("bot_token")
                        ? config.get("bot_token").getAsString() : "";
                return new TelegramChannel(entity.getId(), token, httpClient, gson);
            }
            case "discord": {
                String token = config.has("bot_token")
                        ? config.get("bot_token").getAsString() : "";
                return new DiscordChannel(entity.getId(), token, httpClient, gson);
            }
            case "slack": {
                String appToken = config.has("app_token")
                        ? config.get("app_token").getAsString() : "";
                String botToken = config.has("bot_token")
                        ? config.get("bot_token").getAsString() : "";
                return new SlackChannel(entity.getId(), appToken, botToken, httpClient, gson);
            }
            case "gateway": {
                String serverUrl = config.has("server_url")
                        ? config.get("server_url").getAsString() : "";
                String authToken = config.has("auth_token")
                        ? config.get("auth_token").getAsString() : "";
                return new GatewayChannel(entity.getId(), serverUrl, authToken, httpClient, gson);
            }
            default:
                return null;
        }
    }
}
