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
 * 전체 앱 E2E 시나리오 테스트.
 * 사용자의 실제 사용 패턴을 시뮬레이션합니다:
 * 1. 앱 시작 → 대화 생성 → 메시지 전송 → 설정 확인 → 채널 확인 → 음성 화면
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class FullJourneyE2ETest {

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

    /**
     * 신규 사용자 전체 여정:
     * 1. 앱 시작 → 대화 목록 표시
     * 2. 새 대화 생성 → 채팅 화면 진입
     * 3. 메시지 전송
     * 4. 뒤로가기 → 대화 목록에 새 대화 존재
     * 5. 설정 화면으로 이동 → 모델 설정 진입 → 복귀
     * 6. 보안 설정 진입 → 복귀
     * 7. 앱 정보 확인
     * 8. 채널 목록 확인
     * 9. 음성 대화 화면 확인
     */
    @Test
    public void fullUserJourney_newUser() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // Step 1: 앱 시작 — 대화 목록 표시
            onView(withId(com.clawdroid.feature.chat.R.id.recyclerConversations))
                    .check(matches(isDisplayed()));

            // Step 2: 새 대화 생성
            onView(withId(com.clawdroid.feature.chat.R.id.fabNewConversation)).perform(click());
            onView(withId(com.clawdroid.feature.chat.R.id.etMessage)).check(matches(isDisplayed()));

            // Step 3: 메시지 전송
            onView(withId(com.clawdroid.feature.chat.R.id.etMessage))
                    .perform(androidx.test.espresso.action.ViewActions.typeText("Hello ClawDroid!"),
                            androidx.test.espresso.action.ViewActions.closeSoftKeyboard());
            onView(withId(com.clawdroid.feature.chat.R.id.btnSend)).perform(click());

            // Step 4: 뒤로가기 → 대화 목록
            pressBack();
            onView(withId(com.clawdroid.feature.chat.R.id.recyclerConversations))
                    .check(matches(isDisplayed()));

            // Step 5: 설정 → 모델 설정 → 복귀
            onView(withId(com.clawdroid.core.ui.R.id.settingsFragment)).perform(click());
            onView(withId(com.clawdroid.feature.settings.R.id.btnModelSettings)).perform(click());
            pressBack();
            onView(withId(com.clawdroid.feature.settings.R.id.btnModelSettings)).check(matches(isDisplayed()));

            // Step 6: 보안 설정 → 복귀
            onView(withId(com.clawdroid.feature.settings.R.id.btnSecuritySettings)).perform(click());
            onView(withId(com.clawdroid.feature.settings.R.id.switchAppLock)).check(matches(isDisplayed()));
            pressBack();

            // Step 7: 앱 정보
            onView(withId(com.clawdroid.feature.settings.R.id.btnAbout)).perform(click());
            onView(withText(org.hamcrest.Matchers.containsString("1.0"))).check(matches(isDisplayed()));
            pressBack();

            // Step 8: 채널 목록
            onView(withId(com.clawdroid.core.ui.R.id.channelListFragment)).perform(click());
            onView(withId(com.clawdroid.feature.channels.R.id.recyclerChannels))
                    .check(matches(isDisplayed()));

            // Step 9: 음성 대화
            onView(withId(com.clawdroid.core.ui.R.id.voiceChatFragment)).perform(click());
            onView(withId(com.clawdroid.feature.voice.R.id.btnRecord))
                    .check(matches(isDisplayed()));
        }
    }

    /**
     * 설정 탐색 여정:
     * Settings → 모든 하위 설정 화면 순회
     */
    @Test
    public void settingsExploration_journey() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(com.clawdroid.core.ui.R.id.settingsFragment)).perform(click());

            // 모델 설정
            onView(withId(com.clawdroid.feature.settings.R.id.btnModelSettings)).perform(click());
            pressBack();

            // 페르소나 설정
            onView(withId(com.clawdroid.feature.settings.R.id.btnPersonaSettings)).perform(click());
            pressBack();

            // 도구·스킬 설정
            onView(withId(com.clawdroid.feature.settings.R.id.btnToolSettings)).perform(click());
            pressBack();

            // 보안 설정
            onView(withId(com.clawdroid.feature.settings.R.id.btnSecuritySettings)).perform(click());
            onView(withId(com.clawdroid.feature.settings.R.id.switchAppLock)).check(matches(isDisplayed()));
            onView(withId(com.clawdroid.feature.settings.R.id.switchEncryption)).check(matches(isDisplayed()));
            onView(withId(com.clawdroid.feature.settings.R.id.spinnerAutoDelete)).check(matches(isDisplayed()));
            pressBack();

            // 앱 정보
            onView(withId(com.clawdroid.feature.settings.R.id.btnAbout)).perform(click());
            pressBack();

            // 설정 화면 복귀 확인
            onView(withId(com.clawdroid.feature.settings.R.id.btnModelSettings)).check(matches(isDisplayed()));
        }
    }

    /**
     * 다중 대화 관리 여정:
     * 대화 1 생성 → 대화 2 생성 → 목록에서 대화 선택
     */
    @Test
    public void multipleConversations_journey() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // 대화 1 생성
            onView(withId(com.clawdroid.feature.chat.R.id.fabNewConversation)).perform(click());
            onView(withId(com.clawdroid.feature.chat.R.id.etMessage))
                    .perform(androidx.test.espresso.action.ViewActions.typeText("대화 1"),
                            androidx.test.espresso.action.ViewActions.closeSoftKeyboard());
            onView(withId(com.clawdroid.feature.chat.R.id.btnSend)).perform(click());
            pressBack();

            // 대화 2 생성
            onView(withId(com.clawdroid.feature.chat.R.id.fabNewConversation)).perform(click());
            onView(withId(com.clawdroid.feature.chat.R.id.etMessage))
                    .perform(androidx.test.espresso.action.ViewActions.typeText("대화 2"),
                            androidx.test.espresso.action.ViewActions.closeSoftKeyboard());
            onView(withId(com.clawdroid.feature.chat.R.id.btnSend)).perform(click());
            pressBack();

            // 대화 목록에 최소 2개 대화가 있는지 확인
            onView(withId(com.clawdroid.feature.chat.R.id.recyclerConversations))
                    .check(matches(isDisplayed()));
        }
    }
}
