package com.clawdroid.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.clawdroid.feature.chat.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

import javax.inject.Inject;

/**
 * 채팅 플로우 E2E 테스트.
 * 대화 목록 → 새 대화 생성 → 메시지 전송 → 대화 목록 복귀 플로우를 검증합니다.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class ChatFlowE2ETest {

    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Inject
    SharedPreferences prefs;

    @Before
    public void setUp() {
        hiltRule.inject();
        // 앱 잠금 비활성화 — 테스트에서 LockActivity 우회
        prefs.edit()
                .putBoolean("app_lock_enabled", false)
                .putBoolean("is_pin_set", false)
                .apply();
    }

    @Test
    public void conversationListDisplayed_onLaunch() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // 대화 목록 화면이 표시되는지 확인
            onView(withId(R.id.recyclerConversations)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void createNewConversation_andNavigateToChat() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // FAB 클릭 → 새 대화 생성 → 채팅 화면으로 이동
            onView(withId(R.id.fabNewConversation)).perform(click());

            // 채팅 화면의 메시지 입력 필드가 표시되는지 확인
            onView(withId(R.id.etMessage)).check(matches(isDisplayed()));

            // 전송 버튼 존재 확인
            onView(withId(R.id.btnSend)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void sendMessage_displaysInRecyclerView() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // 새 대화 생성
            onView(withId(R.id.fabNewConversation)).perform(click());

            // 메시지 입력
            String testMessage = "안녕하세요, 테스트 메시지입니다.";
            onView(withId(R.id.etMessage))
                    .perform(typeText(testMessage), closeSoftKeyboard());

            // 전송
            onView(withId(R.id.btnSend)).perform(click());

            // 메시지가 RecyclerView에 표시되는지 확인
            onView(withText(testMessage)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void sendSlashCommand_status() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // 새 대화 생성
            onView(withId(R.id.fabNewConversation)).perform(click());

            // /status 명령어 입력
            onView(withId(R.id.etMessage))
                    .perform(typeText("/status"), closeSoftKeyboard());
            onView(withId(R.id.btnSend)).perform(click());

            // 상태 메시지가 표시되는지 확인 (모델명 또는 '상태' 키워드)
            onView(withText(org.hamcrest.Matchers.containsString("모델")))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void sendSlashCommand_new_resetsConversation() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            // 새 대화 생성
            onView(withId(R.id.fabNewConversation)).perform(click());

            // 메시지 전송
            onView(withId(R.id.etMessage))
                    .perform(typeText("첫 번째 메시지"), closeSoftKeyboard());
            onView(withId(R.id.btnSend)).perform(click());

            // /new 명령어로 새 대화 시작
            onView(withId(R.id.etMessage))
                    .perform(typeText("/new"), closeSoftKeyboard());
            onView(withId(R.id.btnSend)).perform(click());

            // 입력 필드가 비어있고 채팅 화면이 유지되는지 확인
            onView(withId(R.id.etMessage)).check(matches(withText("")));
        }
    }

    @Test
    public void toolbar_displaysConversationTitle() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.fabNewConversation)).perform(click());

            // 채팅 화면 툴바에 제목이 표시되는지 확인
            onView(withId(R.id.tvTitle)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void modelName_isDisplayedInChat() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.fabNewConversation)).perform(click());

            // 모델 이름 표시 확인
            onView(withId(R.id.tvModelName)).check(matches(isDisplayed()));
        }
    }
}
