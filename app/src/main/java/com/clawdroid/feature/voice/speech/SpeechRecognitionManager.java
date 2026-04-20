package com.clawdroid.feature.voice.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class SpeechRecognitionManager {

    public interface Callback {
        void onPartialResult(String text);
        void onFinalResult(String text);
        void onError(String message);
        void onRmsChanged(float rms);
    }

    private final Context context;
    private SpeechRecognizer recognizer;
    private Callback callback;
    private boolean isListening = false;

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
            if (callback != null) callback.onError("음성 인식을 사용할 수 없습니다.");
            return;
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {
                if (callback != null) callback.onRmsChanged(rmsdB);
            }

            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { isListening = false; }

            @Override
            public void onError(int error) {
                isListening = false;
                String msg;
                switch (error) {
                    case SpeechRecognizer.ERROR_NO_MATCH: msg = "인식 결과 없음"; break;
                    case SpeechRecognizer.ERROR_NETWORK: msg = "네트워크 오류"; break;
                    case SpeechRecognizer.ERROR_AUDIO: msg = "오디오 오류"; break;
                    default: msg = "음성 인식 오류 (" + error + ")"; break;
                }
                if (callback != null) callback.onError(msg);
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty() && callback != null) {
                    callback.onFinalResult(matches.get(0));
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
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREA.toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        recognizer.startListening(intent);
        isListening = true;
    }

    public void stopListening() {
        isListening = false;
        if (recognizer != null) {
            recognizer.stopListening();
            recognizer.destroy();
            recognizer = null;
        }
    }

    public void destroy() {
        stopListening();
        callback = null;
    }
}
