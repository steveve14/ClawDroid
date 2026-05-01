package com.clawdroid.core.locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

public final class AppLocaleManager {

    public static final String PREF_NAME = "clawdroid_prefs";
    public static final String KEY_APP_LANGUAGE = "app_language";
    public static final String LANGUAGE_KOREAN = "ko";
    public static final String LANGUAGE_ENGLISH = "en";

    private AppLocaleManager() {}

    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return normalizeLanguage(prefs.getString(KEY_APP_LANGUAGE, LANGUAGE_KOREAN));
    }

    public static void setLanguage(Context context, String languageTag) {
        String normalized = normalizeLanguage(languageTag);
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_APP_LANGUAGE, normalized)
                .apply();
        applyLanguage(normalized);
    }

    public static void applySavedLanguage(Context context) {
        applyLanguage(getSavedLanguage(context));
    }

    public static Context wrap(Context context) {
        String languageTag = getSavedLanguage(context);
        Locale locale = Locale.forLanguageTag(languageTag);
        Locale.setDefault(locale);

        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(new LocaleList(locale));
        } else {
            configuration.setLocale(locale);
        }
        return context.createConfigurationContext(configuration);
    }

    public static String normalizeLanguage(String languageTag) {
        if (LANGUAGE_ENGLISH.equals(languageTag)) {
            return LANGUAGE_ENGLISH;
        }
        return LANGUAGE_KOREAN;
    }

    private static void applyLanguage(String languageTag) {
        AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(normalizeLanguage(languageTag)));
    }
}