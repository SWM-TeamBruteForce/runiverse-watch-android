# Runiverse Watch Android

> 멀리 떨어져 있어도 함께 달리는 원격 동반 러닝 플랫폼, **Runiverse**의 갤럭시워치(Wear OS) 앱입니다.

폰 앱은 [runiverse-frontend](https://github.com/SWM-TeamBruteForce/runiverse-frontend), 서버는 [runiverse-backend](https://github.com/SWM-TeamBruteForce/runiverse-backend)입니다.
애플워치 앱은 [runiverse-watch-ios](https://github.com/SWM-TeamBruteForce/runiverse-watch-ios)입니다.

## 기술 스택

| 분류 | 사용 기술 |
|---|---|
| Language | Kotlin (Wear OS) |
| 폰 연동 | Wearable Data Layer API |
| 배포 | Google Play Wear OS 트랙 (폰 앱과 같은 등록 정보) |

## 구성

폰 앱과 따로 빌드하고 따로 설치하는 별개 앱입니다. 갤럭시워치 말고도 Wear OS 워치(픽셀워치 등)에서 돌아갑니다.

- 폰 앱과 데이터를 주고받으려면 **패키지명(`com.runiverse.app`)과 설치된 앱의 서명**이 같아야 합니다.
- 워치만 고치면 워치 앱만 Wear OS 트랙에 올립니다. 폰 앱 버전은 그대로입니다.
- `versionCode`는 폰 앱과 겹치면 안 됩니다. 범위를 정하면 여기에 적습니다.

## 개발

- **폰과 같이 볼 때**: 폰 앱과 워치 앱을 **둘 다 로컬에서** 빌드해 설치합니다. 같은 맥의 debug 키로 서명돼야 연결됩니다.
  Play에서 받은 폰 앱과 로컬 debug 워치 앱은 서명이 달라 연결되지 않습니다.
- **출시 전 검증**: 둘 다 Play 테스트 트랙에서 받아 확인합니다.

## 상태

준비 중입니다. 범위(폰 연동형 / 단독형)는 아직 정하지 않았습니다.
