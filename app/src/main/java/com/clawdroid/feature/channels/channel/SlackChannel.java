package com.clawdroid.feature.channels.channel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class SlackChannel implements Channel {

    private final String id;
    private final String appToken;
    private final String botToken;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final MutableLiveData<ChannelStatus> statusLiveData =
            new MutableLiveData<>(ChannelStatus.DISCONNECTED);
    private final PublishSubject<InboundMessage> messageSubject = PublishSubject.create();
    private WebSocket webSocket;
    private volatile boolean running = false;

    private static final String CONNECTIONS_OPEN = "https://slack.com/api/apps.connections.open";
    private static final String CHAT_POST = "https://slack.com/api/chat.postMessage";

    public SlackChannel(String id, String appToken, String botToken,
                        OkHttpClient httpClient, Gson gson) {
        this.id = id;
        this.appToken = appToken;
        this.botToken = botToken;
        this.httpClient = httpClient;
        this.gson = gson;
    }

    @Override public String getId() { return id; }
    @Override public ChannelType getType() { return ChannelType.SLACK; }
    @Override public LiveData<ChannelStatus> getStatus() { return statusLiveData; }

    @Override
    public Completable connect() {
        return Completable.fromAction(() -> {
            statusLiveData.postValue(ChannelStatus.CONNECTING);
            running = true;

            // Get WebSocket URL via Socket Mode
            RequestBody emptyBody = RequestBody.create(
                    MediaType.parse("application/json"), "");
            Request httpRequest = new Request.Builder()
                    .url(CONNECTIONS_OPEN)
                    .addHeader("Authorization", "Bearer " + appToken)
                    .post(emptyBody)
                    .build();

            String wsUrl;
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                JsonObject json = gson.fromJson(response.body().string(), JsonObject.class);
                if (!json.get("ok").getAsBoolean()) {
                    throw new IOException("Slack connections.open failed");
                }
                wsUrl = json.get("url").getAsString();
            }

            Request wsRequest = new Request.Builder().url(wsUrl).build();
            webSocket = httpClient.newWebSocket(wsRequest, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket ws, Response response) {
                    statusLiveData.postValue(ChannelStatus.CONNECTED);
                }

                @Override
                public void onMessage(WebSocket ws, String text) {
                    processSlackEvent(ws, text);
                }

                @Override
                public void onFailure(WebSocket ws, Throwable t, Response response) {
                    statusLiveData.postValue(ChannelStatus.ERROR);
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
            JsonObject body = new JsonObject();
            body.addProperty("channel", message.getTargetId());
            body.addProperty("text", message.getContent());
            if (message.getReplyToId() != null) {
                body.addProperty("thread_ts", message.getReplyToId());
            }

            RequestBody reqBody = RequestBody.create(
                    MediaType.parse("application/json"), gson.toJson(body));
            Request request = new Request.Builder()
                    .url(CHAT_POST)
                    .addHeader("Authorization", "Bearer " + botToken)
                    .post(reqBody)
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                // ignore response
            }
        });
    }

    @Override
    public Observable<InboundMessage> incomingMessages() {
        return messageSubject.hide();
    }

    private void processSlackEvent(WebSocket ws, String text) {
        JsonObject envelope = JsonParser.parseString(text).getAsJsonObject();
        String type = envelope.has("type") ? envelope.get("type").getAsString() : "";

        // Acknowledge the envelope
        if (envelope.has("envelope_id")) {
            JsonObject ack = new JsonObject();
            ack.addProperty("envelope_id", envelope.get("envelope_id").getAsString());
            ws.send(gson.toJson(ack));
        }

        if ("events_api".equals(type) && envelope.has("payload")) {
            JsonObject payload = envelope.getAsJsonObject("payload");
            JsonObject event = payload.has("event")
                    ? payload.getAsJsonObject("event") : null;
            if (event != null && "message".equals(event.get("type").getAsString())) {
                // Skip bot messages
                if (event.has("bot_id")) return;

                String channelId = event.get("channel").getAsString();
                String senderId = event.has("user") ? event.get("user").getAsString() : "";
                String content = event.has("text") ? event.get("text").getAsString() : "";
                String ts = event.has("ts") ? event.get("ts").getAsString() : "";

                InboundMessage inbound = new InboundMessage(
                        id, senderId, senderId, content,
                        Collections.emptyList(), ts);
                messageSubject.onNext(inbound);
            }
        }
    }
}
