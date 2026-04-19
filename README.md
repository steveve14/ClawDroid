# 🐾 ClawDroid

**Android 네이티브 개인 AI 어시스턴트**

온디바이스 AI(Gemini Nano)를 기본으로, 서버 없이 독립 실행되는 Android AI 채팅 앱입니다.  
[OpenClaw](https://github.com/openclaw/openclaw)에서 영감을 받아 Android 네이티브로 재설계했습니다.

---

## ✨ 주요 특징

- **온디바이스 AI 기본** — Gemini Nano(AICore)로 완전 오프라인 AI 대화
- **멀티 모델 지원** — Gemini Nano / Gemini Cloud / OpenAI / Ollama / 커스텀 엔드포인트
- **멀티 채널 연동** — Telegram, Discord, Slack 동시 응답
- **음성 대화** — Wake Word("Hey Claw"), STT, TTS 핸즈프리
- **도구·스킬 시스템** — 브라우저, 캘린더, 카메라 등 기기 기능을 AI가 직접 활용
- **프라이버시 우선** — 온디바이스 처리 우선, 외부 전송 최소화

## 🏗️ 아키텍처

**MVVM + Clean Architecture** 기반 멀티모듈 구조

```
ClawDroid/
├── app/                    # 메인 앱 (Application, DI, Navigation)
├── core/
│   ├── model/              # 도메인 모델 (순수 Java POJO)
│   ├── data/               # 데이터 레이어 (Room, Repository, Network)
│   ├── ai/                 # AI 프로바이더 추상화 + 구현
│   └── ui/                 # 공통 UI 컴포넌트
├── feature/
│   ├── chat/               # 채팅 기능
│   ├── voice/              # 음성 대화
│   ├── channels/           # 멀티채널 연동 (Telegram/Discord/Slack)
│   ├── tools/              # 도구·스킬 시스템
│   └── settings/           # 설정
└── gradle/
    └── libs.versions.toml  # Version Catalog
```

## 🛠️ 기술 스택

| 항목 | 기술 |
|---|---|
| 언어 | Java 17 |
| UI | XML Layouts + ViewBinding, Material 3 |
| 아키텍처 | MVVM + Clean Architecture |
| DI | Hilt (Dagger) |
| 비동기 | RxJava 3 |
| DB | Room (SQLite) |
| 네트워크 | Retrofit 2.11 + OkHttp 4.12 |
| 온디바이스 AI | Gemini Nano (AICore), ML Kit GenAI |
| 클라우드 AI | Gemini Cloud, OpenAI, Ollama |
| 보안 | EncryptedSharedPreferences, Android Keystore, BiometricPrompt |
| 빌드 | Gradle 8.x + Groovy DSL, AGP 8.8.0 |
| SDK | compileSdk 35, minSdk 26, targetSdk 35 |

## 📋 개발 로드맵

| Phase | 내용 |
|---|---|
| **Phase 1** | MVP AI 채팅 — 스트리밍 응답, Markdown 렌더링, 대화 관리, AI 프로바이더 통합 |
| **Phase 2** | 음성 대화 — STT, TTS, Talk Mode UI, Wake Word |
| **Phase 3** | 멀티채널 — Telegram/Discord/Slack 봇 연동 |
| **Phase 4** | 도구·스킬 — Function Calling, 내장 도구, SKILL.md 기반 스킬 시스템 |
| **Phase 5** | 고급 기능 — 멀티 세션, 보안(PIN/생체인증), App Widget, CI/CD |

## 🚀 시작하기

### 요구사항

- **Android Studio** Ladybug (2024.2+)
- **JDK** 17
- **Android SDK** API 35
- **Git** 2.40+
- Gemini Nano 테스트 시 **실제 기기** 필요 (Pixel 8 Pro+, Galaxy S24+)

### 설치

```bash
git clone https://github.com/your-username/ClawDroid.git
cd ClawDroid
```

### API 키 설정

`local.properties`에 아래 키를 추가합니다 (Git에 커밋되지 않음):

```properties
GEMINI_API_KEY=your_gemini_api_key
OPENAI_API_KEY=your_openai_api_key       # 선택
TELEGRAM_BOT_TOKEN=your_telegram_token   # 선택
DISCORD_BOT_TOKEN=your_discord_token     # 선택
```

- **GEMINI_API_KEY**: [Google AI Studio](https://aistudio.google.com/)에서 발급
- **OPENAI_API_KEY**: [OpenAI Platform](https://platform.openai.com/)에서 발급

### 빌드 및 실행

Android Studio에서 프로젝트를 열고 **Run** 버튼을 클릭하거나:

```bash
./gradlew assembleDebug
```

## 🆚 OpenClaw과의 차이

| | OpenClaw | ClawDroid |
|---|---|---|
| 플랫폼 | Node.js Gateway 서버 | **Android 네이티브 (Java)** |
| AI 모델 | 클라우드 중심 | **Gemini Nano 온디바이스** + 클라우드 폴백 |
| 서버 | Gateway 필수 | **독립 실행** (서버 불필요) |
| 오프라인 | 불가 | **완전 오프라인 지원** |
| 프라이버시 | 서버 경유 | **기기 내 처리 우선** |
| 도구 실행 | 서버에서 실행 | **기기에서 직접 실행** |

## 📚 문서

상세 설계 문서는 [`docs/`](docs/) 디렉토리를 참고하세요.

## 📄 라이선스

TBD
