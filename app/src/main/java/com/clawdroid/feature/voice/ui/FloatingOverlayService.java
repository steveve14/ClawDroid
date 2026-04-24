package com.clawdroid.feature.voice.ui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.clawdroid.core.ai.AiProviderManager;
import com.clawdroid.core.ai.PromptBuilder;
import com.clawdroid.core.data.repository.SettingsRepository;
import com.clawdroid.core.model.AiConfig;
import com.clawdroid.core.model.AiMessage;
import com.clawdroid.core.model.AiRequest;
import com.clawdroid.app.R;
import com.clawdroid.feature.voice.speech.SpeechRecognitionManager;
import com.clawdroid.feature.voice.speech.TtsManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@AndroidEntryPoint
public class FloatingOverlayService extends Service {

    private static final String CHANNEL_ID = "floating_overlay_channel";
    private static final int NOTIFICATION_ID = 1002;
    public static final String ACTION_STOP = "com.clawdroid.STOP_OVERLAY";

    @Inject AiProviderManager providerManager;
    @Inject PromptBuilder promptBuilder;
    @Inject SettingsRepository settingsRepository;

    private WindowManager windowManager;
    private View overlayView;
    private SpeechRecognitionManager speechManager;
    private TtsManager ttsManager;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private boolean isListening = false;

    private ImageView ivMic;
    private ProgressBar progress;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        speechManager = new SpeechRecognitionManager(this);
        ttsManager = new TtsManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildNotification());
        showOverlay();
        return START_STICKY;
    }

    private void showOverlay() {
        if (overlayView != null) return;

        overlayView = LayoutInflater.from(this).inflate(R.layout.layout_floating_overlay, null);
        ivMic = overlayView.findViewById(R.id.ivOverlayMic);
        progress = overlayView.findViewById(R.id.progressOverlay);

        int layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 16;
        params.y = 200;

        setupTouch(params);
        setupSpeechCallback();

        overlayView.setOnClickListener(v -> toggleListening());

        windowManager.addView(overlayView, params);
    }

    private void setupTouch(WindowManager.LayoutParams params) {
        overlayView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean moved = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                            moved = true;
                        }
                        params.x = initialX - (int) dx;
                        params.y = initialY + (int) dy;
                        windowManager.updateViewLayout(overlayView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            v.performClick();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void toggleListening() {
        if (isListening) {
            stopListening();
        } else {
            startListening();
        }
    }

    private void startListening() {
        isListening = true;
        ivMic.setColorFilter(0xFFEF4444);
        overlayView.setScaleX(1.2f);
        overlayView.setScaleY(1.2f);
        speechManager.startListening();
    }

    private void stopListening() {
        isListening = false;
        ivMic.clearColorFilter();
        overlayView.setScaleX(1.0f);
        overlayView.setScaleY(1.0f);
        speechManager.stopListening();
    }

    private void setupSpeechCallback() {
        speechManager.setCallback(new SpeechRecognitionManager.Callback() {
            @Override
            public void onPartialResult(String text) {
                // Visual feedback during partial result
            }

            @Override
            public void onFinalResult(String text) {
                isListening = false;
                ivMic.clearColorFilter();
                showProcessing(true);
                processWithAi(text);
            }

            @Override
            public void onError(String message) {
                isListening = false;
                ivMic.clearColorFilter();
                overlayView.setScaleX(1.0f);
                overlayView.setScaleY(1.0f);
            }

            @Override
            public void onRmsChanged(float rms) {
                if (overlayView != null) {
                    float scale = 1.0f + (rms / 15f);
                    overlayView.setScaleX(Math.min(scale, 1.4f));
                    overlayView.setScaleY(Math.min(scale, 1.4f));
                }
            }
        });
    }

    private void processWithAi(String userText) {
        String systemPrompt = settingsRepository.getSystemPrompt();
        List<AiMessage> prompt = promptBuilder.build(
                systemPrompt, Collections.emptyList(), userText, null);

        String overlayProvider = settingsRepository.getActiveProvider();
        String overlayModel = overlayProvider != null ? settingsRepository.getDefaultModelId(overlayProvider) : null;
        String overlayModelKey = settingsRepository.buildModelKey(overlayProvider, overlayModel);
        AiConfig config = new AiConfig(
                settingsRepository.getTemperatureForModel(overlayModelKey),
                settingsRepository.getTopPForModel(overlayModelKey),
                40,
                settingsRepository.getMaxTokensForModel(overlayModelKey));

        AiRequest request = new AiRequest(prompt, null, config, null);

        StringBuilder response = new StringBuilder();
        disposables.add(
                providerManager.generateStream(request)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                response::append,
                                e -> showProcessing(false),
                                () -> {
                                    showProcessing(false);
                                    ttsManager.speak(response.toString());
                                }
                        )
        );
    }

    private void showProcessing(boolean processing) {
        if (progress != null) {
            progress.setVisibility(processing ? View.VISIBLE : View.GONE);
        }
        if (ivMic != null) {
            ivMic.setVisibility(processing ? View.GONE : View.VISIBLE);
        }
    }

    private void removeOverlay() {
        if (overlayView != null) {
            windowManager.removeView(overlayView);
            overlayView = null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "플로팅 음성 대화",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("플로팅 오버레이로 음성 대화 진행 중");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent stopIntent = new Intent(this, FloatingOverlayService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(
                this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ClawDroid")
                .setContentText("플로팅 음성 대화 활성")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(android.R.drawable.ic_delete, "종료", stopPending)
                .build();
    }

    @Override
    public void onDestroy() {
        disposables.clear();
        removeOverlay();
        if (speechManager != null) {
            speechManager.destroy();
        }
        if (ttsManager != null) {
            ttsManager.destroy();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
