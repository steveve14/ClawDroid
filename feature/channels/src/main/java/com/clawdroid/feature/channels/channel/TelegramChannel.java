package com.clawdroid.feature.channels.channel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Collections;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TelegramChannel implements Channel {

    private final String id;
    private final String botToken;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final MutableLiveData<ChannelStatus> statusLiveData =
            new MutableLiveData<>(ChannelStatus.DISCONNECTED);
    private final PublishSubject<InboundMessage> messageSubject = PublishSubject.create();
    private volatile boolean running = false;
    private Thread pollingThread;

    private static final String BASE_URL = "https://api.telegram.org/bot";

    public TelegramChannel(String id, String botToken,
                           OkHttpClient httpClient, Gson gson) {
        this.id = id;
        this.botToken = botToken;
        this.httpClient = httpClient;
        this.gson = gson;
    }

    @Override public String getId() { return id; }
    @Override public ChannelType getType() { return ChannelType.TELEGRAM; }
    @Override public LiveData<ChannelStatus> getStatus() { return statusLiveData; }

    @Override
    public Completable connect() {
        return Completable.fromAction(() -> {
            statusLiveData.postValue(ChannelStatus.CONNECTING);
            running = true;
            pollingThread = new Thread(this::pollLoop, "telegram-poll-" + id);
            pollingThread.setDaemon(true);
            pollingThread.start();
            statusLiveData.postValue(ChannelStatus.CONNECTED);
        });
    }

    @Override
    public Completable disconnect() {
        return Completable.fromAction(() -> {
            running = false;
            if (pollingThread != null) pollingThread.interrupt();
            statusLiveData.postValue(ChannelStatus.DISCONNECTED);
        });
    }

    @Override
    public Completable sendMessage(OutboundMessage message) {
        return Completable.fromAction(() -> {
            JsonObject body = new JsonObject();
            body.addProperty("chat_id", message.getTargetId());
            body.addProperty("text", message.getContent());
            if (message.getReplyToId() != null) {
                body.addProperty("reply_to_message_id", message.getReplyToId());
            }
            body.addProperty("parse_mode", "Markdown");

            RequestBody reqBody = RequestBody.create(
                    MediaType.parse("application/json"), gson.toJson(body));
            Request request = new Request.Builder()
                    .url(BASE_URL + botToken + "/sendMessage")
                    .post(reqBody)
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Telegram sendMessage failed: " + response.code());
                }
            }
        });
    }

    @Override
    public Observable<InboundMessage> incomingMessages() {
        return messageSubject.hide();
    }

    private void pollLoop() {
        long offset = 0;
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                Request request = new Request.Builder()
                        .url(BASE_URL + botToken
                                + "/getUpdates?offset=" + offset + "&timeout=30")
                        .build();
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) continue;
                    String responseBody = response.body().string();
                    JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                    if (!json.has("result")) continue;

                    JsonArray results = json.getAsJsonArray("result");
                    for (JsonElement elem : results) {
                        JsonObject update = elem.getAsJsonObject();
                        offset = update.get("update_id").getAsLong() + 1;
                        processUpdate(update);
                    }
                }
            } catch (IOException e) {
                if (!running) break;
                statusLiveData.postValue(ChannelStatus.ERROR);
                try { Thread.sleep(5000); } catch (InterruptedException ie) { break; }
                statusLiveData.postValue(ChannelStatus.CONNECTING);
            }
        }
    }

    private void processUpdate(JsonObject update) {
        if (!update.has("message")) return;
        JsonObject msg = update.getAsJsonObject("message");
        if (!msg.has("text")) return;

        String chatId = String.valueOf(msg.getAsJsonObject("chat").get("id").getAsLong());
        JsonObject from = msg.getAsJsonObject("from");
        String senderId = String.valueOf(from.get("id").getAsLong());
        String senderName = from.has("first_name")
                ? from.get("first_name").getAsString() : "Unknown";
        String text = msg.get("text").getAsString();
        String msgId = String.valueOf(msg.get("message_id").getAsLong());

        InboundMessage inbound = new InboundMessage(
                id, senderId, senderName, text,
                Collections.emptyList(), msgId);
        messageSubject.onNext(inbound);
    }
}
