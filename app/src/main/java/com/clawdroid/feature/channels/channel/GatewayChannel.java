package com.clawdroid.feature.channels.channel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Collections;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class GatewayChannel implements Channel {

    private final String id;
    private final String serverUrl;
    private final String authToken;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final MutableLiveData<ChannelStatus> statusLiveData =
            new MutableLiveData<>(ChannelStatus.DISCONNECTED);
    private final PublishSubject<InboundMessage> messageSubject = PublishSubject.create();
    private WebSocket webSocket;
    private volatile boolean running = false;

    public GatewayChannel(String id, String serverUrl, String authToken,
                          OkHttpClient httpClient, Gson gson) {
        this.id = id;
        this.serverUrl = serverUrl;
        this.authToken = authToken;
        this.httpClient = httpClient;
        this.gson = gson;
    }

    @Override public String getId() { return id; }
    @Override public ChannelType getType() { return ChannelType.GATEWAY; }
    @Override public LiveData<ChannelStatus> getStatus() { return statusLiveData; }

    @Override
    public Completable connect() {
        return Completable.fromAction(() -> {
            // Enforce TLS for token security
            if (!serverUrl.startsWith("wss://") && !serverUrl.startsWith("https://")) {
                throw new IllegalArgumentException(
                        "Gateway 서버는 보안 연결(wss://)이 필요합니다.");
            }

            statusLiveData.postValue(ChannelStatus.CONNECTING);
            running = true;

            Request wsRequest = new Request.Builder()
                    .url(serverUrl)
                    .addHeader("Authorization", "Bearer " + authToken)
                    .build();

            webSocket = httpClient.newWebSocket(wsRequest, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket ws, Response response) {
                    statusLiveData.postValue(ChannelStatus.CONNECTED);
                }

                @Override
                public void onMessage(WebSocket ws, String text) {
                    processGatewayMessage(text);
                }

                @Override
                public void onFailure(WebSocket ws, Throwable t, Response response) {
                    statusLiveData.postValue(ChannelStatus.ERROR);
                    if (running) reconnect();
                }

                @Override
                public void onClosed(WebSocket ws, int code, String reason) {
                    statusLiveData.postValue(ChannelStatus.DISCONNECTED);
                }
            });
        });
    }

    @Override
    public Completable disconnect() {
        return Completable.fromAction(() -> {
            running = false;
            if (webSocket != null) webSocket.close(1000, "Disconnect");
            statusLiveData.postValue(ChannelStatus.DISCONNECTED);
        });
    }

    @Override
    public Completable sendMessage(OutboundMessage message) {
        return Completable.fromAction(() -> {
            JsonObject payload = new JsonObject();
            payload.addProperty("type", "message");
            payload.addProperty("channel_id", message.getChannelId());
            payload.addProperty("target_id", message.getTargetId());
            payload.addProperty("content", message.getContent());
            if (message.getReplyToId() != null) {
                payload.addProperty("reply_to", message.getReplyToId());
            }
            webSocket.send(gson.toJson(payload));
        });
    }

    @Override
    public Observable<InboundMessage> incomingMessages() {
        return messageSubject.hide();
    }

    private void processGatewayMessage(String text) {
        JsonObject json = JsonParser.parseString(text).getAsJsonObject();
        String type = json.has("type") ? json.get("type").getAsString() : "";

        if ("message".equals(type)) {
            String senderId = json.has("sender_id") ? json.get("sender_id").getAsString() : "";
            String senderName = json.has("sender_name") ? json.get("sender_name").getAsString() : senderId;
            String content = json.has("content") ? json.get("content").getAsString() : "";
            String replyTo = json.has("reply_to") ? json.get("reply_to").getAsString() : null;

            InboundMessage inbound = new InboundMessage(
                    id, senderId, senderName, content,
                    Collections.emptyList(), replyTo);
            messageSubject.onNext(inbound);
        }
    }

    private void reconnect() {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                if (running) connect().subscribe();
            } catch (InterruptedException ignored) {}
        }, "gateway-reconnect-" + id).start();
    }
}
