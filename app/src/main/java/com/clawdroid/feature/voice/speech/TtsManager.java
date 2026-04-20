package com.clawdroid.feature.voice.speech;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class TtsManager {

    public interface Callback {
        void onStart();
        void onDone();
        void onError(String message);
    }

    private final Context context;
    private TextToSpeech tts;
    private boolean isReady = false;
    private Callback callback;

    @Inject
    public TtsManager(@ApplicationContext Context context) {
        this.context = context;
        initTts();
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    private void initTts() {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.KOREA);
                isReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;

                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) {
                        if (callback != null) callback.onStart();
                    }
                    @Override public void onDone(String utteranceId) {
                        if (callback != null) callback.onDone();
                    }
                    @Override public void onError(String utteranceId) {
                        if (callback != null) callback.onError("TTS 오류");
                    }
                });
            }
        });
    }

    public void speak(String text) {
        if (!isReady || tts == null) return;
        String utteranceId = UUID.randomUUID().toString();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }

    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    public boolean isSpeaking() {
        return tts != null && tts.isSpeaking();
    }

    public void destroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        callback = null;
    }
}
