# 🦞 ClawDroid — 개발 TODO 리스트

> **최종 갱신**: 2026-04-19
> **기술 스택**: Java 17 · XML Views (ViewBinding) · RxJava 3 · Retrofit + OkHttp · Room · Hilt · Gemini Nano

---

## Phase 1 — MVP: AI 채팅 (코어) `3~4주`

### 🏗️ 프로젝트 초기 설정

- [ ] 멀티모듈 프로젝트 스캐폴딩 (`app`, `core:model`, `core:data`, `core:ai`, `core:ui`, `feature:*`)
- [ ] `settings.gradle` / 루트 `build.gradle` (Groovy DSL) 구성
- [ ] Version Catalog (`libs.versions.toml`) 전체 의존성 정의
- [ ] Hilt DI 모듈 기본 구성 (`AppModule`, `DatabaseModule`, `NetworkModule`)
- [ ] CI/CD 기초 설정 (GitHub Actions — 빌드 & 테스트)

### 🗃️ 데이터 레이어

- [ ] Room DB 초기 스키마 생성 (`Conversation`, `Message`, `AiModelConfig`, `Channel`)
- [ ] Room DAO 정의 (RxJava 3 — `Flowable`, `Single`, `Completable`)
- [ ] Room Migration 전략 수립
- [ ] Repository 패턴 구현 (`ConversationRepository`, `MessageRepository`)
- [ ] SharedPreferences / EncryptedSharedPreferences 유틸 구현
- [ ] API 키 암호화 저장 (`EncryptedSharedPreferences`)

### 🤖 AI 프로바이더

- [ ] `AiProvider` 인터페이스 정의 (`generate()` → `Single<AiResponse>`, `generateStream()` → `Observable<String>`)
- [ ] `AiProviderManager` 구현 (프로바이더 선택, 자동 폴백 로직)
- [ ] `GeminiNanoProvider` 구현 (AICore / ML Kit GenAI)
- [ ] Gemini Nano 가용성 런타임 체크 (`isAvailable()`)
- [ ] `GeminiCloudProvider` 구현 (Retrofit + OkHttp SSE)
- [ ] `OpenAiProvider` 구현 (Retrofit + OkHttp SSE)
- [ ] `OllamaProvider` 구현 (Retrofit + OkHttp)
- [ ] 커스텀 엔드포인트 프로바이더 지원
- [ ] `PromptBuilder` — 시스템 프롬프트 + 히스토리 + 도구 결과 조합

### 💬 채팅 UI

- [ ] `ConversationListFragment` + XML 레이아웃 (RecyclerView + ListAdapter)
- [ ] 대화 검색 (SearchView → Room FTS)
- [ ] 대화 스와이프 (ItemTouchHelper — 좌: 보관, 우: 삭제)
- [ ] 고정(Pinned) / 최근(Recent) 섹션 구분
- [ ] `ChatFragment` + XML 레이아웃 (RecyclerView + 메시지 입력)
- [ ] 스트리밍 응답 표시 (`Observable<String>` → RecyclerView 실시간 업데이트)
- [ ] Markdown 렌더링 (코드 블록 구문 강조, 테이블, 리스트)
- [ ] 모델 전환 UI (`BottomSheetDialogFragment`)
- [ ] 이미지 입력 (카메라/갤러리 → 멀티모달 AI)
- [ ] 대화 내보내기 (Markdown / JSON)
- [ ] 컨텍스트 압축 (`/compact` 명령어)

### 🔧 AI 모델 설정 화면

- [ ] `ModelSettingsFragment` — AI 모델 선택 UI (Base Model 카드)
- [ ] API 키 입력 / 저장 UI (Gemini Cloud, OpenAI)
- [ ] Ollama 서버 연결 설정 (IP:Port)
- [ ] 모델 파라미터 조절 (temperature, top_p, max_tokens)

### ⚙️ 설정 화면 기본

