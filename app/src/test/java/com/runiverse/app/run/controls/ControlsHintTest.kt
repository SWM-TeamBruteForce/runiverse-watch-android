package com.runiverse.app.run.controls

import com.runiverse.app.phone.FinishProgress
import com.runiverse.app.phone.FinishStage
import com.runiverse.app.phone.RunState
import com.runiverse.app.phone.RunStatus
import com.runiverse.app.run.common.RunPreviewSamples
import org.junit.Assert.assertEquals
import org.junit.Test

class ControlsHintTest {

    private val party = RunPreviewSamples.party.copy(targetDistanceMeters = 5000, isPenaltyApplicable = true)

    private fun RunState.at(distanceMeters: Int, status: RunStatus = RunStatus.RUNNING) =
        copy(status = status, me = me.copy(distanceMeters = distanceMeters))

    @Test
    fun `제재 방 80퍼센트 전에는 80퍼센트까지 남은 거리와 매칭 제한을 알린다`() {
        // when & then
        assertEquals(
            ControlsHint(ControlsFirstHint.TO_PENALTY_LINE, 420, ControlsSecondHint.PENALTY_IF_SHORT),
            controlsHint(party.at(3580), isPhoneReachable = true),
        )
        assertEquals(
            ControlsHint(ControlsFirstHint.TO_PENALTY_LINE, 420, ControlsSecondHint.PENALTY_IF_FINISHED),
            controlsHint(party.at(3580, RunStatus.PAUSED), isPhoneReachable = true),
        )
    }

    @Test
    fun `80퍼센트를 넘으면 목표까지 남은 거리로 바뀌고 멈췄을 때만 미완주를 알린다`() {
        // when & then
        assertEquals(
            ControlsHint(ControlsFirstHint.TO_GOAL, 1000),
            controlsHint(party.at(4000), isPhoneReachable = true),
        )
        assertEquals(
            ControlsHint(ControlsFirstHint.TO_GOAL, 380, ControlsSecondHint.MAY_NOT_COMPLETE),
            controlsHint(party.at(4620, RunStatus.PAUSED), isPhoneReachable = true),
        )
    }

    @Test
    fun `제재 없는 방은 80퍼센트 전에도 목표까지 남은 거리를 알린다`() {
        // given
        val run = party.copy(isPenaltyApplicable = false)

        // when & then
        assertEquals(
            ControlsHint(ControlsFirstHint.TO_GOAL, 3400),
            controlsHint(run.at(1600), isPhoneReachable = true),
        )
    }

    @Test
    fun `화면 거리가 목표에 닿으면 확인 중으로 두고 달리는 중에만 계속 달리기를 권한다`() {
        // when & then
        assertEquals(
            ControlsHint(ControlsFirstHint.DISTANCE_CHECKING, second = ControlsSecondHint.KEEP_RUNNING_IF_SHORT),
            controlsHint(party.at(5000), isPhoneReachable = true),
        )
        assertEquals(
            ControlsHint(ControlsFirstHint.DISTANCE_CHECKING),
            controlsHint(party.at(5000, RunStatus.PAUSED), isPhoneReachable = true),
        )
    }

    @Test
    fun `서버가 남은 거리를 답하면 화면 거리보다 먼저 쓴다`() {
        // given
        val pending = party.copy(finish = FinishProgress(FinishStage.GOAL_PENDING, remainingMeters = 40))

        // when & then
        assertEquals(
            ControlsHint(ControlsFirstHint.TO_GOAL, 40),
            controlsHint(pending.at(5000), isPhoneReachable = true),
        )
        assertEquals(
            ControlsHint(ControlsFirstHint.TO_GOAL, 40, ControlsSecondHint.MAY_NOT_COMPLETE),
            controlsHint(pending.at(5000, RunStatus.PAUSED), isPhoneReachable = true),
        )
    }

    @Test
    fun `폰 끊김 안내가 서버 끊김보다 먼저다`() {
        // given
        val serverLost = party.copy(isServerConnected = false)

        // when & then
        assertEquals(
            ControlsHint(ControlsFirstHint.PHONE_CHECKING, second = ControlsSecondHint.CONTROLS_AFTER_RECONNECT),
            controlsHint(serverLost, isPhoneReachable = false),
        )
        assertEquals(
            ControlsHint(ControlsFirstHint.SERVER_CHECKING, second = ControlsSecondHint.RUN_RECORDING),
            controlsHint(serverLost, isPhoneReachable = true),
        )
    }

    @Test
    fun `혼자 달리면 서버가 끊겨도 서버 안내를 띄우지 않고, 목표 없는 솔로는 안내가 없다`() {
        // given
        val solo = RunPreviewSamples.solo.copy(isServerConnected = false)
        val soloMatch = party.copy(players = emptyList(), isServerConnected = false)

        // when & then
        assertEquals(ControlsHint(), controlsHint(solo, isPhoneReachable = true))
        assertEquals(
            ControlsHint(ControlsFirstHint.TO_PENALTY_LINE, 420, ControlsSecondHint.PENALTY_IF_SHORT),
            controlsHint(soloMatch.at(3580), isPhoneReachable = true),
        )
    }
}
