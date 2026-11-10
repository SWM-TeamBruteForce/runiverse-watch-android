# Code Convention

## 기본 원칙

- 포맷팅은 루트 `.editorconfig`를 기준으로 한다.
- `.editorconfig`와 프로젝트 문서에 없는 규칙은 [Kotlin 코딩 컨벤션](https://kotlinlang.org/docs/coding-conventions.html)을 따른다.

## 네이밍

- 클래스·인터페이스·object·enum은 `PascalCase`, 함수·변수·파라미터는 `camelCase`, 상수(`const val`·최상위 불변 값)는 `UPPER_SNAKE_CASE`, 패키지는 밑줄 없는 소문자로 쓴다.
- `@Composable` 함수는 `Unit`을 돌려주면 `PascalCase`, 값을 돌려주면 `camelCase`로 쓴다.
- 이름은 동작·역할이 드러나게 짓고, 축약어·의미 없는 이름(`data`, `info`, `temp`)은 피한다.
- 같은 것은 프로젝트 전체에서 한 이름으로 부른다. 새 이름을 짓기 전에 같은 뜻의 이름을 찾아본다.
- 나는 `me`·`my`, 파티원은 `player`, 사람 사이 거리는 `gap`, 화면 여백은 `_GAP`·`…Gap`으로 부른다.
- 둥근 화면 지름(가로·세로 계산에 함께 쓰는 값)은 `screenSize`, 가로 폭은 `screenWidth`로 부른다.
- 메시지와 화면 부품 이름을 겹치지 않는다(`PlayerPhoto`는 메시지, 사진 부품은 `PlayerAvatar`).
- 폰 메시지는 이벤트·필드 이름을 그대로 쓴다(`WATCH_SYNCED` → `WatchSynced`, `forced`).
- 함수는 Composable 밖에서 거리·시간·페이스 표기 규칙을 구현하면 `format*`, 같은 일을 하는 짝보다 값 부재나 실패를 더 null로 돌려주면 `*OrNull`, 화면 크기나 줄 자리를 그대로 돌려주는 Composable은 `current*`, 사건 콜백은 `on*`, 자릿수 채우기 같은 범용 보조 함수는 동작 이름으로 짓는다.
- 상태를 묻는 Boolean은 `is*`·`are*`·`has*`·`can*`, 들어가는지 판단은 `*Fits*`, 동작을 고르는 옵션은 동사(`abbreviateUnits`, `pointsUp`), 성공 여부를 돌려주는 동작 함수는 동작 이름(`pause`, `stop`)으로 짓는다. `current*`보다 이 규칙이 먼저다.
- 진행 상태는 대상을 앞에 둔다(`PHONE_CHECKING`, `DISTANCE_CHECKING`).
- 간격 상수는 서로 다른 둘 사이면 `A_TO_B_GAP`, 같은 것끼리면 `X_GAP`으로 짓는다. 패딩으로 넣어도 두 항목 사이면 `_GAP`이다.
- 여백 상수는 화면 곡선까지 남기는 여유면 `_MARGIN`, 패딩으로 넣으면 `_PADDING`, 그릴 때 안쪽으로 밀면 `_INSET`으로 짓는다.
- 비율 상수는 전체에 대한 몫이면 `_FRACTION`(정수 퍼센트면 `_PERCENT`), 두 값의 비면 `A_TO_B_RATIO`로 짓는다.
- 그 밖의 상수는 투명도 `_ALPHA`, 각도 `_DEGREES`, 경계 미만 판정 `_BELOW`, 개수 `_COUNT`, 위치 번호 `_INDEX`, 최소·최대 `MIN_*`·`MAX_*`, 중심 사이 거리 `_SPACING`, 가장 넓은 값을 재는 예시 `WIDEST_*`로 짓는다.
- `object`·`companion object`에 둔 고정 치수·시간·식별자도 상수 규칙을 따른다. 샘플 모델과 코덱 인스턴스 프로퍼티는 `camelCase`로 쓴다.
- 백킹 프로퍼티 `_state`와 백틱 테스트 이름은 네이밍 규칙의 예외다.

## 화면

- 화면·페이지는 파일 하나씩 둔다. 그 화면만 쓰는 부품은 같은 파일에 `private`로 둔다.
- 부품 파일 이름은 컴포저블 이름과 같다. `Components.kt`·`Utils.kt`는 만들지 않는다.
- 전체 화면은 `*Screen`, 넘기는 장은 `*Page`, 항목을 가로로 늘어놓는 줄과 눌리는 줄은 `*Row`(`ComboEffectRow`, 여러 줄이면 `*Rows`), 누르는 부품은 `*Button`, 자리 계산 결과는 `*Slots`, 미리보기는 `<대상><경우>Preview`, 값 하나를 그리는 덩어리는 내용 이름으로 짓는다.
- 모양 이름은 역할이 다른 부품이 같이 쓰는 치수에만 쓴다(`PILL_HEIGHT`).
- 보이는 문구는 `strings.xml`에 두고, 표기 규칙(거리·격차·심박 구간)은 Composable 밖 함수로 두고 단위 테스트한다.
- 문자열 키는 `<쓰는 곳>_<내용>`으로 짓는다. 쓰는 곳은 `run`·`controls`·`unit`·`update`·`idle`이고, Android 관례인 `app_name`만 예외다.
- 시안의 dp·sp 치수를 그대로 쓰고 px로 환산하지 않는다.
- 화면은 `@Preview`로 맞추고, 마지막은 손목 실물로 확인한다.
- 글자나 배치를 바꾸면 44mm·40mm·192dp에서 글자 1.0·1.06·1.24·1.3배로 확인한다. 겹치거나 잘리면 안 된다. 192dp 1.3배에서 첫 파티원 격차가 세 자리면 `▲`가 링에 조금 겹치는 것만 예외로 둔다.
- 달리는 중 값이 바뀌는 칸과 줄 자리는 나올 수 있는 값 중 가장 넓은 것으로 잰다. 글꼴 폭을 재서 고른다.

## 물리량 단위

- 값을 담는 필드·파라미터·지역 변수에는 단위 접미사 `...Meters` / `...Seconds` / `...Ms` / `...SecondsPerKm` / `...Bpm` / `...Spm` / `...Kcal`을 이름 맨 끝에 붙인다(`EXTRA_DECIMAL_BELOW_METERS`). 단위가 곧 이름인 중간값은 그대로 둔다(`km`, `minutes`, `tenthsOfKm`, `maxBpm`).
- 시간 상수는 `Duration`으로 두고(`_DELAY`, `_TIMEOUT`, `_INTERVAL`), 숫자가 필요할 때만 `_MS`로 짓는다.
- 화면 좌표 Float는 px로 보고 접미사를 붙이지 않는다. 같은 자리에 Dp·`TextUnit` 값이 있을 때만 `Px`를 붙인다(`contentGapPx`, `fontSizePx`).

## 주석

- 코드만 봐서는 알 수 없는 이유만 적는다. 코드가 하는 일을 다시 설명하지 않는다.
- 시안·측정·계산에서 온 숫자는 상수로 두고, 추정이면 추정이라고 적는다. 그림 좌표처럼 그 자리에서만 쓰는 숫자는 예외다. 재지 않은 사실을 주석에 쓰지 않는다.
- 한 줄로 쓰고, 길어도 두 줄을 넘기지 않는다.
- 어미는 `~다`로 맺고 명사형으로 끝내지 않는다.
- `//` 주석 끝에는 마침표를 붙이지 않고, KDoc에는 붙인다.
- 로그 문구는 명사형으로 끝낸다(`폰 메시지 해석 실패`).
- 명세 문서를 절 번호·문서명 모두 인용하지 않는다. 개편되면 낡으므로 자립하는 문장으로 적는다.
- 보류·미완성은 `// TODO: 파티원 목데이터 — 폰 연결을 만들 때 실제 값으로 교체한다`처럼 적는다.

## 테스트

- 새 로직에는 단위 테스트를 작성한다.
- 새 화면에는 Robolectric Compose 테스트를 작성하고, 화면 연결·닫기 흐름은 실제 `MainActivity`를 띄워 테스트한다.
- 버그를 고칠 땐 실패를 재현하는 테스트를 먼저 추가한다. 글꼴·기기에 달린 결함은 실기기 전후 사진으로 대신한다.
- 테스트는 given-when-then으로 쓰고, 구분 주석을 `// given`, `// when`, `// then`으로 통일한다. 실행과 검증이 한 문장이면 `// when & then`으로 합칠 수 있고, 토큰 뒤에 `-> 설명`으로 의도를 덧붙일 수 있다.
- 테스트 이름은 행위가 드러나게 짓는다.
- 내부 구현보다 외부에서 관찰 가능한 결과를 검증한다.
- 불필요한 mock과 과도한 `verify()`를 쓰지 않는다.
- 핵심 조건은 본문에 두고, 나머지 반복 준비는 헬퍼로 뺄 수 있다.
- 테스트는 대상 하나에 파일 하나, 패키지는 main을 따라간다(`phone/PhoneLink.kt` → `phone/PhoneLinkTest.kt`).
- 테스트 더블은 그 파일에 `private`로 두고(`@Config`로 넘기는 앱은 `public`), 두 테스트 이상이 쓰면 같은 패키지에 `Fake*.kt`로 뺀다. 공용 `testutil` 패키지는 만들지 않는다.
