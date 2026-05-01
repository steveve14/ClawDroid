package com.clawdroid.feature.voice.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import com.clawdroid.app.R;
import com.clawdroid.core.locale.AppLocaleManager;

import java.util.ArrayList;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class SpeechRecognitionManager {

    private static final long NO_RESPONSE_TIMEOUT_MS = 7000L;
    private static final long MAX_LISTENING_DURATION_MS = 30000L;

    public interface Callback {
        void onPartialResult(String text);
        void onFinalResult(String text);
        void onError(String message);
        void onRmsChanged(float rms);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private Callback callback;
    private boolean isListening = false;
    private boolean speechStarted = false;
    private boolean stoppedManually = false;

    private final Runnable noResponseTimeoutRunnable = () -> {
        if (isListening && !speechStarted) {
            stopWithMessage(getString(R.string.speech_error_no_response));
        }
    };

    private final Runnable maxDurationTimeoutRunnable = () -> {
        if (isListening) {
            stopWithMessage(getString(R.string.speech_error_too_long));
        }
    };

    @Inject
    public SpeechRecognitionManager(@ApplicationContext Context context) {
        this.context = context;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public boolean isListening() {
        return isListening;
    }

    public void startListening() {
        if (isListening) return;

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            if (callback != null) callback.onError(getString(R.string.speech_error_unavailable));
            return;
        }

        stopListening();
        stoppedManually = false;
        speechStarted = false;
        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {
                speechStarted = true;
                mainHandler.removeCallbacks(noResponseTimeoutRunnable);
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                if (callback != null) callback.onRmsChanged(rmsdB);
            }

            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {
                isListening = false;
                cancelTimeouts();
            }

            @Override
            public void onError(int error) {
                if (stoppedManually) return;
                isListening = false;
                cancelTimeouts();
                String msg;
                switch (error) {
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        msg = getString(R.string.speech_error_no_response);
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        msg = getString(R.string.speech_error_no_match);
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        msg = getString(R.string.speech_error_network);
                        break;
                    case SpeechRecognizer.ERROR_AUDIO:
                        msg = getString(R.string.speech_error_audio);
                        break;
                    default:
                        msg = getString(R.string.speech_error_generic, error);
                        break;
                }
                if (callback != null) callback.onError(msg);
            }

            @Override
            public void onResults(Bundle results) {
                if (stoppedManually) return;
                isListening = false;
                cancelTimeouts();
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty() && callback != null) {
                    callback.onFinalResult(matches.get(0));
                } else if (callback != null) {
                    callback.onError(getString(R.string.speech_error_no_match));
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty() && callback != null) {
                    callback.onPartialResult(matches.get(0));
                }
            }

            @Override public void onEvent(int eventType, Bundle params) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, getRecognitionLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L);

        recognizer.startListening(intent);
        isListening = true;
        scheduleTimeouts();
    }

    public void stopListening() {
        stoppedManually = true;
        isListening = false;
        speechStarted = false;
        cancelTimeouts();
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.destroy();
            recognizer = null;
        }
    }

    private void stopWithMessage(String message) {
        stoppedManually = true;
        isListening = false;
        speechStarted = false;
        cancelTimeouts();
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.destroy();
            recognizer = null;
        }
        if (callback != null) {
            callback.onError(message);
        }
    }

    private void scheduleTimeouts() {
        cancelTimeouts();
        mainHandler.postDelayed(noResponseTimeoutRunnable, NO_RESPONSE_TIMEOUT_MS);
        mainHandler.postDelayed(maxDurationTimeoutRunnable, MAX_LISTENING_DURATION_MS);
    }

    private void cancelTimeouts() {
        mainHandler.removeCallbacks(noResponseTimeoutRunnable);
        mainHandler.removeCallbacks(maxDurationTimeoutRunnable);
    }

    private String getRecognitionLanguageTag() {
        String appLanguage = AppLocaleManager.getSavedLanguage(context);
        return AppLocaleManager.LANGUAGE_ENGLISH.equals(appLanguage)
                ? Locale.ENGLISH.toLanguageTag()
                : Locale.KOREA.toLanguageTag();
    }

    private String getString(int resId, Object... args) {
        return AppLocaleManager.wrap(context).getString(resId, args);
    }

    public void destroy() {
        stopListening();
        callback = null;
    }
}
