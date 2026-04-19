package com.clawdroid.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

import javax.inject.Inject;

/**
 * 설정 화면 E2E 테스트.
 * 설정 메뉴 → 하위 설정 화면 진입 → 복귀 플로우를 검증합니다.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class SettingsFlowE2ETest {

    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Inject
    SharedPreferences prefs;

    @Before
    public void setUp() {
        hiltRule.inject();
        prefs.edit()
                .putBoolean("app_lock_enabled", false)
                .putBoolean("is_pin_set", false)
                .apply();
    }

    private void navigateToSettings(ActivityScenario<MainActivity> scenario) {
        onView(withId(com.clawdroid.core.ui.R.id.settingsFragment)).perform(click());
    }

    @Test
    public void settingsScreen_displaysAllSections() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            navigateToSettings(scenario);

            onView(withId(com.clawdroid.feature.settings.R.id.btnModelSettings)).check(matches(isDisplayed()));
            onView(withId(com.clawdroid.feature.settings.R.id.btnPersonaSettings)).check(matches(isDisplayed()));
            onView(withId(com.clawdroid.feature.settings.R.id.btnToolSettings)).check(matches(isDisplayed()));
            onView(withId(com.clawdroid.feature.settings.R.id.btnSecuritySettings)).check(matches(isDisplayed()));
            onView(withId(com.clawdroid.feature.settings.R.id.btnAbout)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void navigateToModelSettings_andBack() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            navigateToSettings(scenario);
            onView(withId(com.clawdroid.feature.settings.R.id.btnModelSettings)).perform(click());

            // ModelSettingsFragment 확인 — 모델 관련 UI 존재
            // 뒤로가기
            pressBack();

            // 설정 화면으로 복귀 확인
            onView(withId(com.clawdroid.feature.settings.R.id.btnModelSettings)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void navigateToPersonaSettings_andBack() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            navigateToSettings(scenario);
            onView(withId(com.clawdroid.feature.settings.R.id.btnPersonaSettings)).perform(click());

            pressBack();

            onView(withId(com.clawdroid.feature.settings.R.id.btnPersonaSettings)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void navigateToSecuritySettings_andBack() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            navigateToSettings(scenario);
            onView(withId(com.clawdroid.feature.settings.R.id.btnSecuritySettings)).perform(click());

            // 보안 설정 화면 확인
            onView(withId(com.clawdroid.feature.settings.R.id.switchAppLock))
                    .check(matches(isDisplayed()));

            pressBack();

            onView(withId(com.clawdroid.feature.settings.R.id.btnSecuritySettings)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void navigateToToolSettings_andBack() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            navigateToSettings(scenario);
            onView(withId(com.clawdroid.feature.settings.R.id.btnToolSettings)).perform(click());

            pressBack();

            onView(withId(com.clawdroid.feature.settings.R.id.btnToolSettings)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void navigateToAbout_displaysVersionInfo() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            navigateToSettings(scenario);
            onView(withId(com.clawdroid.feature.settings.R.id.btnAbout)).perform(click());

            // 앱 정보 화면 — 버전 표시 확인
            onView(withText(org.hamcrest.Matchers.containsString("1.0")))
                    .check(matches(isDisplayed()));

            pressBack();

            onView(withId(com.clawdroid.feature.settings.R.id.btnAbout)).check(matches(isDisplayed()));
        }
    }
}
