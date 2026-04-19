package com.clawdroid.app;

import android.app.Application;

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
        super.onCreate();
        scheduleAutoDelete();
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
