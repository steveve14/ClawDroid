package com.clawdroid.core.locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AppLocaleManagerTest {

    @Test
    void normalizeLanguage_acceptsEnglish() {
        assertEquals(AppLocaleManager.LANGUAGE_ENGLISH,
                AppLocaleManager.normalizeLanguage(AppLocaleManager.LANGUAGE_ENGLISH));
    }

    @Test
    void normalizeLanguage_defaultsUnsupportedValuesToKorean() {
        assertEquals(AppLocaleManager.LANGUAGE_KOREAN,
                AppLocaleManager.normalizeLanguage(null));
        assertEquals(AppLocaleManager.LANGUAGE_KOREAN,
                AppLocaleManager.normalizeLanguage("ja"));
        assertEquals(AppLocaleManager.LANGUAGE_KOREAN,
                AppLocaleManager.normalizeLanguage(""));
    }
}
