# CLAUDE.md

Runiverse 갤럭시워치(Wear OS) 앱. 폰 앱(runiverse-frontend)과 Data Layer로 붙어, 폰이 기록·판단하고 워치는 표시·입력만 한다.

## 빌드

```bash
./gradlew :app:assembleDevDebug    # 빌드
./gradlew :app:lintDevDebug        # lint
./gradlew :app:installDevDebug     # 연결된 워치에 설치
```

## 문서 인덱스 — 구현 전 해당 문서를 읽을 것

- `docs/code-convention.md` — 네이밍·화면·주석·테스트 규칙
- `docs/git-convention.md` — 커밋·브랜치·PR 규칙
- `docs/phone-communication.md` — 폰↔워치 메시지·번호·상태 모양·보내고 받는 규칙

## 구조

- 모듈은 `app` 하나, 코드는 `app/src/main/java/com/runiverse/app/`. 패키지는 아래 자리에 두고, 기능 패키지를 새로 만들면 여기에 적는다.
  ```
  RuniverseApp   앱에 하나뿐인 것(폰 연결·운동 세션)을 직접 만든다
  MainActivity   화면을 열 때 SYNC, 켜짐·꺼짐을 알린다. 러닝이 보이면 운동 서비스를 시작한다
  IdleScreen     러닝이 없을 때 보이는 대기 화면
  phone/         폰과 메시지 주고받기·연결 상태
  exercise/      러닝 중 앱을 살려 두는 서비스·심박 측정
  run/           러닝 중 화면(첫 화면·파티원 장·내 기록 장·조작 장)과 표기 규칙
  ui/theme/      여러 기능이 같이 쓰는 색·글자. 화면은 MaterialTheme으로 읽는다
  ui/component/  두 기능 이상이 쓰는 부품
  ```
- 화면은 폰 상태로 `when` 분기한다. 폰 상태는 맨 위 화면 한 곳에서만 읽고, 아래로는 값과 동작 함수만 넘긴다.
- 라이브러리·플러그인 버전은 `gradle/libs.versions.toml`에만 적는다.
- flavor는 폰 앱과 같다 — `dev`(`com.runiverse.app.dev`)·`prod`(`com.runiverse.app`). 폰 dev 빌드와 붙여 볼 땐 워치도 `dev`.

## 범위

- 1차는 폰 경유 러닝 화면과 끝난 직후 결과 요약뿐이다. 워치가 서버를 직접 부르는 기능(매칭·기록 보기)은 넣지 않는다.
- 심박은 워치가 재서 워치에만 보여 준다. 폰에 보내지도 저장하지도 않는다.
- 러닝 중 화면은 스와이프로 닫히지 않는다. 종료는 종료 버튼으로만 한다. 폰 소식이 10분 넘게 없을 때만 앱이 스스로 닫힌다(화면이 꺼져 있으면 더 늦어질 수 있다).

## 주의사항

- 폰과 통신하려면 applicationId와 서명이 폰 앱과 같아야 한다. Play에서 받은 폰 앱과 로컬 빌드 워치 앱은 서명이 달라 안 붙는다.
- 러닝 중엔 `ExerciseService`(포그라운드 서비스)가 앱을 살려 둔다. 심박을 쓰는 서비스라 화면이 보일 때만 시작할 수 있다.
- 화면이 꺼지면 CPU가 잠들어 `delay`가 밀린다. 꺼진 동안의 시간은 `elapsedRealtime`으로 잰다.
- 키스토어·`key.properties`는 커밋하지 않는다.
- 요청 없이 라이브러리를 추가하지 않는다.
- 문제를 우회하는 해결 금지 — 테스트 약화·빈 catch·기능 삭제로 오류를 없애지 않는다.
