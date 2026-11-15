# 폰 ↔ 워치 통신

폰이 러닝을 기록하고 판단한다. 워치는 받은 값을 보여 주고 누른 것을 폰에 넘길 뿐, 서버와 직접 통신하지 않는다.

## 통로

- Wear OS `MessageClient`, 경로 `/running` 하나. 본문은 UTF-8 JSON `{"event": ..., "data": {...}}` — 서버 WebSocket과 같은 봉투다.
- 폰이 보낼 땐 capability `runiverse_running`을 선언한 워치 앱이 있는 기기 중 `isNearby`인 것, 워치가 보낼 땐 `NodeClient.connectedNodes` 중 `isNearby`인 것. 폰 앱에 capability 선언은 두지 않는다.
- 연결돼 있을 때만 보낸다. 끊긴 동안 쌓아 두지 않고, 다시 연결되면 `WATCH_SYNC`로 맞춘다.
- 모르는 `event`·필드는 무시한다. enum 값을 늘릴 땐 양쪽을 함께 바꾼다 — 모르는 값이 든 메시지는 통째로 버려진다.

## 메시지

보내는 쪽이 워치면 현재형, 폰이면 과거형이다. 명령에 따로 답하지 않고 바뀐 상태로 알린다(서버도 일시정지·재개엔 답이 없다).

| event | 방향 | data | 언제 |
|---|---|---|---|
| `WATCH_SYNC` | 워치 → 폰 | `syncId`·`watchVersion` | 앱을 열 때, 다시 연결할 때, 모르는 번호를 받았을 때, 러닝 중 화면이 꺼져 있고 소식이 1분 넘게 없을 때 |
| `WATCH_SYNCED` | 폰 → 워치 | 상태 전체 + `syncId`·`birthday`·`photos` | `WATCH_SYNC`를 받으면 바로 |
| `WATCH_STATE_UPDATED` | 폰 → 워치 | 상태 전체 | 아래 「보내는 규칙」 |
| `WATCH_PAUSE` | 워치 → 폰 | 명령 공통 | 일시정지 |
| `WATCH_RESUME` | 워치 → 폰 | 명령 공통 | 계속 달리기 |
| `WATCH_FINISH` | 워치 → 폰 | 명령 공통 + `forced` | 종료. `forced`는 폰 종료 버튼과 같은 규칙. 보류 중이면 폰이 `true`로, 복구 중이라 거리를 모르면 `false`로 보낸다 |
| `WATCH_COMBO_TRIGGERED` | 폰 → 워치 | `eventId`·`userId`·`comboCount`·`kind` | 콤보가 오르거나 끊긴 순간 |
| `WATCH_VISIBILITY_UPDATE` | 워치 → 폰 | `isVisible` | 화면이 켜지거나 꺼질 때, 켜진 동안 10초마다 |
| `ERROR` | 폰 → 워치 | `code`·`message`·`sourceType`·`requestId` | 명령이나 `WATCH_SYNC`를 거절할 때만 |

명령 공통 data는 `requestId`·`phoneInstanceId`·`runningRoomId`·`statusVersion`(누를 때 워치가 본 값)이다. `runningRoomId`가 `null`인 상태에서 누르면 `null` 그대로 보낸다.

`WATCH_SYNC`의 `watchVersion`은 워치 앱 버전 이름(`x.y.z`)이다.

## 번호

| 이름 | 누가 | 뜻 |
|---|---|---|
| `phoneInstanceId` | 폰 | 폰 앱이 켜질 때마다 새 값. 워치는 바뀐 걸 보면 SYNC, 폰은 다른 값이 실린 명령을 거절 |
| `stateSequence` | 폰 | 상태를 보낼 때마다 +1(SYNCED 포함). 워치는 지난 번호를 버린다. 서버 `sequence`(좌표 순번)와 다르다 |
| `statusVersion` | 폰 | 일시정지·재개·종료 단계가 바뀔 때만 +1. 명령에 실린 값과 다르면 거절 |
| `lastRequestId` | 폰 | 마지막으로 처리한 워치 명령. 워치는 이걸로 명령 완료를 판정한다 |
| `requestId` | 워치 | 명령마다 새 값. 폰은 같은 값이 두 번 와도 한 번만 처리한다 |
| `syncId` | 워치 | SYNC마다 새 값. SYNCED가 그대로 돌려준다 |
| `eventId` | 폰 | 콤보 순간마다 새 값. 워치는 본 값을 다시 받으면 버린다 |

