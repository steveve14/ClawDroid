package com.clawdroid.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

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
 * 네비게이션 E2E 테스트.
 * BottomNavigationView를 통한 화면 전환을 검증합니다.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class NavigationE2ETest {

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

    @Test
    public void bottomNav_navigateToVoiceChat() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // 음성 대화 탭 클릭
            onView(withId(com.clawdroid.core.ui.R.id.voiceChatFragment))
                    .perform(click());

            // 음성 대화 UI 요소 확인
            onView(withId(com.clawdroid.feature.voice.R.id.btnRecord))
                    .check(matches(isDisplayed()));
            onView(withId(com.clawdroid.feature.voice.R.id.tvStatus))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void bottomNav_navigateToChannels() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // 채널 탭 클릭
            onView(withId(com.clawdroid.core.ui.R.id.channelListFragment))
                    .perform(click());

            // 채널 목록 UI 확인
            onView(withId(com.clawdroid.feature.channels.R.id.recyclerChannels))
                    .check(matches(isDisplayed()));
            onView(withId(com.clawdroid.feature.channels.R.id.fabAddChannel))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void bottomNav_navigateToSettings() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // 설정 탭 클릭
            onView(withId(com.clawdroid.core.ui.R.id.settingsFragment))
                    .perform(click());

            // 설정 화면 UI 요소 확인
            onView(withId(com.clawdroid.feature.settings.R.id.btnModelSettings))
                    .check(matches(isDisplayed()));
            onView(withId(com.clawdroid.feature.settings.R.id.btnSecuritySettings))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void bottomNav_navigateBackToChat() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // 설정으로 이동
            onView(withId(com.clawdroid.core.ui.R.id.settingsFragment))
                    .perform(click());

            // 다시 채팅 탭 클릭
            onView(withId(com.clawdroid.core.ui.R.id.conversationListFragment))
                    .perform(click());

            // 대화 목록 화면 복귀 확인
            onView(withId(com.clawdroid.feature.chat.R.id.recyclerConversations))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void bottomNav_cycleAllTabs() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Chat → Voice
            onView(withId(com.clawdroid.core.ui.R.id.voiceChatFragment)).perform(click());
            onView(withId(com.clawdroid.feature.voice.R.id.btnRecord)).check(matches(isDisplayed()));

            // Voice → Channels
            onView(withId(com.clawdroid.core.ui.R.id.channelListFragment)).perform(click());
            onView(withId(com.clawdroid.feature.channels.R.id.recyclerChannels)).check(matches(isDisplayed()));

            // Channels → Settings
            onView(withId(com.clawdroid.core.ui.R.id.settingsFragment)).perform(click());
            onView(withId(com.clawdroid.feature.settings.R.id.btnModelSettings)).check(matches(isDisplayed()));

            // Settings → Chat
            onView(withId(com.clawdroid.core.ui.R.id.conversationListFragment)).perform(click());
            onView(withId(com.clawdroid.feature.chat.R.id.recyclerConversations)).check(matches(isDisplayed()));
        }
    }
}
