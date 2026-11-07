package com.runiverse.app.exercise

import com.runiverse.app.phone.MyRecord
import com.runiverse.app.phone.PhoneLink
import com.runiverse.app.phone.PhoneState
import com.runiverse.app.phone.RoomType
import com.runiverse.app.phone.RunState
import com.runiverse.app.phone.RunStatus
import com.runiverse.app.phone.WatchMessage
import com.runiverse.app.phone.WatchStateUpdated
import com.runiverse.app.phone.WatchSync
import com.runiverse.app.phone.WatchSynced
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
class ExerciseSessionTest {

    @Test
    fun `시작하면 운동을 시작하고 받은 심박을 내보낸다`() = runTest {
        // given
        val fixture = Fixture(this)

        // when
        fixture.startSession()
        fixture.tracker.deliver(148)

        // then
        assertEquals(listOf("listen", "start"), fixture.tracker.calls)
        assertEquals(148, fixture.session.heartRateBpm.value)
    }

    @Test
    fun `심박을 잴 수 없으면 운동을 시작하지 않고 세션은 유지한다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.tracker.isMeasurable = false

        // when
        fixture.startSession()

        // then
        assertTrue(fixture.tracker.calls.isEmpty())
        assertFalse(fixture.isEnded)
    }

    @Test
    fun `다른 앱이 운동 중이면 덮어쓰지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.tracker.owner = ExerciseOwner.OTHER_APP

        // when
        fixture.startSession()

