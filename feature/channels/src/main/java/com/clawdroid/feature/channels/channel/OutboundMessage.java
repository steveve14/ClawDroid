package com.clawdroid.feature.channels.channel;

import androidx.annotation.Nullable;

public class OutboundMessage {
    private final String channelId;
    private final String targetId;
    private final String content;
    @Nullable private final String replyToId;

    public OutboundMessage(String channelId, String targetId, String content,
                           @Nullable String replyToId) {
        this.channelId = channelId;
        this.targetId = targetId;
        this.content = content;
        this.replyToId = replyToId;
    }

    public String getChannelId() { return channelId; }
    public String getTargetId() { return targetId; }
    public String getContent() { return content; }
    @Nullable public String getReplyToId() { return replyToId; }
}
