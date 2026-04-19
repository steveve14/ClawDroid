package com.clawdroid.feature.voice.speech;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.speech.SpeechRecognizer;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.clawdroid.core.data.repository.SettingsRepository;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WakeWordService extends Service {

    private static final String CHANNEL_ID = "wake_word_channel";
    private static final int NOTIFICATION_ID = 1001;

    @Inject
    SettingsRepository settingsRepository;

    private SpeechRecognitionManager recognitionManager;
    private boolean isActive = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        recognitionManager = new SpeechRecognitionManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!settingsRepository.isWakeWordEnabled()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildNotification());
        startListeningForWakeWord();
        return START_STICKY;
    }

    private void startListeningForWakeWord() {
        if (isActive) return;
        isActive = true;

        recognitionManager.setCallback(new SpeechRecognitionManager.Callback() {
            @Override
            public void onPartialResult(String text) {
                checkForWakeWord(text);
            }

            @Override
            public void onFinalResult(String text) {
                checkForWakeWord(text);
                // Restart listening
                if (isActive) {
                    recognitionManager.startListening();
                }
            }

            @Override
            public void onError(String message) {
                // Restart after error
                if (isActive) {
                    recognitionManager.startListening();
                }
            }

            @Override
            public void onRmsChanged(float rms) {}
        });

        recognitionManager.startListening();
    }

    private void checkForWakeWord(String text) {
        if (text == null) return;
        String lower = text.toLowerCase();
        if (lower.contains("클로") || lower.contains("claw") || lower.contains("hey claw")) {
            // Broadcast wake word detected
            Intent broadcast = new Intent("com.clawdroid.WAKE_WORD_DETECTED");
            sendBroadcast(broadcast);

            // Launch floating overlay
            Intent overlayIntent = new Intent(this,
                    com.clawdroid.feature.voice.ui.FloatingOverlayService.class);
            startService(overlayIntent);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "웨이크 워드 감지",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("음성 명령 대기 중");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ClawDroid")
                .setContentText("음성 명령 대기 중...")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        isActive = false;
        if (recognitionManager != null) {
            recognitionManager.destroy();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