        // then
        assertTrue(fixture.tracker.calls.isEmpty())
        assertFalse(fixture.isEnded)
    }

    @Test
    fun `다른 앱 운동이 끝난 뒤 시작 요청을 다시 받으면 심박을 잰다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.tracker.owner = ExerciseOwner.OTHER_APP
        fixture.startSession()

        // when
        fixture.tracker.owner = ExerciseOwner.NONE
        fixture.startSession()

        // then
        assertEquals(listOf("listen", "start"), fixture.tracker.calls)
    }

    @Test
    fun `이미 내 운동이 돌고 있으면 새로 시작하지 않고 이어 받는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.tracker.owner = ExerciseOwner.ME

        // when
        fixture.startSession()

        // then
        assertEquals(listOf("listen"), fixture.tracker.calls)
    }

    @Test
    fun `재는 중에 시작 요청을 다시 받아도 운동을 또 시작하지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()

        // when
        fixture.startSession()

        // then
        assertEquals(listOf("listen", "start"), fixture.tracker.calls)
    }

    @Test
    fun `측정할 수 없게 되면 심박이 null이 된다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()
        fixture.tracker.deliver(148)

        // when
        fixture.tracker.deliver(null)

        // then
        assertNull(fixture.session.heartRateBpm.value)
    }

    @Test
    fun `다른 앱에 밀려 운동이 끝나면 심박은 null이고 세션은 유지한다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()
        fixture.tracker.deliver(148)

        // when
        fixture.tracker.endByOtherApp()
        runCurrent()

        // then
        assertNull(fixture.session.heartRateBpm.value)
        assertFalse(fixture.isEnded)
    }

    @Test
    fun `운동 시작을 기다리는 사이 다른 앱에 밀리면 다음 시작 요청에서 다시 잰다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.tracker.startDelayMs = 1_000
        fixture.startSession()

        // when
        fixture.tracker.endByOtherApp()
        advanceTimeBy(1_001)
        runCurrent()
        fixture.startSession()
        advanceTimeBy(1_001)
        runCurrent()

        // then
        assertEquals(listOf("listen", "start", "listen"), fixture.tracker.calls)
    }

    @Test
    fun `측정이 끊긴 채 러닝이 끝나도 운동 정리를 부른다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()
        fixture.tracker.endByOtherApp()

        // when
        fixture.receiveState(sequence = 2, run = null)
        runCurrent()

        // then
        assertEquals("stop", fixture.tracker.calls.last())
    }

    @Test
    fun `이미 끝난 세션을 서비스가 다시 멈춰도 운동 정리는 한 번만 부른다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()
        fixture.receiveState(sequence = 2, run = null)
        runCurrent()

        // when
        fixture.session.stop()
        runCurrent()

        // then
        assertEquals(1, fixture.tracker.calls.count { it == "stop" })
    }

    @Test
    fun `운동 시작에 실패하면 다음 시작 요청에서 다시 시작한다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.tracker.isStartSuccessful = false
        fixture.startSession()

        // when
        fixture.tracker.isStartSuccessful = true
        fixture.startSession()

        // then
        assertEquals(listOf("listen", "start", "listen", "start"), fixture.tracker.calls)
    }

    @Test
    fun `운동 주인을 알 수 없으면 운동을 시작하지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.tracker.owner = null

        // when
        fixture.startSession()

        // then
        assertTrue(fixture.tracker.calls.isEmpty())
        assertFalse(fixture.isEnded)
    }

    @Test
    fun `운동 주인을 묻는 사이 러닝이 끝나면 운동을 시작하지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.tracker.ownerDelayMs = 1_000
        fixture.startSession()

        // when
        fixture.receiveState(sequence = 2, run = null)
        runCurrent()
        advanceTimeBy(1_001)
        runCurrent()

        // then
        assertEquals(listOf("stop"), fixture.tracker.calls)
    }

    @Test
    fun `운동을 끈 뒤에 세션 종료를 알린다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()
        fixture.tracker.stopDelayMs = 1_000

        // when
        fixture.receiveState(sequence = 2, run = null)
        runCurrent()

        // then -> 운동을 끄는 동안엔 서비스를 내리지 않는다
        assertFalse(fixture.isEnded)

        // when
        advanceTimeBy(1_001)
        runCurrent()

        // then
        assertTrue(fixture.isEnded)
    }

    @Test
    fun `운동 끄기가 실패하면 다시 시도해 끈 뒤에 세션 종료를 알린다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()
        fixture.tracker.stopFailures = 1

        // when
        fixture.receiveState(sequence = 2, run = null)
        runCurrent()

        // then -> 다시 시도하기 전엔 서비스를 내리지 않는다
        assertFalse(fixture.isEnded)

        // when
        advanceTimeBy(1_001)
        runCurrent()

        // then
        assertEquals(listOf("listen", "start", "stop", "stop"), fixture.tracker.calls)
        assertEquals(ExerciseOwner.NONE, fixture.tracker.owner)
        assertTrue(fixture.isEnded)
    }

    @Test
    fun `끄기를 다시 시도하는 사이 다시 시작해도 다시 시도가 끝난 뒤에 운동을 시작한다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()
        fixture.tracker.stopFailures = 1
        fixture.tracker.calls.clear()

        // when
        fixture.session.stop()
        runCurrent()
        fixture.startSession()
        advanceTimeBy(1_001)
        runCurrent()

        // then
        assertEquals(listOf("stop", "stop", "listen", "start"), fixture.tracker.calls)
        assertEquals(ExerciseOwner.ME, fixture.tracker.owner)
    }

    @Test
    fun `운동 끄기가 계속 실패해도 세 번 시도한 뒤에는 세션 종료를 알린다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()
        fixture.tracker.stopFailures = 5

        // when
        fixture.receiveState(sequence = 2, run = null)
        advanceTimeBy(2_001)
        runCurrent()

        // then
        assertEquals(3, fixture.tracker.calls.count { it == "stop" })
        assertTrue(fixture.isEnded)
    }

    @Test
    fun `세션을 시작한 적 없이 러닝이 없다는 상태를 받으면 남은 운동을 한 번 끈다`() = runTest {
        // given -> 앱이 죽었다 다시 열려 이 프로세스에선 세션이 없다
        val fixture = Fixture(this)

        // when
        fixture.receiveState(sequence = 2, run = run(RunStatus.FINISHED))
        fixture.receiveState(sequence = 3, run = null)
        runCurrent()

        // then
        assertEquals(listOf("stop"), fixture.tracker.calls)
    }

    @Test
    fun `세션을 시작한 적 없어도 러닝 중이면 운동을 끄지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)

        // when
        fixture.receiveState(sequence = 2, run = run(RunStatus.PAUSED))
        runCurrent()

        // then
        assertTrue(fixture.tracker.calls.isEmpty())
    }

    @Test
    fun `러닝이 사라지면 운동을 끝내고 세션을 끝낸다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()

        // when
        fixture.receiveState(sequence = 2, run = null)
        runCurrent()

        // then
        assertEquals("stop", fixture.tracker.calls.last())
        assertTrue(fixture.isEnded)
        assertFalse(fixture.isPhoneLost)
    }

    @Test
    fun `러닝이 끝나면 운동을 끝내고 세션을 끝낸다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()

        // when
        fixture.receiveState(sequence = 2, run = run(RunStatus.FINISHED))
        runCurrent()

        // then
        assertEquals("stop", fixture.tracker.calls.last())
        assertTrue(fixture.isEnded)
    }

    @Test
    fun `일시정지는 운동을 끝내지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()

        // when
        fixture.receiveState(sequence = 2, run = run(RunStatus.PAUSED))
        runCurrent()

        // then
        assertFalse(fixture.tracker.calls.contains("stop"))
        assertFalse(fixture.isEnded)
    }

    @Test
    fun `끝낸 뒤 늦게 온 심박은 버린다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()
        fixture.session.stop()
        runCurrent()

        // when
        fixture.tracker.deliver(148)

        // then
        assertNull(fixture.session.heartRateBpm.value)
    }

    @Test
    fun `끝내는 중에 다시 시작해도 끝난 뒤에 운동을 시작한다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()
        fixture.tracker.stopDelayMs = 1_000
        fixture.tracker.calls.clear()

        // when
        fixture.session.stop()
        runCurrent()
        fixture.startSession()
        advanceTimeBy(1_001)
        runCurrent()

        // then
        assertEquals(listOf("stop", "listen", "start"), fixture.tracker.calls)
    }

    @Test
    fun `화면이 꺼진 동안 폰 소식이 1분 넘게 없으면 SYNC로 묻는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()
        fixture.sent.clear()

        // when
        advanceTimeBy(60_001)
        runCurrent()

        // then
        assertEquals(1, fixture.sent.count { it is WatchSync })
    }

    @Test
    fun `화면이 켜져 있으면 3초 감시에 맡기고 따로 묻지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()
        fixture.link.onScreenStarted()
        runCurrent()
        fixture.sent.clear()
        fixture.sleptMs += 60_001

        // when
        fixture.tracker.deliver(148)
        runCurrent()

        // then
        assertEquals(0, fixture.sent.count { it is WatchSync })
    }

    @Test
    fun `CPU가 잠들어 시간 재기가 밀려도 심박이 배달되면 실제 시각으로 묻는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()
        fixture.sent.clear()

        // when
        fixture.sleptMs += 150_000
        fixture.tracker.deliver(148)
        runCurrent()

        // then
        assertEquals(1, fixture.sent.count { it is WatchSync })
    }

    @Test
    fun `심박이 자주 와도 1분에 한 번만 묻는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()
        fixture.sent.clear()
        fixture.sleptMs += 60_001

        // when
        repeat(10) {
            fixture.sleptMs += 1_000
            fixture.tracker.deliver(148)
        }
        runCurrent()

        // then
        assertEquals(1, fixture.sent.count { it is WatchSync })
    }

    @Test
    fun `물어본 뒤 답이 없는 채 10분이 넘으면 마지막 값을 지우고 세션을 끝내고 화면에 알린다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()

        // when -> 첫 물음은 1분째, 10분 뒤인 11분째 마지막으로 묻고 1분을 더 기다린다
        advanceTimeBy(720_001)
        runCurrent()

        // then
        assertNull(fixture.link.state.value.phoneState)
        assertEquals("stop", fixture.tracker.calls.last())
        assertTrue(fixture.isEnded)
        assertTrue(fixture.isPhoneLost)
    }

    @Test
    fun `답이 없어도 10분이 안 되면 세션을 유지한다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()

        // when
        advanceTimeBy(600_001)
        runCurrent()

        // then
        assertFalse(fixture.isEnded)
        assertFalse(fixture.isPhoneLost)
    }

    @Test
    fun `오래 조용했다가 깨어나도 물어보기 전엔 세션을 끝내지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()
        fixture.sent.clear()

        // when
        fixture.sleptMs += 3_600_000
        fixture.tracker.deliver(148)
        runCurrent()

        // then
        assertFalse(fixture.isEnded)
        assertEquals(1, fixture.sent.count { it is WatchSync })
    }

    @Test
    fun `한 번 물은 뒤 오래 잠들었다 깨어나면 끝내지 않고 다시 묻는다`() = runTest {
        // given -> 1분째 물었지만 답이 없다
        val fixture = Fixture(this)
        fixture.startSession()
        advanceTimeBy(60_001)
        runCurrent()
        fixture.sent.clear()

        // when
        fixture.sleptMs += 600_000
        fixture.tracker.deliver(148)
        runCurrent()

        // then
        assertFalse(fixture.isEnded)
        assertEquals(1, fixture.sent.count { it is WatchSync })
    }

    @Test
    fun `다시 물은 뒤에도 답이 없으면 그때 세션을 끝낸다`() = runTest {
        // given -> 오래 잠들었다 깨어나 다시 물었다
        val fixture = Fixture(this)
        fixture.startSession()
        advanceTimeBy(60_001)
        runCurrent()
        fixture.sleptMs += 600_000
        fixture.tracker.deliver(148)
        runCurrent()

        // when
        fixture.sleptMs += 60_000
        fixture.tracker.deliver(148)
        runCurrent()

        // then
        assertTrue(fixture.isEnded)
        assertTrue(fixture.isPhoneLost)
    }

    @Test
    fun `심박이 150초 간격으로 와도 답이 없으면 10분 뒤 세션을 끝낸다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()

        // when -> CPU가 잠들어 시간 재기는 멈추고 심박만 배달된다
        repeat(6) {
            fixture.sleptMs += 150_000
            fixture.tracker.deliver(148)
        }
        runCurrent()

        // then
        assertTrue(fixture.isEnded)
        assertTrue(fixture.isPhoneLost)
    }

    @Test
    fun `심박이 5분 넘는 간격으로 와도 답이 없으면 세션을 끝낸다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()

        // when -> 실기기에서 잠든 동안 배달 간격이 302초까지 벌어졌다
        repeat(4) {
            fixture.sleptMs += 302_000
            fixture.tracker.deliver(148)
        }
        runCurrent()

        // then
        assertTrue(fixture.isEnded)
        assertTrue(fixture.isPhoneLost)
    }

    @Test
    fun `폰을 잃고 끝난 뒤 다시 시작하면 10분을 처음부터 센다`() = runTest {
        // given -> 폰을 잃어 세션이 끝났다가 앱을 다시 열어 러닝을 받았다
        val fixture = Fixture(this)
        fixture.startSession()
        advanceTimeBy(720_001)
        runCurrent()
        fixture.link.start()
        runCurrent()
        fixture.answerSync(sequence = 3)
        fixture.isEnded = false
        fixture.isPhoneLost = false

        // when
        fixture.startSession()
        advanceTimeBy(120_001)
        runCurrent()

        // then
        assertFalse(fixture.isEnded)
        assertFalse(fixture.isPhoneLost)
    }

    @Test
    fun `SYNC에 답이 오면 10분을 처음부터 다시 센다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.startSession()
        advanceTimeBy(540_001)
        runCurrent()

        // when -> 9분째 폰이 답한다
        fixture.answerSync(sequence = 2)
        advanceTimeBy(240_000)
        runCurrent()

        // then -> 그 뒤 4분이 지나 처음부터 세면 13분이어도 끝나지 않는다
        assertFalse(fixture.isEnded)
        assertFalse(fixture.isPhoneLost)
    }

    @Test
    fun `포기 시각에 막 받은 폰 답이 수집기보다 늦게 닿아도 세션을 끝내지 않는다`() = runTest {
        // given -> 1분째 묻고, 오래 잠들었다 깨어나 다시 물었다
        val fixture = Fixture(this)
        fixture.startSession()
        advanceTimeBy(60_001)
        runCurrent()
        fixture.sleptMs += 600_000
        fixture.tracker.deliver(148)
        runCurrent()
        fixture.sleptMs += 60_000

        // when -> 수집기가 돌기 전에 심박이 먼저 배달된다
        fixture.answerSync(sequence = 2)
        fixture.tracker.deliver(148)
        runCurrent()

        // then
        assertFalse(fixture.isEnded)
        assertFalse(fixture.isPhoneLost)
        assertEquals(2L, fixture.link.state.value.phoneState?.stateSequence)
    }

    private class Fixture(private val testScope: TestScope) {
        val sent = mutableListOf<WatchMessage>()
        val tracker = FakeTracker()
        var sleptMs = 0L
        var isEnded = false
        var isPhoneLost = false
        private var nextId = 0
        val link = PhoneLink(
            scope = testScope.backgroundScope,
            send = { sent += it },
            newId = { "id-${++nextId}" },
        )
        val session = ExerciseSession(
            scope = testScope.backgroundScope,
            phoneLink = link,
            tracker = tracker,
            nowMs = { testScope.testScheduler.currentTime + sleptMs },
        )

        init {
            link.start()
            testScope.runCurrent()
            answerSync(sequence = 1)
            testScope.backgroundScope.launch { session.phoneLost.collect { isPhoneLost = true } }
            testScope.runCurrent()
        }

        fun startSession() {
            session.start(onEnded = { isEnded = true })
            testScope.runCurrent()
        }

        fun answerSync(sequence: Long) {
            val sync = sent.last { it is WatchSync } as WatchSync
            link.onReceived(
                WatchSynced(
                    syncId = sync.syncId,
                    state = state(sequence, run(RunStatus.RUNNING)),
                    birthday = null,
                    photos = emptyList(),
                ),
            )
        }

        fun receiveState(sequence: Long, run: RunState?) {
            link.onReceived(WatchStateUpdated(state(sequence, run)))
        }
    }

    private class FakeTracker : HeartRateTracker {
        val calls = mutableListOf<String>()
        var isMeasurable = true
        var owner: ExerciseOwner? = ExerciseOwner.NONE
        var isStartSuccessful = true
        var ownerDelayMs = 0L
        var startDelayMs = 0L
        var stopDelayMs = 0L
        var stopFailures = 0
        private var onHeartRate: ((Int?) -> Unit)? = null
        private var onMeasuringStopped: (() -> Unit)? = null

        override suspend fun canMeasure() = isMeasurable

        override suspend fun currentOwner(): ExerciseOwner? {
            delay(ownerDelayMs)
            return owner
        }

        override fun listen(onHeartRate: (Int?) -> Unit, onMeasuringStopped: () -> Unit) {
            calls += "listen"
            this.onHeartRate = onHeartRate
            this.onMeasuringStopped = onMeasuringStopped
        }

        override suspend fun startExercise(): Boolean {
            calls += "start"
            delay(startDelayMs)
            if (!isStartSuccessful) return false
            owner = ExerciseOwner.ME
            return true
        }

        override suspend fun stop(): Boolean {
            calls += "stop"
            delay(stopDelayMs)
            if (stopFailures > 0) {
                stopFailures--
                return false
            }
            owner = ExerciseOwner.NONE
            return true
        }

        fun deliver(heartRateBpm: Int?) {
            onHeartRate?.invoke(heartRateBpm)
        }

        fun endByOtherApp() {
            onMeasuringStopped?.invoke()
        }
    }

    private companion object {
        fun state(sequence: Long, run: RunState?) = PhoneState(
            phoneInstanceId = "p1",
            stateSequence = sequence,
            statusVersion = 0,
            run = run,
        )

        fun run(status: RunStatus) = RunState(
            runningRoomId = 812,
            type = RoomType.MATCH,
            status = status,
            targetDistanceMeters = 5000,
            isServerConnected = true,
            isPenaltyApplicable = true,
            me = MyRecord(distanceMeters = 1000, elapsedSeconds = 300),
            players = emptyList(),
        )
    }
}
