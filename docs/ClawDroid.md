---
tags:
  - dashboard
  - index
cssclass: dashboard
---

# 🦞 ClawDroid — 개인 AI 어시스턴트

> **프로젝트 유형**: 안드로이드 네이티브 앱 (OpenClaw 영감)
> **최종 갱신**: 2026-04

---

## 📌 빠른 탐색

| 카테고리 | 링크 | 설명 |
|---|---|---|
| 🏠 개요 | [[01_프로젝트_개요]] | 목표, 비전, 마일스톤 |
| 🏗️ 아키텍처 | [[02_시스템_아키텍처]] | 멀티모듈 + MVVM + 온디바이스 AI |
| 🛠️ 기술 스택 | [[03_기술_스택]] | Java / XML Views / Gemini Nano / 멀티모델 |
| 📋 기능 요구사항 | [[04_기능_요구사항]] | 핵심 기능 & 상세 명세 |
| 🗃️ 데이터베이스 | [[05_데이터베이스_설계]] | ERD & 테이블 정의 (Room) |
| 🤖 AI 프로바이더 | [[06_AI_프로바이더_설계]] | Gemini Nano + 클라우드 폴백 + 멀티모델 |
| 🔗 채널 연동 | [[07_채널_연동_설계]] | Telegram / Discord / Slack / Gateway |
| 🧰 도구·스킬 | [[08_도구_스킬_시스템]] | Function Calling + 내장/커스텀 도구 |
| 🖥️ 화면 설계 | [[09_UI_화면_설계]] | 화면 구성 & 와이어프레임 |
| 💻 개발 환경 | [[10_개발환경_설정]] | Android Studio + Gemini Nano 설정 |

---

## 🚦 현재 진행 상태

### Phase 1 — MVP: AI 채팅 (코어)

- [x] 프로젝트 설계 문서 작성
- [x] 멀티모듈 프로젝트 스캐폴딩
- [x] AI 프로바이더 추상화 레이어 구현
- [x] Room DB 초기 스키마
- [x] Gemini Nano 온디바이스 통합
- [x] 클라우드 AI 폴백 (Gemini Cloud, OpenAI)
- [x] 채팅 UI (스트리밍, Markdown 렌더링)

### Phase 2 — 음성 대화

- [x] STT: ML Kit 음성 인식 + SpeechRecognizer 폴백
- [x] TTS: Android TTS + ElevenLabs 선택적 지원
- [x] Talk Mode UI (웨이브폼, 오버레이)
- [x] Wake Word 감지

### Phase 3 — 멀티채널 연동

- [x] 채널 프레임워크 (`Channel` 인터페이스)
- [x] Telegram Bot 연동
- [x] Discord Bot 연동
- [x] Slack Bot 연동
- [x] Gateway 서버 모드

### Phase 4 — 도구·스킬 시스템

- [x] 도구 프레임워크 (`Tool` 인터페이스)
- [x] Function Calling 통합
- [x] 내장 도구 (browser, calendar, contacts 등)
- [x] 스킬 시스템 (SKILL.md 기반)

### Phase 5 — 고급 기능 & 배포

- [ ] 멀티 세션 + 명령어 시스템
- [ ] 보안 (PIN, 생체인증, prompt injection 방어)
- [ ] App Widget (RemoteViews)
- [ ] GitHub Actions CI/CD
- [ ] Play Store 배포

---

## 🆚 OpenClaw과의 차이점

| 항목 | OpenClaw | ClawDroid |
|---|---|---|
| **플랫폼** | Node.js Gateway 중심, 앱은 노드 | **Android 네이티브 퍼스트** |
| **AI 모델** | 클라우드 모델 중심 | **Gemini Nano 온디바이스 기본** + 클라우드 폴백 |
| **온디바이스** | 제한적 | 오프라인 완전 동작 지원 |
| **채널** | 20+ 채널 지원 | 주요 채널 우선 (Telegram, Discord, Slack) |
| **프라이버시** | 서버 거쳐 처리 | 기기 내 처리 우선, 데이터 외부 전송 최소화 |
| **서버 의존성** | Gateway 필수 | 독립 모드 + Gateway 모드 선택 |

---

## 🔗 관련 프로젝트 & 참고 자료

- [[AI/Gemini/Gemini|Gemini]] — Gemini 모델 종합 가이드
- [[AI/Gemini/Gemini_API|Gemini API]] — API 사용법
- [OpenClaw GitHub](https://github.com/openclaw/openclaw) — 원본 프로젝트
- [OpenClaw Docs](https://docs.openclaw.ai/) — 공식 문서

### 스택: #프로젝트 #AI어시스턴트 #안드로이드 #GeminiNano #OpenClaw
