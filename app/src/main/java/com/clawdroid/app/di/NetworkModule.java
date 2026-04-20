package com.clawdroid.app.di;

import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import com.clawdroid.app.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        // Debug: HEADERS only (no body → prevents API key/prompt/response leakage)
        // Release: NONE (no logging at all)
        logging.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.HEADERS
                : HttpLoggingInterceptor.Level.NONE);

        // SEC-H2: Redact every known-sensitive header regardless of build type.
        logging.redactHeader("Authorization");
        logging.redactHeader("Proxy-Authorization");
        logging.redactHeader("Cookie");
        logging.redactHeader("Set-Cookie");
        logging.redactHeader("x-api-key");
        logging.redactHeader("x-goog-api-key");
        logging.redactHeader("api-key");
        logging.redactHeader("anthropic-api-key");
        logging.redactHeader("OpenAI-Organization");
        logging.redactHeader("X-Slack-Signature");
        logging.redactHeader("X-Hub-Signature");
        logging.redactHeader("X-Hub-Signature-256");

        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                // URL path 내 토큰(예: Telegram bot token) 마스킹 헬퍼 설치
                .addInterceptor(new SensitiveHeaderInterceptor())
                .addInterceptor(logging)
                .build();
    }

    @Provides
    @Singleton
    public Retrofit.Builder provideRetrofitBuilder(OkHttpClient client, Gson gson) {
        return new Retrofit.Builder()
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create());
    }
}
