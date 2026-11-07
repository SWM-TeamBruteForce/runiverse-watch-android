package com.runiverse.app.phone

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhoneLinkTest {

    @Test
    fun `앱을 열면 SYNC를 보내고 SYNCED를 받을 때까지 잠근다`() = runTest {
        // given
        val fixture = Fixture(this)

        // when
        fixture.phoneLink.start()
        runCurrent()

        // then
        val sync = fixture.sent.single() as WatchSync
        assertTrue(fixture.phoneLink.state.value.isLocked)

        // when -> 폰이 답한다
        fixture.phoneLink.onReceived(synced(sync.syncId, state(stateSequence = 1)))

        // then
        val linkState = fixture.phoneLink.state.value
        assertFalse(linkState.isLocked)
        assertEquals(1L, linkState.phoneState?.stateSequence)
    }

    @Test
    fun `보낸 적 없는 syncId의 SYNCED는 버린다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.phoneLink.start()
        runCurrent()

        // when
        fixture.phoneLink.onReceived(synced("모르는-id", state(stateSequence = 1)))

        // then
        assertTrue(fixture.phoneLink.state.value.isSyncing)
        assertNull(fixture.phoneLink.state.value.phoneState)
    }

    @Test
    fun `러닝 중 화면이 켜져 있는데 3초 동안 못 받으면 폰 연결 확인 중으로 바꾸고 SYNC를 보낸다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))
        fixture.phoneLink.onScreenStarted()
        runCurrent()
        fixture.sent.clear()

        // when
        advanceTimeBy(3_001)

        // then
        val linkState = fixture.phoneLink.state.value
        assertFalse(linkState.isPhoneReachable)
        assertTrue(linkState.isLocked)
        assertEquals(1L, linkState.phoneState?.stateSequence)
        assertTrue(fixture.sent.any { it is WatchSync })
    }

    @Test
    fun `보내기가 끝나지 않아도 3초마다 SYNC를 다시 보낸다`() = runTest {
        // given
        val fixture = Fixture(this, isSendHanging = true)
        fixture.phoneLink.onScreenStarted()
        fixture.phoneLink.start()

        // when
        advanceTimeBy(6_001)

        // then
        assertEquals(3, fixture.sent.count { it is WatchSync })
        assertFalse(fixture.phoneLink.state.value.isPhoneReachable)
    }

    @Test
    fun `보내기가 끝나지 않으면 3초 뒤 포기해서 보내기가 쌓이지 않는다`() = runTest {
        // given
        val fixture = Fixture(this, isSendHanging = true)
        fixture.phoneLink.onScreenStarted()
        fixture.phoneLink.start()

        // when
        advanceTimeBy(30_001)

        // then
        assertTrue(fixture.hangingSends <= 2)
    }

    @Test
    fun `화면 멈춤이 시작보다 먼저 와도 다음 시작에서 켜짐 신호를 보낸다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))
        fixture.phoneLink.onScreenStopped()

        // when
        fixture.phoneLink.onScreenStarted()
        runCurrent()

        // then
        assertTrue(fixture.delivered.any { it is WatchVisibilityUpdate && it.isVisible })
    }

    @Test
    fun `화면이 꺼져 있으면 오래 못 받아도 연결 상태를 바꾸지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))

        // when
        advanceTimeBy(60_000)

        // then
        assertTrue(fixture.phoneLink.state.value.isPhoneReachable)
        assertFalse(fixture.phoneLink.state.value.isLocked)
    }

    @Test
    fun `상태가 계속 오면 3초가 지나도 연결된 채로 둔다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))
        fixture.phoneLink.onScreenStarted()

        // when
        for (stateSequence in 2L..5L) {
            advanceTimeBy(1_000)
            fixture.phoneLink.onReceived(WatchStateUpdated(state(stateSequence = stateSequence)))
        }
        advanceTimeBy(2_000)

        // then
        assertTrue(fixture.phoneLink.state.value.isPhoneReachable)
        assertEquals(5L, fixture.phoneLink.state.value.phoneState?.stateSequence)
    }

    @Test
    fun `러닝이 없을 때 화면으로 돌아와 3초 동안 답이 없으면 폰 연결 확인 중으로 바꾸고 SYNC를 보낸다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1).copy(run = null))
        fixture.phoneLink.onScreenStarted()
        fixture.phoneLink.onReceived(WatchStateUpdated(state(stateSequence = 2).copy(run = null)))
        fixture.phoneLink.onScreenStopped()
        runCurrent()
        fixture.sent.clear()

        // when -> 폰이 사라진 뒤 화면으로 돌아온다
        fixture.phoneLink.onScreenStarted()
        advanceTimeBy(3_001)

        // then
        assertFalse(fixture.phoneLink.state.value.isPhoneReachable)
        assertTrue(fixture.sent.any { it is WatchSync })
    }

    @Test
    fun `러닝이 없을 때 화면으로 돌아와 폰이 상태를 보내면 연결된 채로 둔다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1).copy(run = null))

        // when
        fixture.phoneLink.onScreenStarted()
        advanceTimeBy(1_000)
        fixture.phoneLink.onReceived(WatchStateUpdated(state(stateSequence = 2).copy(run = null)))
        advanceTimeBy(5_000)

        // then
        assertTrue(fixture.phoneLink.state.value.isPhoneReachable)
        assertTrue(fixture.sent.none { it is WatchSync })
    }

    @Test
    fun `화면이 꺼져 있으면 SYNC를 한 번만 보낸다`() = runTest {
        // given
        val fixture = Fixture(this)

        // when
        fixture.phoneLink.start()
        advanceTimeBy(6_001)

        // then
        assertEquals(1, fixture.sent.count { it is WatchSync })
        assertTrue(fixture.phoneLink.state.value.isSyncing)
        assertFalse(fixture.phoneLink.state.value.isPhoneReachable)
    }

    @Test
    fun `SYNC 중에 화면이 켜지면 SYNC를 다시 보내고 3초마다 되풀이한다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.phoneLink.start()
        advanceTimeBy(3_001)
        fixture.sent.clear()

        // when
        fixture.phoneLink.onScreenStarted()
        advanceTimeBy(3_001)

        // then
        assertEquals(2, fixture.sent.count { it is WatchSync })
    }

    @Test
    fun `SYNC 중에 온 상태는 버린다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))
        fixture.phoneLink.onReceived(WatchStateUpdated(state(stateSequence = 2, phoneInstanceId = "p2")))

        // when
        fixture.phoneLink.onReceived(WatchStateUpdated(state(stateSequence = 3)))

        // then
        assertTrue(fixture.phoneLink.state.value.isSyncing)
        assertEquals(1L, fixture.phoneLink.state.value.phoneState?.stateSequence)
    }

    @Test
    fun `화면이 켜져 있는 동안 10초마다 켜짐 신호를 보내고 꺼지면 꺼짐 신호를 보낸다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))

        // when
        fixture.phoneLink.onScreenStarted()
        advanceTimeBy(20_001)

        // then
        assertEquals(3, fixture.delivered.count { it is WatchVisibilityUpdate && it.isVisible })

        // when -> 화면이 꺼진다
        fixture.phoneLink.onScreenStopped()
        advanceTimeBy(20_001)

        // then
        val visibilities = fixture.delivered.filterIsInstance<WatchVisibilityUpdate>()
        assertFalse(visibilities.last().isVisible)
        assertEquals(3, visibilities.count { it.isVisible })
    }

    @Test
    fun `지난 번호의 상태는 버린다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 5))

        // when
        fixture.phoneLink.onReceived(WatchStateUpdated(state(stateSequence = 4, distanceMeters = 999)))

        // then
        assertEquals(5L, fixture.phoneLink.state.value.phoneState?.stateSequence)
    }

    @Test
    fun `폰 앱이 다시 켜져 phoneInstanceId가 바뀌면 SYNC를 보내고 그 상태는 쓰지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 5))

        // when
        fixture.phoneLink.onReceived(WatchStateUpdated(state(stateSequence = 1, phoneInstanceId = "p2")))
        runCurrent()

        // then
        assertEquals("p1", fixture.phoneLink.state.value.phoneState?.phoneInstanceId)
        assertTrue(fixture.phoneLink.state.value.isSyncing)
        assertTrue(fixture.sent.single() is WatchSync)
    }

    @Test
    fun `모르는 방 번호가 오면 SYNC를 보낸다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1, runningRoomId = 812))

        // when
        fixture.phoneLink.onReceived(WatchStateUpdated(state(stateSequence = 2, runningRoomId = 900)))
        runCurrent()

        // then
        assertEquals(812L, fixture.phoneLink.state.value.phoneState?.run?.runningRoomId)
        assertTrue(fixture.sent.single() is WatchSync)
    }

    @Test
    fun `방 번호가 처음 오면 SYNC를 보낸다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1, runningRoomId = null))

        // when
        fixture.phoneLink.onReceived(WatchStateUpdated(state(stateSequence = 2, runningRoomId = 812)))
        runCurrent()

        // then
        assertEquals(1L, fixture.phoneLink.state.value.phoneState?.stateSequence)
        assertTrue(fixture.sent.single() is WatchSync)
    }

    @Test
    fun `러닝이 없다가 방 번호 없는 러닝이 오면 SYNC 없이 받는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1).copy(run = null))

        // when
        fixture.phoneLink.onReceived(WatchStateUpdated(state(stateSequence = 2, runningRoomId = null)))
        runCurrent()

        // then
        assertEquals(2L, fixture.phoneLink.state.value.phoneState?.stateSequence)
        assertTrue(fixture.sent.none { it is WatchSync })
    }

    @Test
    fun `방 번호가 있던 러닝이 사라지면 SYNC 없이 run null을 받는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1, runningRoomId = 812))

        // when
        fixture.phoneLink.onReceived(WatchStateUpdated(state(stateSequence = 2).copy(run = null)))
        runCurrent()

        // then
        val linkState = fixture.phoneLink.state.value
        assertEquals(2L, linkState.phoneState?.stateSequence)
        assertNull(linkState.phoneState?.run)
        assertFalse(linkState.isSyncing)
        assertTrue(fixture.sent.none { it is WatchSync })
    }

    @Test
    fun `방 번호가 있던 러닝 다음에 방 번호 없는 러닝이 오면 SYNC 없이 받는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1, runningRoomId = 812))

        // when
        fixture.phoneLink.onReceived(WatchStateUpdated(state(stateSequence = 2, runningRoomId = null)))
        runCurrent()

        // then
        val linkState = fixture.phoneLink.state.value
        assertEquals(2L, linkState.phoneState?.stateSequence)
        assertFalse(linkState.isSyncing)
        assertTrue(fixture.sent.none { it is WatchSync })
    }

    @Test
    fun `명령은 처리됐다는 상태가 올 때까지 잠근다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1, statusVersion = 4))

        // when
        val isSent = fixture.phoneLink.pause()
        runCurrent()

        // then
        assertTrue(isSent)
        val pause = fixture.sent.single() as WatchPause
        assertEquals(
            WatchPause(requestId = pause.requestId, phoneInstanceId = "p1", runningRoomId = 812, statusVersion = 4),
            pause,
        )
        assertTrue(fixture.phoneLink.state.value.isLocked)

        // when -> 폰이 반영한 상태를 보낸다
        fixture.phoneLink.onReceived(
            WatchStateUpdated(state(stateSequence = 2, statusVersion = 5, lastRequestId = pause.requestId)),
        )

        // then
        assertFalse(fixture.phoneLink.state.value.isLocked)
    }

    @Test
    fun `방 번호 없는 러닝에서 누르면 방 번호 null 그대로 명령을 보낸다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1, runningRoomId = null, statusVersion = 4))

        // when
        val isSent = fixture.phoneLink.pause()
        runCurrent()

        // then
        assertTrue(isSent)
        val pause = fixture.sent.single() as WatchPause
        assertEquals(
            WatchPause(requestId = pause.requestId, phoneInstanceId = "p1", runningRoomId = null, statusVersion = 4),
            pause,
        )
    }

    @Test
    fun `다른 명령이 처리된 상태로는 잠금을 풀지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))
        fixture.phoneLink.pause()

        // when
        fixture.phoneLink.onReceived(WatchStateUpdated(state(stateSequence = 2, lastRequestId = "폰에서-누른-것")))

        // then
        assertTrue(fixture.phoneLink.state.value.isLocked)
    }

    @Test
    fun `보낸 명령이 종료인지 구분하고 답을 받으면 지운다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))

        // when
        fixture.phoneLink.pause()
        runCurrent()

        // then
        assertFalse(fixture.phoneLink.state.value.isFinishPending)

        // when -> 일시정지가 처리된 뒤 종료를 보낸다
        val pause = fixture.sent.single() as WatchPause
        fixture.phoneLink.onReceived(WatchStateUpdated(state(stateSequence = 2, lastRequestId = pause.requestId)))
        fixture.phoneLink.finish(forced = false)
        runCurrent()

        // then
        assertTrue(fixture.phoneLink.state.value.isFinishPending)

        // when -> 폰이 반영한 상태를 보낸다
        val finish = fixture.sent.last() as WatchFinish
        fixture.phoneLink.onReceived(WatchStateUpdated(state(stateSequence = 3, lastRequestId = finish.requestId)))

        // then
        assertFalse(fixture.phoneLink.state.value.isFinishPending)
        assertFalse(fixture.phoneLink.state.value.isLocked)
    }

    @Test
    fun `일시정지를 거절받으면 잠금을 푼다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))
        fixture.phoneLink.pause()
        runCurrent()
        val pause = fixture.sent.single() as WatchPause

        // when
        fixture.phoneLink.onReceived(PhoneError("STALE_STATUS", "다른 상태", "WATCH_PAUSE", pause.requestId))

        // then
        assertFalse(fixture.phoneLink.state.value.isLocked)
    }

    @Test
    fun `일시정지에 3초 동안 답이 없어도 종료 대기로 보지 않고 SYNC만 보낸다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))
        fixture.phoneLink.pause()
        runCurrent()

        // when
        advanceTimeBy(3_001)

        // then
        val linkState = fixture.phoneLink.state.value
        assertTrue(linkState.isCommandOverdue)
        assertTrue(linkState.isLocked)
        assertFalse(linkState.isFinishPending)
        assertEquals(1, fixture.sent.count { it is WatchPause })
        assertTrue(fixture.sent.last() is WatchSync)
    }

    @Test
    fun `종료를 거절받으면 종료 대기를 지운다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))
        fixture.phoneLink.finish(forced = false)
        runCurrent()
        val finish = fixture.sent.single() as WatchFinish

        // when
        fixture.phoneLink.onReceived(PhoneError("STALE_STATUS", "다른 상태", "WATCH_FINISH", finish.requestId))

        // then
        assertFalse(fixture.phoneLink.state.value.isFinishPending)
        assertFalse(fixture.phoneLink.state.value.isLocked)
    }

    @Test
    fun `명령에 3초 동안 답이 없으면 SYNC를 보내고 명령은 다시 보내지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))
        fixture.phoneLink.finish(forced = false)
        runCurrent()

        // when
        advanceTimeBy(3_001)

        // then
        assertTrue(fixture.phoneLink.state.value.isCommandOverdue)
        assertTrue(fixture.phoneLink.state.value.isFinishPending)
        assertTrue(fixture.phoneLink.state.value.isLocked)
        assertEquals(1, fixture.sent.count { it is WatchFinish })
        assertTrue(fixture.sent.last() is WatchSync)
    }

    @Test
    fun `명령에 답이 없어 SYNC를 보낸 뒤 늦게 SYNCED가 오면 잠금을 푼다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))
        fixture.phoneLink.finish(forced = false)
        runCurrent()
        advanceTimeBy(3_001)
        val sync = fixture.sent.last() as WatchSync

        // when
        fixture.phoneLink.onReceived(synced(sync.syncId, state(stateSequence = 2)))

        // then
        assertFalse(fixture.phoneLink.state.value.isFinishPending)
        assertFalse(fixture.phoneLink.state.value.isLocked)
        assertFalse(fixture.phoneLink.state.value.isCommandOverdue)
    }

    @Test
    fun `폰 상태를 받기 전이거나 러닝이 없으면 명령을 보내지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)

        // when & then -> 폰 상태를 아직 받지 않았다
        assertFalse(fixture.phoneLink.pause())

        // given
        fixture.connect(state(stateSequence = 1).copy(run = null))

        // when & then -> 러닝이 없다
        assertFalse(fixture.phoneLink.finish(forced = false))
        runCurrent()
        assertTrue(fixture.sent.none { it is WatchPause || it is WatchFinish })
    }

    @Test
    fun `요청 번호가 없거나 다른 ERROR로는 잠금을 풀지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))
        fixture.phoneLink.pause()

        // when
        fixture.phoneLink.onReceived(PhoneError(code = "X", message = "m", sourceType = "WATCH_PAUSE", requestId = null))
        fixture.phoneLink.onReceived(PhoneError(code = "X", message = "m", sourceType = "WATCH_PAUSE", requestId = "다른-명령"))

        // then
        assertTrue(fixture.phoneLink.state.value.isLocked)
    }

    @Test
    fun `거절 ERROR가 오면 잠금을 푼다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))
        fixture.phoneLink.resume()
        runCurrent()
        val resume = fixture.sent.single() as WatchResume

        // when
        fixture.phoneLink.onReceived(
            PhoneError(
                code = "STATUS_VERSION_MISMATCH",
                message = "상태가 바뀌었습니다.",
                sourceType = "WATCH_RESUME",
                requestId = resume.requestId,
            ),
        )

        // then
        assertFalse(fixture.phoneLink.state.value.isLocked)
    }

    @Test
    fun `잠겨 있으면 명령을 보내지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))
        fixture.phoneLink.pause()
        runCurrent()

        // when
        val isSent = fixture.phoneLink.finish(forced = true)
        runCurrent()

        // then
        assertFalse(isSent)
        assertEquals(1, fixture.sent.size)
    }

    @Test
    fun `화면 둘이 겹쳐도 하나가 멈추면 꺼짐을 보내지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.phoneLink.onScreenStarted()
        fixture.phoneLink.onScreenStarted()
        runCurrent()
        fixture.sent.clear()

        // when
        fixture.phoneLink.onScreenStopped()
        runCurrent()

        // then
        assertTrue(fixture.sent.none { it is WatchVisibilityUpdate && !it.isVisible })

        // when -> 남은 화면도 멈춘다
        fixture.phoneLink.onScreenStopped()
        runCurrent()

        // then
        assertTrue(fixture.sent.any { it is WatchVisibilityUpdate && !it.isVisible })
    }

    @Test
    fun `꺼짐 신호 보내기가 늦어져도 마지막으로 닿는 신호는 지금 화면 상태다`() = runTest {
        // given
        val fixture = Fixture(this, sendDelayMs = { if (it is WatchVisibilityUpdate && !it.isVisible) 500 else 0 })
        fixture.phoneLink.onScreenStarted()
        runCurrent()

        // when -> 꺼졌다 바로 다시 켜진다
        fixture.phoneLink.onScreenStopped()
        fixture.phoneLink.onScreenStarted()
        advanceTimeBy(1_000)

        // then
        val last = fixture.delivered.filterIsInstance<WatchVisibilityUpdate>().last()
        assertTrue(last.isVisible)
    }

    @Test
    fun `같은 eventId의 콤보는 한 번만 내보낸다`() = runTest {
        // given
        val fixture = Fixture(this)
        val received = mutableListOf<WatchComboTriggered>()
        backgroundScope.launch { fixture.phoneLink.combos.collect { received += it } }
        runCurrent()
        val combo = WatchComboTriggered(eventId = "e1", userId = "u2", comboCount = 3, kind = ComboKind.MILESTONE)

        // when
        fixture.phoneLink.onReceived(combo)
        fixture.phoneLink.onReceived(combo)
        runCurrent()

        // then
        assertEquals(listOf(combo), received)
    }

    @Test
    fun `서버 보류로 들어갈 때마다 한 번씩만 알린다`() = runTest {
        // given
        val pending = FinishProgress(FinishStage.GOAL_PENDING, remainingMeters = 40)
        val finishing = FinishProgress(FinishStage.FINISHING)
        val linkStates = listOf(null, pending, pending, finishing, pending, null).mapIndexed { index, finish ->
            val base = state(stateSequence = index.toLong())
            PhoneLinkState(phoneState = base.copy(run = base.run?.copy(finish = finish)))
        }

        // when
        val starts = linkStates.asFlow().goalPendingStarts().toList()

        // then
        assertEquals(2, starts.size)
    }

    @Test
    fun `계속 달리기로 보류가 풀린 뒤 다시 보류되면 한 번 더 알린다`() = runTest {
        // given
        val pending = FinishProgress(FinishStage.GOAL_PENDING, remainingMeters = 40)
        val linkStates = listOf(pending, null, pending).mapIndexed { index, finish ->
            val base = state(stateSequence = index.toLong())
            PhoneLinkState(phoneState = base.copy(run = base.run?.copy(finish = finish)))
        }

        // when
        val starts = linkStates.asFlow().goalPendingStarts().toList()

        // then
        assertEquals(2, starts.size)
    }

    @Test
    fun `연결 상태가 바뀌어도 같은 보류는 다시 알리지 않고 상태가 없으면 알리지 않는다`() = runTest {
        // given
        val base = state(stateSequence = 1)
        val pending = base.copy(run = base.run?.copy(finish = FinishProgress(FinishStage.GOAL_PENDING, 40)))
        val linkStates = listOf(
            PhoneLinkState(),
            PhoneLinkState(phoneState = pending),
            PhoneLinkState(phoneState = pending, isPhoneReachable = false, isSyncing = true),
            PhoneLinkState(phoneState = pending),
        )

        // when
        val starts = linkStates.asFlow().goalPendingStarts().toList()

        // then
        assertEquals(1, starts.size)
    }

    @Test
    fun `일시정지·끊김은 진행 중인 러닝이고 끝났거나 러닝이 없으면 아니다`() {
        // given
        val running = state(stateSequence = 1)
        val runningRun = requireNotNull(running.run)

        // when & then
        assertTrue(PhoneLinkState(phoneState = running).isRunActive)
        assertTrue(linkStateWith(running, runningRun.copy(status = RunStatus.PAUSED)).isRunActive)
        assertTrue(linkStateWith(running, runningRun.copy(status = RunStatus.DISCONNECTED)).isRunActive)
        assertFalse(linkStateWith(running, runningRun.copy(status = RunStatus.FINISHED)).isRunActive)
        assertFalse(linkStateWith(running, null).isRunActive)
        assertFalse(PhoneLinkState().isRunActive)
    }

    @Test
    fun `화면이 꺼져 있으면 물어볼 때 SYNC를 보낸다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))

        // when
        fixture.phoneLink.probeWhileScreenOff()
        runCurrent()

        // then
        assertEquals(1, fixture.sent.count { it is WatchSync })
    }

    @Test
    fun `화면이 켜져 있으면 물어봐도 SYNC를 보내지 않고 잠그지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))
        fixture.phoneLink.onScreenStarted()
        runCurrent()
        fixture.sent.clear()

        // when
        fixture.phoneLink.probeWhileScreenOff()
        runCurrent()

        // then
        assertEquals(0, fixture.sent.count { it is WatchSync })
        assertFalse(fixture.phoneLink.state.value.isLocked)
    }

    @Test
    fun `러닝을 접으면 마지막 값을 지우고 다음 상태는 받지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))
        fixture.phoneLink.probeWhileScreenOff()
        runCurrent()

        // when
        fixture.phoneLink.forgetRun()
        fixture.phoneLink.onReceived(WatchStateUpdated(state(stateSequence = 2)))

        // then
        assertEquals(PhoneLinkState(), fixture.phoneLink.state.value)
    }

    @Test
    fun `러닝을 접기 전에 보낸 SYNC의 답이 늦게 와도 받지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))
        fixture.phoneLink.probeWhileScreenOff()
        runCurrent()
        val lateSync = fixture.sent.last { it is WatchSync } as WatchSync

        // when
        fixture.phoneLink.forgetRun()
        fixture.phoneLink.onReceived(synced(lateSync.syncId, state(stateSequence = 5)))

        // then
        assertEquals(PhoneLinkState(), fixture.phoneLink.state.value)
    }

    @Test
    fun `러닝을 접은 뒤 3초가 지나도 연결 끊김으로 바뀌지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))
        fixture.phoneLink.probeWhileScreenOff()
        runCurrent()

        // when
        fixture.phoneLink.forgetRun()
        advanceTimeBy(3_001)

        // then
        assertTrue(fixture.phoneLink.state.value.isPhoneReachable)
    }

    @Test
    fun `러닝을 접은 뒤 앱을 다시 열면 SYNCED로 러닝을 다시 받는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(stateSequence = 1))
        fixture.phoneLink.forgetRun()

        // when
        fixture.phoneLink.start()
        runCurrent()
        val sync = fixture.sent.last { it is WatchSync } as WatchSync
        fixture.phoneLink.onReceived(synced(sync.syncId, state(stateSequence = 5)))

        // then
        assertEquals(5L, fixture.phoneLink.state.value.phoneState?.stateSequence)
        assertTrue(fixture.phoneLink.state.value.isRunActive)
    }

    private fun linkStateWith(base: PhoneState, run: RunState?) = PhoneLinkState(phoneState = base.copy(run = run))

    private class Fixture(
        private val testScope: TestScope,
        isSendHanging: Boolean = false,
        sendDelayMs: (WatchMessage) -> Long = { 0 },
    ) {
        val sent = mutableListOf<WatchMessage>()
        val delivered = mutableListOf<WatchMessage>()
        var hangingSends = 0
        private var nextId = 0
        val phoneLink = PhoneLink(
            scope = testScope.backgroundScope,
            send = {
                sent += it
                if (isSendHanging) {
                    hangingSends++
                    try {
                        awaitCancellation()
                    } finally {
                        hangingSends--
                    }
                }
                delay(sendDelayMs(it))
                delivered += it
            },
            newId = { "id-${++nextId}" },
        )

        fun connect(initial: PhoneState) {
            phoneLink.start()
            testScope.runCurrent()
            val sync = sent.last() as WatchSync
            phoneLink.onReceived(synced(sync.syncId, initial))
            sent.clear()
        }
    }

    private companion object {
        fun synced(syncId: String, state: PhoneState) =
            WatchSynced(syncId = syncId, state = state, birthday = null, photos = emptyList())

        fun state(
            stateSequence: Long,
            phoneInstanceId: String = "p1",
            runningRoomId: Long? = 812,
            statusVersion: Int = 0,
            lastRequestId: String? = null,
            distanceMeters: Int = 1000,
        ) = PhoneState(
            phoneInstanceId = phoneInstanceId,
            stateSequence = stateSequence,
            statusVersion = statusVersion,
            lastRequestId = lastRequestId,
            run = RunState(
                runningRoomId = runningRoomId,
                type = RoomType.MATCH,
                status = RunStatus.RUNNING,
                targetDistanceMeters = 5000,
                isServerConnected = true,
                isPenaltyApplicable = true,
                me = MyRecord(distanceMeters = distanceMeters, elapsedSeconds = 300),
                players = emptyList(),
            ),
        )
    }
}
