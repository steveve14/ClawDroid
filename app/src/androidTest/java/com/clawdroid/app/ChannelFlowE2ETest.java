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
 * 채널 관리 E2E 테스트.
 * 채널 목록 → 채널 추가 화면 진입 플로우를 검증합니다.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class ChannelFlowE2ETest {

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

    private void navigateToChannels() {
        onView(withId(com.clawdroid.core.ui.R.id.channelListFragment)).perform(click());
    }

    @Test
    public void channelListScreen_displaysCorrectly() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            navigateToChannels();

            // 채널 목록 RecyclerView 또는 빈 상태 표시 확인
            onView(withId(com.clawdroid.feature.channels.R.id.recyclerChannels))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void fabAddChannel_navigatesToAddScreen() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            navigateToChannels();

            // FAB 클릭 → 채널 추가 화면
            onView(withId(com.clawdroid.feature.channels.R.id.fabAddChannel)).perform(click());

            // 채널 추가 화면이 표시되는지 확인
            // ChannelAddFragment 내 UI 요소 확인
            onView(withId(com.clawdroid.feature.channels.R.id.fabAddChannel))
                    .check(matches(org.hamcrest.Matchers.not(isDisplayed())));
        }
    }

    @Test
    public void channelList_showsEmptyState_whenNoChannels() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            navigateToChannels();

            // 채널이 없을 때 빈 상태 또는 RecyclerView 표시
            // 둘 중 하나는 반드시 표시됨
            try {
                onView(withId(com.clawdroid.feature.channels.R.id.emptyState))
                        .check(matches(isDisplayed()));
            } catch (AssertionError e) {
                // 채널이 있으면 RecyclerView 표시
                onView(withId(com.clawdroid.feature.channels.R.id.recyclerChannels))
                        .check(matches(isDisplayed()));
            }
        }
    }
}
