package com.clawdroid.feature.channels.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;

import com.clawdroid.core.data.db.dao.ChannelDao;
import com.clawdroid.feature.channels.channel.ChannelManager;
import com.clawdroid.feature.channels.channel.MessageRouter;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@AndroidEntryPoint
public class ChannelService extends LifecycleService {

    private static final String CHANNEL_ID = "channel_service";
    private static final int NOTIFICATION_ID = 2001;

    @Inject ChannelManager channelManager;
    @Inject MessageRouter messageRouter;
    @Inject ChannelDao channelDao;

    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        connectAllChannels();
        messageRouter.startRouting();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        messageRouter.stopRouting();
        channelManager.dispose();
        disposables.clear();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }

    private void connectAllChannels() {
        disposables.add(
                channelDao.getConnected()
                        .firstOrError()
                        .subscribeOn(Schedulers.io())
                        .subscribe(channels -> {
                            for (var entity : channels) {
                                channelManager.connectChannel(entity)
                                        .subscribeOn(Schedulers.io())
                                        .subscribe(() -> {}, e -> { /* connection error logged internally */ });
                            }
                        }, e -> { /* channel load error logged internally */ })
        );
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel nc = new NotificationChannel(
                    CHANNEL_ID, "채널 서비스",
                    NotificationManager.IMPORTANCE_LOW);
            nc.setDescription("외부 채널 연결 유지");
            getSystemService(NotificationManager.class).createNotificationChannel(nc);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ClawDroid 채널")
                .setContentText("채널 연결 활성 중")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build();
    }
}
