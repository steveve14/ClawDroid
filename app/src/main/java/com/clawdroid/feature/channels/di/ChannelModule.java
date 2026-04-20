package com.clawdroid.feature.channels.di;

import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Channel module — OkHttpClient and Gson already provided by AppModule/NetworkModule.
 */
@Module
@InstallIn(SingletonComponent.class)
public class ChannelModule {
    // Dependencies provided by app-level modules
}
