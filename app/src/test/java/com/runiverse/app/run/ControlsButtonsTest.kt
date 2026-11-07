package com.runiverse.app.run

import com.runiverse.app.phone.FinishProgress
import com.runiverse.app.phone.FinishStage
import com.runiverse.app.phone.RunStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlsButtonsTest {

    private val running = RunPreviewSamples.party.copy(status = RunStatus.RUNNING, finish = null)
    private val finishing = running.copy(finish = FinishProgress(FinishStage.FINISHING))

    @Test
    fun `달리는 중엔 길게 눌러야 끝나고 멈췄으면 한 번에 끝난다`() {
        // when & then
        assertEquals(FinishButtonStage.HOLD, finishButtonStage(running, RunCommandState()))
        assertEquals(
            FinishButtonStage.TAP,
            finishButtonStage(running.copy(status = RunStatus.PAUSED), RunCommandState()),
        )
    }

    @Test
    fun `서버 보류 중엔 다시 끝낼 수 있게 버튼이 돌아온다`() {
        // given
        val pending = running.copy(finish = FinishProgress(FinishStage.GOAL_PENDING, remainingMeters = 40))

        // when & then
        assertEquals(FinishButtonStage.HOLD, finishButtonStage(pending, RunCommandState()))
        assertFalse(areControlsLocked(pending, isPhoneReachable = true, RunCommandState()))
    }

    @Test
    fun `종료를 보내면 종료 중이고 3초 넘게 답이 없으면 종료 확인 중이다`() {
        // when & then
        assertEquals(
            FinishButtonStage.FINISHING,
            finishButtonStage(running, RunCommandState(isLocked = true, isFinishPending = true)),
        )
        assertEquals(
            FinishButtonStage.CHECKING,
            finishButtonStage(
                running,
                RunCommandState(isLocked = true, isFinishPending = true, isCommandOverdue = true),
            ),
        )
    }

    @Test
    fun `멈춘 상태에서 종료를 보내도 종료 중으로 바뀐다`() {
        // given
        val paused = running.copy(status = RunStatus.PAUSED)

        // when & then
        assertEquals(
            FinishButtonStage.FINISHING,
            finishButtonStage(paused, RunCommandState(isLocked = true, isFinishPending = true)),
        )
    }

    @Test
    fun `일시정지에 답이 늦어도 종료 문구를 띄우지 않는다`() {
        // when & then
        assertEquals(
            FinishButtonStage.HOLD,
            finishButtonStage(running, RunCommandState(isLocked = true, isCommandOverdue = true)),
        )
    }

    @Test
    fun `폰이 서버 답을 기다리면 폰에서 누른 종료여도 종료 중으로 잠근다`() {
        // when & then
        assertEquals(FinishButtonStage.FINISHING, finishButtonStage(finishing, RunCommandState()))
        assertTrue(areControlsLocked(finishing, isPhoneReachable = true, RunCommandState()))
    }

    @Test
    fun `폰이 끊기거나 명령 답을 기다리면 잠근다`() {
        // when & then
        assertFalse(areControlsLocked(running, isPhoneReachable = true, RunCommandState()))
        assertTrue(areControlsLocked(running, isPhoneReachable = false, RunCommandState()))
        assertTrue(areControlsLocked(running, isPhoneReachable = true, RunCommandState(isLocked = true)))
    }

    @Test
    fun `일시정지 답을 기다리는 동안은 잠그되 흐리지 않고, 3초가 넘으면 흐린다`() {
        // given
        val waiting = RunCommandState(isLocked = true)

        // when & then
        assertTrue(areControlsLocked(running, isPhoneReachable = true, waiting))
        assertFalse(areControlsDimmed(running, isPhoneReachable = true, waiting))
        assertTrue(areControlsDimmed(running, isPhoneReachable = true, waiting.copy(isCommandOverdue = true)))
    }

    @Test
    fun `오래 걸리는 잠금은 흐린다`() {
        // given
        val finishPending = RunCommandState(isLocked = true, isFinishPending = true)

        // when & then
        assertFalse(areControlsDimmed(running, isPhoneReachable = true, RunCommandState()))
        assertTrue(areControlsDimmed(running, isPhoneReachable = false, RunCommandState()))
        assertTrue(areControlsDimmed(finishing, isPhoneReachable = true, RunCommandState()))
        assertTrue(areControlsDimmed(running, isPhoneReachable = true, finishPending))
    }

    @Test
    fun `화면 거리가 목표에 못 미칠 때만 일찍 끝내는 종료다`() {
        // given
        val atDistance = { meters: Int -> running.copy(me = running.me.copy(distanceMeters = meters)) }

        // when & then
        assertTrue(isFinishForced(atDistance(4999).copy(targetDistanceMeters = 5000)))
        assertFalse(isFinishForced(atDistance(5000).copy(targetDistanceMeters = 5000)))
        assertFalse(isFinishForced(RunPreviewSamples.solo))
    }
}
