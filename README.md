# Runiverse Watch Android

> 멀리 떨어져 있어도 함께 달리는 원격 동반 러닝 플랫폼, **Runiverse**의 갤럭시워치(Wear OS) 앱입니다.

폰 앱과 붙어서 러닝 중 화면과 끝난 직후 결과 요약을 보여 줍니다. 기록과 판단은 폰이 하고, 워치는 보여 주기와 조작만 합니다.
심박은 워치가 재서 워치에만 보여 주고, 폰에 보내거나 저장하지 않습니다. 갤럭시워치 말고도 Wear OS 워치(픽셀워치 등)에서 돌아갑니다.

폰 앱은 [runiverse-frontend](https://github.com/SWM-TeamBruteForce/runiverse-frontend), 서버는 [runiverse-backend](https://github.com/SWM-TeamBruteForce/runiverse-backend),
애플워치 앱은 [runiverse-watch-ios](https://github.com/SWM-TeamBruteForce/runiverse-watch-ios)입니다.

## 기술 스택

| 분류 | 사용 기술 |
|---|---|
| Language | Kotlin 2.4 |
| UI | Compose for Wear OS (Material 3) |
| 폰 연동 | Wearable Data Layer API |
| Sensor | Health Services (심박), Ongoing Activity |
| Serialization | kotlinx.serialization |
| Build | AGP 9.3, minSdk 30 · targetSdk 36 |
| Test | JUnit 4, Robolectric, Compose UI Test |
| CI/CD | GitHub Actions → Play Wear OS 내부 테스트 |

## 프로젝트 구조

```text
runiverse-watch-android
├── app/src/main/java/com/runiverse/app
│   ├── phone       # 폰과 메시지 주고받기 · 연결 상태
│   ├── exercise    # 러닝 중 앱을 살려 두는 서비스 · 심박 측정
│   ├── run         # 러닝 화면 · 장 · 결과 요약
│   └── ui          # 여러 기능이 같이 쓰는 색 · 글자 · 부품
└── docs            # 컨벤션 · 폰 통신 문서
```

## 시작하기

```bash
./gradlew :app:assembleDevDebug       # 빌드
./gradlew :app:lintDevDebug           # lint
./gradlew :app:testDevDebugUnitTest   # 단위 테스트
./gradlew :app:installDevDebug        # 연결된 워치에 설치
```

flavor는 폰 앱과 같습니다. `dev`는 `com.runiverse.app.dev`, `prod`는 `com.runiverse.app`입니다.

> **폰 앱과 데이터를 주고받으려면 applicationId와 서명 인증서가 같아야 합니다.**
> 로컬에서 볼 때는 두 앱을 같은 flavor의 debug로 빌드해 같은 debug 키로 서명합니다.
> Play에서 받을 때는 둘 다 Play 테스트 트랙에서 받습니다. Play 폰 앱과 로컬 debug 워치 앱은 서명이 달라 통신하지 않습니다.

## 브랜치 전략 · 배포

| 브랜치 | 역할 | 동작 |
|---|---|---|
| `<type>/<description>` | 기능 개발 | `dev`로 PR을 열어 리뷰 후 머지 |
| `dev` | 통합 | Actions에서 **Dev Release**를 돌리면 lint·테스트 후 dev 앱을 Play Wear OS 내부 테스트에 올린다 |
| `main` | 운영 | `dev`에서 온 PR만 허용. 머지되면 lint·테스트 후 운영 앱을 Play Wear OS 내부 테스트에 올린다(**Prod Release**) |

- 워치 앱은 폰 앱과 같은 Play 등록 정보의 Wear OS 트랙에 올라갑니다. 폰 쪽 변경이 없는 워치 수정은 워치 앱만 올리고, 폰 앱 버전은 그대로입니다.
- `versionCode`는 폰 앱과 겹치지 않고, 각 기기의 이전 배포본보다 커야 합니다. 워치는 배포 workflow가 `10000 + 실행 번호`로 붙입니다(로컬 빌드는 1).
- 서명 키와 Play 업로드 계정은 GitHub 환경 `dev`·`prod`의 Secret에 있습니다. 이름은 폰 앱 저장소와 같습니다.
- Play 프로덕션 승급은 사람이 합니다.

## 문서

| 문서 | 내용 |
|---|---|
| [code-convention.md](docs/code-convention.md) | 네이밍 · 화면 · 물리량 단위 · 주석 · 테스트 |
| [git-convention.md](docs/git-convention.md) | 커밋 · 브랜치 · PR |
| [phone-communication.md](docs/phone-communication.md) | 폰↔워치 메시지 · 상태 모양 · 보내고 받는 규칙 |

## 팀원

| <img src="https://github.com/jihwanjo-98.png" width="120"> | <img src="https://github.com/KimDwDev.png" width="120"> | <img src="https://github.com/zxc88kr.png" width="120"> |
|:---:|:---:|:---:|
| **조지환** | **김동완** | **박찬** |
| [@jihwanjo-98](https://github.com/jihwanjo-98) | [@KimDwDev](https://github.com/KimDwDev) | [@zxc88kr](https://github.com/zxc88kr) |
| AI / Frontend | Backend | Backend |