- [ ] `SettingsFragment` — 메인 설정 화면 (PreferenceFragmentCompat 또는 커스텀)
- [ ] Intelligence Core 섹션 (AI Models, Persona, Tools & Skills)
- [ ] Integrations 섹션 (Channels)
- [ ] Application 섹션 (App Lock, About)
- [ ] 하단 네비게이션 바 (`BottomNavigationView` — Chat, Voice, Channels, Settings)
- [ ] Navigation Component (`nav_graph.xml`) 전체 플로우 정의

---

## Phase 2 — 음성 대화 `2주`

### 🔊 STT (Speech-to-Text)

- [ ] ML Kit 음성 인식 온디바이스 통합
- [ ] `SpeechRecognizer` 폴백 구현
- [ ] 오디오 권한 요청 및 관리

### 🗣️ TTS (Text-to-Speech)

- [ ] Android TTS 기본 구현
- [ ] ElevenLabs TTS 선택적 지원 (Retrofit)
- [ ] TTS 프로바이더 선택 설정 UI

### 🎙️ Talk Mode UI

- [ ] `VoiceChatFragment` + XML 레이아웃
- [ ] 웨이브폼 오디오 비주얼라이저 (커스텀 View)
- [ ] PTT(Push-to-Talk) 버튼
- [ ] 음성 대화 로그 패널 (AI/User 메시지 표시)
- [ ] 리스닝 상태 / 처리 중 상태 UI 전환

### 🔔 Wake Word

- [ ] Wake Word 감지 엔진 통합 ("Hey Claw")
- [ ] Foreground Service로 백그라운드 Wake Word 감지
- [ ] 플로팅 오버레이 미니 UI (P2)

---

## Phase 3 — 멀티채널 연동 `3주`

### 🔗 채널 프레임워크

- [ ] `Channel` 인터페이스 정의 (`connect()`, `disconnect()`, `sendMessage()`)
- [ ] `InboundMessage` / `OutboundMessage` POJO 정의
- [ ] `ChannelService` (Foreground Service) — 채널 연결 유지
- [ ] `CompositeDisposable` 기반 채널 생명주기 관리

### 📱 Telegram 연동

- [ ] `TelegramChannel` 구현 (OkHttp Long Polling)
- [ ] Telegram Bot Token 입력 UI
- [ ] 텔레그램 연결 상태 표시

### 💬 Discord 연동

- [ ] `DiscordChannel` 구현 (OkHttp WebSocket — Gateway API)
- [ ] Discord Bot Token 입력 UI
- [ ] 서버/채널 권한 안내

### 📨 Slack 연동

- [ ] `SlackChannel` 구현 (OkHttp — Events API + Web API)
- [ ] Slack App 설정 가이드 UI

### 🖥️ 채널 관리 UI

- [ ] `ChannelListFragment` + XML 레이아웃 (채널 카드 목록)
- [ ] 채널 상태 표시 (Connected / Disconnected / Error)
- [ ] `ChannelAddFragment` — 새 채널 추가 플로우
- [ ] `ChannelDetailFragment` — 채널 상세 / 설정 / 연결 테스트
- [ ] DM 페어링 코드 발급 / 승인 UI

### 🌐 Gateway 모드 (P2)

- [ ] Gateway 서버 WebSocket 연결 클라이언트
- [ ] Gateway 모드 설정 UI

---

## Phase 4 — 도구·스킬 시스템 `2~3주`

### 🧰 도구 프레임워크

- [ ] `Tool` 인터페이스 정의 (`execute()` → `Single<ToolResult>`)
- [ ] `ToolManager` — 도구 등록 / 조회 / 실행
- [ ] `NanoFunctionCallingParser` — `<tool_call>` 태그 파싱 (Gemini Nano용)
- [ ] Function Calling 통합 (AI 응답 → 도구 호출 → 결과 반환 → AI 최종 응답)

### 🔧 내장 도구

