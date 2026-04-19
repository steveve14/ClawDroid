package com.clawdroid.feature.channels.channel;

import androidx.annotation.Nullable;

import java.util.List;

public class InboundMessage {
    private final String channelId;
    private final String senderId;
    private final String senderName;
    private final String content;
    @Nullable private final List<byte[]> images;
    @Nullable private final String replyToId;
    private final long timestamp;

    public InboundMessage(String channelId, String senderId, String senderName,
                          String content, @Nullable List<byte[]> images,
                          @Nullable String replyToId) {
        this.channelId = channelId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.images = images;
        this.replyToId = replyToId;
        this.timestamp = System.currentTimeMillis();
    }

    public String getChannelId() { return channelId; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getContent() { return content; }
    @Nullable public List<byte[]> getImages() { return images; }
    @Nullable public String getReplyToId() { return replyToId; }
    public long getTimestamp() { return timestamp; }
}
