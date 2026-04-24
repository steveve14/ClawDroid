package com.clawdroid.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.clawdroid.app.worker.AutoDeleteWorker;

import java.util.concurrent.TimeUnit;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class ClawDroidApp extends Application {

    @Override
    public void onCreate() {
        applySavedLocale();
        super.onCreate();
        scheduleAutoDelete();
    }

    private void applySavedLocale() {
        SharedPreferences prefs = getSharedPreferences("clawdroid_prefs", Context.MODE_PRIVATE);
        String lang = prefs.getString("app_language", "ko");
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang));
    }

    private void scheduleAutoDelete() {
        PeriodicWorkRequest autoDeleteWork = new PeriodicWorkRequest.Builder(
                AutoDeleteWorker.class, 24, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "auto_delete_conversations",
                ExistingPeriodicWorkPolicy.KEEP,
                autoDeleteWork);
    }
}
