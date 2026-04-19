package com.clawdroid.app;

import android.app.Application;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class ClawDroidApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