## 상태

`WATCH_STATE_UPDATED`의 data다.

```json
{
  "phoneInstanceId": "3f2a9c1e",
  "stateSequence": 1532,
  "statusVersion": 4,
  "lastRequestId": "b71d0e42",
  "run": {
    "runningRoomId": 812,
    "type": "MATCH",
    "status": "RUNNING",
    "targetDistanceMeters": 5000,
    "isServerConnected": true,
    "isPenaltyApplicable": true,
    "me": {
      "distanceMeters": 3421,
      "elapsedSeconds": 1450,
      "currentPaceSecondsPerKm": 332,
      "caloriesKcal": 210,
      "cadenceSpm": 172
    },
    "players": [
      {
        "userId": "5b1f…",
        "nickname": "콩나물",
        "colorSlot": 1,
        "distanceMeters": 3541,
        "currentPaceSecondsPerKm": 318,
        "status": "RUNNING",
        "gapMeters": 120,
        "comboCount": 12,
        "maxComboCount": 20,
        "isStale": false
      }
    ],
    "finish": null,
    "result": null
  }
}
```

| 필드 | 뜻 |
|---|---|
| `run` | 진행 중인 러닝. 없으면 `null` |
| `runningRoomId` | 방 번호. 방이 아직 안 세워졌으면 `null`(출발 직후, 오프라인 솔로). 세워지면 폰이 바로 다시 보낸다 |
| `type` | `MATCH`·`SOLO`·`INVITE` |
| `status` | 내 상태 `RUNNING`·`PAUSED`·`FINISHED`. 파티원 `status`엔 `DISCONNECTED`도 온다 |
| `targetDistanceMeters` | 목표 거리. 목표 없는 솔로는 `null` |
| `isServerConnected` | 폰↔서버 연결. `false`면 워치는 파티원만 흐리게 하고 `서버 연결 확인 중` |
| `isPenaltyApplicable` | 못 채우면 매칭 제한이 걸리는 방 |
| `me` | 폰 화면의 내 값. `elapsedSeconds`는 폰 러닝 화면 시간으로 버튼 일시정지만 뺀다. `currentPaceSecondsPerKm`은 폰처럼 30초 기준, 위치가 안 잡히면 `null`. `caloriesKcal`·`cadenceSpm`은 아직 없으면 `null` |
| `players` | **나를 뺀** 참가자. 서버 `players`는 나를 포함하지만, 워치는 내 값을 `me`에서만 읽게 하려고 뺐다. 폰 파티원 화면처럼 통지가 아직 없으면 `distanceMeters` 0·`status` `RUNNING`, 이름을 모르면 `nickname`은 `함께 달리는 사람`이다 |
| `colorSlot` | 파티원 색 번호 1~3(0은 나). 폰 `PartyBoard.colorSlotOf`를 폰 팔레트처럼 4로 나눈 나머지라 0~3만 온다. 러닝 중엔 바뀌지 않는다 |
| `gapMeters` | 상대 − 나. 양수면 상대가 앞. 모르면 `null` |
| `comboCount`·`maxComboCount` | 그 사람과 나 사이의 지금 콤보·최고 콤보. 콤보가 아니면 `comboCount` 0 |
| `isStale` | 그 사람 통지가 19초 넘게 안 왔다. 폰 파티원 화면이 카드를 흐리게 그리는 기준과 같다(통지가 한 번도 없으면 `false`) |
| `finish` | 종료 단계. `{"stage": "FINISHING"}`(서버 응답 대기) 또는 `{"stage": "GOAL_PENDING", "remainingMeters": 420}`(거리 모자람). 아니면 `null`. `GOAL_PENDING`은 폰에서 `계속 달리기`를 고르거나 다시 종료하거나 서버가 끝낼 때까지 남는다 |
| `result` | 끝난 결과. `{"isServerConfirmed", "totalDistanceMeters", "averagePaceSecondsPerKm", "elapsedSeconds"}` — 숫자는 폰 요약과 같다. `elapsedSeconds`는 움직인 시간이라 버튼 없이 10초 넘게 선 구간도 빠져, 러닝 중 마지막 `me.elapsedSeconds`보다 작을 수 있다. `isServerConfirmed`가 `true`면 서버 기록, `false`면 폰이 잰 값(방이 없었거나, 종료 확인을 못 받았거나, 조회가 실패했거나, 내 기록이 안 남음). `status`가 `FINISHED`인데 `null`이면 서버 기록을 받는 중이다 |

