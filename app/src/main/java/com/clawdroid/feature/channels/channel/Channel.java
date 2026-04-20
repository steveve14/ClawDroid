package com.clawdroid.feature.channels.channel;

import androidx.lifecycle.LiveData;

import com.clawdroid.core.model.ToolResult;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;

public interface Channel {
    String getId();
    ChannelType getType();
    LiveData<ChannelStatus> getStatus();
    Completable connect();
    Completable disconnect();
    Completable sendMessage(OutboundMessage message);
    Observable<InboundMessage> incomingMessages();
}