- [ ] `BrowserTool` — 웹 검색 / 페이지 요약 (OkHttp + Jsoup)
- [ ] `CalculatorTool` — 수식 / 단위 변환
- [ ] `CalendarTool` — CalendarProvider로 일정 조회·생성
- [ ] `ContactsTool` — ContactsProvider로 연락처 검색
- [ ] `CameraTool` — 사진 촬영 → AI 이미지 분석
- [ ] `LocationTool` — FusedLocationProvider 현재 위치 / 장소 검색
- [ ] `AlarmTool` — AlarmManager 알림·리마인더 생성
- [ ] `AppLauncherTool` — Intent로 앱 실행 (P2)
- [ ] `FileManagerTool` — SAF로 파일 읽기/쓰기 (P2)

### 🎭 스킬 시스템

- [ ] `SkillLoader` — `SKILL.md` 파일 파싱 / 스킬 등록
- [ ] 내장 스킬 (날씨, 뉴스 요약, 번역)
- [ ] 커스텀 스킬 지원 (사용자 `SKILL.md` 작성)
- [ ] `ToolSettingsFragment` — 도구 활성화/비활성화 토글 UI
- [ ] `SkillSettingsFragment` — 스킬 목록 / 추가 / 관리 UI

---

## Phase 5 — 고급 기능 & 배포 `2주`

### 🔐 보안

- [ ] 앱 잠금 — PIN (4~6자리, SHA-256 해시)
- [ ] 생체 인증 (`BiometricPrompt` — 지문/얼굴)
- [ ] `SecuritySettingsFragment` UI (생체 잠금, 권한 관리, 데이터 암호화)
- [ ] Prompt Injection 방어 (인바운드 메시지 새니타이즈)
- [ ] 대화 자동 삭제 옵션 (N일 후)

### 📱 세션 & 명령어

- [ ] 멀티 세션 (채널·주제별 독립 대화)
- [ ] 슬래시 명령어: `/new`, `/reset`, `/compact`, `/status`, `/think <level>`
- [ ] Room FTS 대화 검색

### 🎛️ 페르소나 설정

- [ ] `PersonaSettingsFragment` — AI 이름, 역할, 시스템 프롬프트 편집
- [ ] Conversation Style 선택 (Friendly / Professional / Humorous / Concise)
- [ ] 채널별 페르소나 설정 (P2)

### 📊 위젯 & 퀵 액세스

- [ ] App Widget (`RemoteViews`) — 빠른 질문 입력 + 응답 표시
- [ ] Quick Settings Tile — 음성 대화 시작

### 🚀 빌드 & 배포

- [ ] ProGuard / R8 난독화 설정 (`proguard-rules.pro`)
- [ ] 서명 설정 (`keystore.properties`)
- [ ] GitHub Actions CI/CD (테스트 → 빌드 → Play Store 업로드)
- [ ] Play Store 스토어 등록 정보 (스크린샷, 설명, 분류)
- [ ] Play Store 내부 테스트 트랙 배포
- [ ] Play Store 프로덕션 출시

### ℹ️ 정보 화면

- [ ] `AboutFragment` — 앱 버전, 업데이트 확인, 이용약관, 개인정보처리방침, 오픈소스 라이선스

---

## 📎 참조 문서

| 문서 | 설명 |
|---|---|
| `docs/01_프로젝트_개요.md` | 목표, 비전, 마일스톤 |
| `docs/02_시스템_아키텍처.md` | 멀티모듈 + MVVM + 온디바이스 AI |
| `docs/03_기술_스택.md` | 전체 의존성 및 Version Catalog |
| `docs/04_기능_요구사항.md` | 핵심 기능 & 상세 명세 |
| `docs/05_데이터베이스_설계.md` | ERD & Room 테이블 정의 |
| `docs/06_AI_프로바이더_설계.md` | Gemini Nano + 클라우드 폴백 설계 |
| `docs/07_채널_연동_설계.md` | Telegram / Discord / Slack 연동 |
| `docs/08_도구_스킬_시스템.md` | Function Calling + 도구/스킬 |
| `docs/09_UI_화면_설계.md` | 화면 구성 & 와이어프레임 |
| `docs/10_개발환경_설정.md` | Android Studio + Gemini Nano 설정 |