`WATCH_SYNCED`에만 있는 것:

| 필드 | 뜻 |
|---|---|
| `birthday` | 프로필 API 값 그대로(`yyyy-MM-dd`), 없으면 `null`. 워치가 심박 구간을 계산한다 |
| `photos` | `[{"userId", "jpegBase64"}]` — 파티원 사진을 64×64 JPEG로 줄인 것. 사진이 없는 사람은 뺀다. 워치는 256px를 넘거나 못 푼 사진을 첫 글자로 그린다 |

`WATCH_COMBO_TRIGGERED`의 `kind`는 `TICK`(1 오름)·`MILESTONE`(1·3·5·10·20… 도달)·`BREAK`(끊김)다.

## 보내는 규칙 — 폰

- `WATCH_STATE_UPDATED`는 워치 화면이 켜져 있고 러닝이 끝나기 전이면 보낸다 — 달리는 중엔 폰 러닝 시간(초)이 바뀔 때마다, 일시정지 중엔 1초마다. 앞 전송이 안 끝났으면 끝나는 대로 최신 하나를 보낸다.
- 일시정지·재개·종료 단계·결과 확정은 화면이 꺼져 있어도 바로 보낸다.
- 러닝이 사라지면(폰에서 요약을 닫음·서버 종료·강퇴) 화면이 꺼져 있어도 바로 `run: null`을 보낸다 — 워치 결과 화면도 같이 닫힌다.
- 워치 명령을 반영하면 1초를 기다리지 않고 바로 보낸다(`statusVersion` +1, `lastRequestId`).
- `WATCH_COMBO_TRIGGERED`는 화면이 꺼져 있어도 보낸다 — 갤럭시워치는 꺼진 화면에서도 진동한다.
- 워치 화면이 켜져 있으면 `WATCH_COMBO_TRIGGERED`(여럿이면 전부) 바로 뒤에 `WATCH_STATE_UPDATED`를 한 번 보낸다 — 워치는 파티원 자리의 콤보 숫자·고리를 상태 값으로 그린다.
- `WATCH_VISIBILITY_UPDATE`(`isVisible: true`)를 받으면 러닝이 끝났어도 지금 상태를 바로 한 번 보낸다 — 화면이 꺼진 사이 놓친 결과 확정을 되찾는다.
- `WATCH_VISIBILITY_UPDATE`가 15초 없으면 화면이 꺼진 것으로 보고 첫 줄의 전송을 멈춘다.
- `WATCH_SYNC`는 서버에서 받아 둔 워치 최소 버전으로 바로 판정한다. `watchVersion`이 최소 버전보다 낮거나, 없거나, `x.y.z`가 아니면 `WATCH_SYNCED` 대신 `ERROR`로 거절하고, `requestId`에는 `syncId`를 넣는다. 명령·`WATCH_VISIBILITY_UPDATE`는 버전을 보지 않는다.
- 최소 버전을 아직 모르면 거절도 답도 하지 않고 서버에 받으러 간다. 워치는 다음 SYNC에서 판정받는다.
- 러닝 중 한 번 통과한 워치는 그 러닝이 끝날 때까지 다시 막지 않는다. 통과하지 않은 워치는 러닝 중이라도 SYNC가 거절되면 러닝을 접는다.
- 명령은 러닝이 없을 때, 러닝을 시작한 계정이 아닐 때(로그아웃·계정 전환), `phoneInstanceId`·`statusVersion`·방이 다를 때만 거절(`ERROR`)한다. 명령의 `runningRoomId`가 `null`이면 방 번호 비교만 생략한다. 워치 명령은 폰 버튼과 같은 길로 처리한다.
- `ERROR`의 `code`는 명령 거절에 `NO_RUN`·`NOT_RUN_OWNER`·`OTHER_PHONE_INSTANCE`·`STALE_STATUS`·`OTHER_ROOM`, SYNC 거절에 `WATCH_UPDATE_REQUIRED`를 쓴다.
- 폰에서 러닝을 시작하면 메시지가 아니라 플랫폼 기능으로 워치 앱을 켠다.

