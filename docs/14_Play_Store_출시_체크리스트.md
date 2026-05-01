# ClawDroid Play Store 출시 체크리스트

> 작성일: 2026-05-01  
> 기준: 현재 `app/` 단일 모듈 구현 및 `.github/workflows/android.yml`

---

## 1. 현재 준비 상태

| 항목 | 상태 | 비고 |
|---|---|---|
| Release AAB 빌드 | 완료 | GitHub Actions `release` job에서 `bundleRelease` 수행 |
| 앱 서명 설정 | 완료 | `keystore.properties` 기반 로컬 서명, CI는 keystore secret 필요 |
| 내부 테스트 트랙 업로드 자동화 | 구성 완료 | `PLAY_SERVICE_ACCOUNT` secret이 있을 때 internal track draft 업로드 |
| 스토어 등록 정보 | 초안 필요 | 아래 등록 정보 초안을 Play Console에 입력 |
| 내부 테스트 실제 배포 | 외부 작업 | Play Console 계정, 서비스 계정 권한, 테스트 사용자 목록 필요 |
| 프로덕션 출시 | 외부 작업 | 심사, 정책 승인, 데이터 안전 선언 완료 필요 |

---

## 2. 스토어 등록 정보 초안

### 앱 이름

ClawDroid

### 한 줄 설명

온디바이스 AI와 멀티채널 연동을 지원하는 개인 Android AI 어시스턴트

### 간단한 설명

ClawDroid는 Gemini Nano 온디바이스 실행, 클라우드 AI 폴백, 음성 대화, Telegram/Discord/Slack 연동, 도구 실행을 하나로 묶은 개인 AI 어시스턴트입니다.

### 자세한 설명

ClawDroid는 Android 네이티브 환경에서 동작하는 개인 AI 어시스턴트입니다. Gemini Nano 기반 온디바이스 응답을 우선 사용하고, 필요할 때 Gemini Cloud, OpenAI, Ollama, OpenAI 호환 커스텀 엔드포인트로 확장할 수 있습니다.

주요 기능:

- 실시간 스트리밍 AI 채팅과 Markdown 렌더링
- 음성 입력, TTS, Wake Word 기반 Talk Mode
- Telegram, Discord, Slack, Gateway 채널 연동
- 브라우저, 계산기, 캘린더, 연락처, 위치, 알람, 파일 도구
- 앱 잠금, 생체 인증, SQLCipher 데이터베이스 암호화
- 빠른 질문 App Widget과 Quick Settings Tile

ClawDroid는 개인 데이터 보호를 위해 API 키를 암호화 저장하고, 대화 데이터베이스를 SQLCipher로 보호하며, 외부 채널 메시지에는 Prompt Injection 방어 지침을 적용합니다.

### 카테고리

Productivity

### 태그 후보

AI assistant, productivity, voice assistant, automation, on-device AI

---

## 3. 스크린샷 준비 순서

1. 대화 목록 화면: 고정/최근 섹션과 검색 UI가 보이는 상태
2. 채팅 화면: 스트리밍 응답, Markdown 코드 블록, 모델 전환 버튼이 보이는 상태
3. 음성 대화 화면: PTT 버튼과 웨이브폼이 활성화된 상태
4. 채널 목록 화면: Telegram/Discord/Slack 연결 상태가 보이는 상태
5. 도구·스킬 설정 화면: 도구 토글과 스킬 목록이 보이는 상태
6. 보안 설정 화면: 앱 잠금, 생체 인증, 자동 삭제 옵션이 보이는 상태

---

## 4. Play Console 필수 입력

| 영역 | 필요 값 |
|---|---|
| 개인정보처리방침 URL | 공개 웹 URL 필요 |
| 데이터 안전 선언 | 수집/공유/암호화/삭제 요청 항목 입력 |
| 콘텐츠 등급 | AI 채팅 및 사용자 생성 콘텐츠 기준 설문 완료 |
| 앱 액세스 | 로그인 또는 API 키 입력이 필요한 기능 설명 |
| 고위험 권한 소명 | `SYSTEM_ALERT_WINDOW`, 마이크, 위치, 연락처, 캘린더 사용 목적 |
| 타깃 사용자 | 일반 사용자, 아동 대상 아님으로 설정 권장 |

---

## 5. 내부 테스트 트랙 배포 순서

1. Play Console에서 `com.clawdroid.app` 앱 생성
2. Play App Signing 활성화
3. Google Cloud 서비스 계정 생성 후 Play Console에 Release Manager 권한 부여
4. GitHub repository secret 추가
   - `PLAY_SERVICE_ACCOUNT`: 서비스 계정 JSON 전체
   - `KEYSTORE_BASE64`: release keystore base64
   - `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
5. `main` 브랜치에 push해 `release` job 실행
6. Play Console internal track에 draft 릴리스가 생성됐는지 확인
7. 테스트 사용자 목록을 추가하고 릴리스 검토 후 게시

---

## 6. 프로덕션 출시 전 게이트

- 보안 리포트의 남은 Medium 이슈 검토 완료
- 핵심 플로우 수동 QA 완료: 채팅, 음성, 채널, 도구, 앱 잠금, 위젯
- Crash 모니터링 또는 릴리스 후 이슈 수집 채널 준비
- 개인정보처리방침과 Data Safety 선언 검토 완료
- 내부 테스트에서 크래시 없는 세션 확보
