package com.clawdroid.app;

import static androidx.test.espresso.Espresso.onView;
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
 * 음성 대화 E2E 테스트.
 * 음성 대화 화면 진입 및 UI 요소 표시를 검증합니다.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class VoiceChatE2ETest {

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

    private void navigateToVoiceChat() {
        onView(withId(com.clawdroid.core.ui.R.id.voiceChatFragment)).perform(click());
    }

    @Test
    public void voiceChatScreen_displaysAllControls() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            navigateToVoiceChat();

            // 녹음 버튼 확인
            onView(withId(com.clawdroid.feature.voice.R.id.btnRecord))
                    .check(matches(isDisplayed()));

            // 중지 버튼 확인
            onView(withId(com.clawdroid.feature.voice.R.id.btnStop))
                    .check(matches(isDisplayed()));

            // 설정 버튼 확인
            onView(withId(com.clawdroid.feature.voice.R.id.btnVoiceSettings))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void voiceChatScreen_displaysStatusText() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            navigateToVoiceChat();

            // 상태 텍스트 표시 확인 (기본: "탭하여 말하기")
            onView(withId(com.clawdroid.feature.voice.R.id.tvStatus))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void voiceChatScreen_displaysHistory() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            navigateToVoiceChat();

            // 음성 대화 기록 RecyclerView 확인
            onView(withId(com.clawdroid.feature.voice.R.id.recyclerHistory))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void voiceChatScreen_displaysVisualizerArea() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            navigateToVoiceChat();

            // 오디오 비주얼라이저 영역 표시 확인
            onView(withId(com.clawdroid.feature.voice.R.id.visualizerArea))
                    .check(matches(isDisplayed()));
        }
    }
}