## 받는 규칙 — 워치

- SYNC를 보내면 SYNCED를 받을 때까지 버튼을 잠근다. 그사이 오는 상태는 버리고, 화면이 켜져 있으면 3초마다 SYNC를 다시 보낸다.
- SYNCED나 켜짐 신호의 답을 기다리는 동안 러닝·결과가 없으면 대기 화면에 문구 없이 앱 그림만 띄운다.
- 러닝 중 화면이 켜져 있는데 상태를 3초 넘게 못 받으면 `폰 연결 확인 중`을 띄우고, 받은 값 전부(시간 포함) 마지막 값에서 멈추고, 버튼을 잠그고, SYNC를 보낸다.
- 화면이 다시 켜지면 러닝이 없어도 3초 안에 상태가 와야 한다. 안 오면 위와 같이 `폰 연결 확인 중`으로 바꾸고 SYNC를 보낸다.
- 결과를 받는 중(`FINISHED`·`result: null`)에 화면이 켜져 있는데 상태를 13초 넘게 못 받으면 `폰 연결 확인 중`을 띄우고 SYNC를 보낸다. 끝난 뒤의 폰은 10초 켜짐 신호에만 답한다. 받은 결과는 끊겨도 그대로 둔다.
- `phoneInstanceId`가 바뀌었거나 모르는 `runningRoomId`가 오면 SYNC. `null`은 모르는 번호로 보지 않는다.
- 러닝 중 SYNC를 보낸 뒤로 10분 넘게 답이 없으면 운동을 끝내고 마지막 값을 지운 뒤 앱을 닫는다(화면이 꺼져 있으면 더 늦어질 수 있다). 폰이 돌아와도 워치가 스스로 다시 켜지 않는다.
- 명령을 보내면 `lastRequestId`가 그 값인 상태나 같은 `requestId`의 `ERROR`가 올 때까지 버튼을 잠근다. 3초 넘게 없으면 SYNC를 보내고 명령은 다시 보내지 않는다.
- `code`가 `WATCH_UPDATE_REQUIRED`인 `ERROR`를 받으면 `requestId`와 상관없이 운동을 끝내고 마지막 값을 지운 뒤 업데이트 화면을 띄운다. 앱을 다시 열 때까지 폰에 새로 보내지 않는다.
- 결과 요약에서 닫기를 누르면 대기 화면으로 돌리고, 뒤로 버튼·쓸어 닫기는 앱을 닫는다. 둘 다 그때의 `phoneInstanceId`·`statusVersion` 쌍을 앱 프로세스가 살아 있는 동안 기억해 같은 쌍의 끝난 상태는 다시 받아도 결과를 띄우지 않는다(폰에 알리지 않는다). 결과 요약이 떠 있을 때 `run: null`을 받아도 대기 화면으로 돌린다.
- 종료 뒤 `GOAL_PENDING`이 오면 한 번 진동한다.
- 러닝 중에 `WATCH_COMBO_TRIGGERED`를 받으면 진동한다. 화면이 꺼져 있거나 어둡거나(항상 켬) 조작 장이 보이면 진동만 하고, 밝아진 뒤 효과를 뒤늦게 띄우지 않는다.
