package com.clawdroid.feature.channels.channel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

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

public class DiscordChannel implements Channel {

    private final String id;
    private final String botToken;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final MutableLiveData<ChannelStatus> statusLiveData =
            new MutableLiveData<>(ChannelStatus.DISCONNECTED);
    private final PublishSubject<InboundMessage> messageSubject = PublishSubject.create();
    private WebSocket webSocket;
    private volatile boolean running = false;
    private int heartbeatInterval;
    private Thread heartbeatThread;
    private Integer lastSequence;

    private static final String GATEWAY_URL = "https://discord.com/api/v10/gateway";
    private static final String API_BASE = "https://discord.com/api/v10";

    public DiscordChannel(String id, String botToken,
                          OkHttpClient httpClient, Gson gson) {
        this.id = id;
        this.botToken = botToken;
        this.httpClient = httpClient;
        this.gson = gson;
    }

    @Override public String getId() { return id; }
    @Override public ChannelType getType() { return ChannelType.DISCORD; }
    @Override public LiveData<ChannelStatus> getStatus() { return statusLiveData; }

    @Override
    public Completable connect() {
        return Completable.fromAction(() -> {
            statusLiveData.postValue(ChannelStatus.CONNECTING);
            running = true;

            // Get gateway URL
            Request httpRequest = new Request.Builder()
                    .url(GATEWAY_URL)
                    .addHeader("Authorization", "Bot " + botToken)
                    .build();
            String gatewayUrl;
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                JsonObject gw = gson.fromJson(response.body().string(), JsonObject.class);
                gatewayUrl = gw.get("url").getAsString();
            }

            Request wsRequest = new Request.Builder()
                    .url(gatewayUrl + "/?v=10&encoding=json")
                    .build();

            webSocket = httpClient.newWebSocket(wsRequest, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket ws, Response response) {
                    // Hello event handled in onMessage
                }

                @Override
                public void onMessage(WebSocket ws, String text) {
                    processGatewayEvent(ws, text);
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
            if (heartbeatThread != null) heartbeatThread.interrupt();
            if (webSocket != null) webSocket.close(1000, "Disconnect");
            statusLiveData.postValue(ChannelStatus.DISCONNECTED);
        });
    }

    @Override
    public Completable sendMessage(OutboundMessage message) {
        return Completable.fromAction(() -> {
            JsonObject body = new JsonObject();
            body.addProperty("content", message.getContent());
            if (message.getReplyToId() != null) {
                JsonObject ref = new JsonObject();
                ref.addProperty("message_id", message.getReplyToId());
                body.add("message_reference", ref);
            }

            RequestBody reqBody = RequestBody.create(
                    MediaType.parse("application/json"), gson.toJson(body));
            Request request = new Request.Builder()
                    .url(API_BASE + "/channels/" + message.getTargetId() + "/messages")
                    .addHeader("Authorization", "Bot " + botToken)
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

    private void processGatewayEvent(WebSocket ws, String text) {
        JsonObject event = JsonParser.parseString(text).getAsJsonObject();
        int op = event.get("op").getAsInt();

        if (event.has("s") && !event.get("s").isJsonNull()) {
            lastSequence = event.get("s").getAsInt();
        }

        switch (op) {
            case 10: // Hello
                heartbeatInterval = event.getAsJsonObject("d")
                        .get("heartbeat_interval").getAsInt();
                sendIdentify(ws);
                startHeartbeat(ws);
                break;
            case 0: // Dispatch
                String eventType = event.get("t").getAsString();
                if ("READY".equals(eventType)) {
                    statusLiveData.postValue(ChannelStatus.CONNECTED);
                } else if ("MESSAGE_CREATE".equals(eventType)) {
                    processMessage(event.getAsJsonObject("d"));
                }
                break;
            case 11: // Heartbeat ACK
                break;
        }
    }

    private void sendIdentify(WebSocket ws) {
        JsonObject identify = new JsonObject();
        identify.addProperty("op", 2);
        JsonObject d = new JsonObject();
        d.addProperty("token", botToken);
        d.addProperty("intents", 513); // GUILDS + GUILD_MESSAGES
        JsonObject properties = new JsonObject();
        properties.addProperty("os", "android");
        properties.addProperty("browser", "clawdroid");
        properties.addProperty("device", "clawdroid");
        d.add("properties", properties);
        identify.add("d", d);
        ws.send(gson.toJson(identify));
    }

    private void startHeartbeat(WebSocket ws) {
        heartbeatThread = new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(heartbeatInterval);
                    JsonObject hb = new JsonObject();
                    hb.addProperty("op", 1);
                    if (lastSequence != null) {
                        hb.addProperty("d", lastSequence);
                    }
                    ws.send(gson.toJson(hb));
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "discord-heartbeat-" + id);
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    private void processMessage(JsonObject d) {
        if (d.has("author") && d.getAsJsonObject("author").has("bot")
                && d.getAsJsonObject("author").get("bot").getAsBoolean()) {
            return; // Ignore bot messages
        }

        String channelId = d.get("channel_id").getAsString();
        JsonObject author = d.getAsJsonObject("author");
        String senderId = author.get("id").getAsString();
        String senderName = author.has("username")
                ? author.get("username").getAsString() : "Unknown";
        String content = d.has("content") ? d.get("content").getAsString() : "";
        String msgId = d.get("id").getAsString();

        InboundMessage inbound = new InboundMessage(
                id, senderId, senderName, content,
                Collections.emptyList(), msgId);
        messageSubject.onNext(inbound);
    }
}
