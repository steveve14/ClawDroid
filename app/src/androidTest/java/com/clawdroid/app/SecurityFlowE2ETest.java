package com.clawdroid.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

import javax.inject.Inject;

/**
 * 보안 설정 E2E 테스트.
 * PIN 잠금, 생체 인증, 암호화, 자동 삭제 UI를 검증합니다.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class SecurityFlowE2ETest {

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
                .putBoolean("biometric_enabled", false)
                .apply();
    }

    private void navigateToSecuritySettings() {
        onView(withId(com.clawdroid.core.ui.R.id.settingsFragment)).perform(click());
        onView(withId(com.clawdroid.feature.settings.R.id.btnSecuritySettings)).perform(click());
    }

    @Test
    public void securityScreen_displaysAllElements() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            navigateToSecuritySettings();

            // 앱 잠금 스위치 표시
            onView(withId(com.clawdroid.feature.settings.R.id.switchAppLock))
                    .check(matches(isDisplayed()));

            // 암호화 스위치 표시
            onView(withId(com.clawdroid.feature.settings.R.id.switchEncryption))
                    .check(matches(isDisplayed()));

            // 자동 삭제 스피너 표시
            onView(withId(com.clawdroid.feature.settings.R.id.spinnerAutoDelete))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void appLockSwitch_initiallyOff() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            navigateToSecuritySettings();

            // 앱 잠금 기본 상태: OFF
            onView(withId(com.clawdroid.feature.settings.R.id.switchAppLock))
                    .check(matches(isNotChecked()));
        }
    }

    @Test
    public void appLockSwitch_toggleOn_showsPinDialog() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            navigateToSecuritySettings();

            // 앱 잠금 스위치 ON → PIN 설정 다이얼로그 표시
            onView(withId(com.clawdroid.feature.settings.R.id.switchAppLock)).perform(click());

            // PIN 입력 다이얼로그가 표시되는지 확인 (AlertDialog)
            onView(withId(android.R.id.button1)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void encryptionSwitch_isDisplayed() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            navigateToSecuritySettings();

            onView(withId(com.clawdroid.feature.settings.R.id.switchEncryption))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void autoDeleteSpinner_isDisplayed() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            navigateToSecuritySettings();

            onView(withId(com.clawdroid.feature.settings.R.id.spinnerAutoDelete))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void securitySettings_navigateBack_returnsToSettings() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            navigateToSecuritySettings();

            // 뒤로가기
            Espresso.pressBack();

            // 설정 화면으로 복귀
            onView(withId(com.clawdroid.feature.settings.R.id.btnSecuritySettings))
                    .check(matches(isDisplayed()));
        }
    }
}
