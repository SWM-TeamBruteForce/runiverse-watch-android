package com.runiverse.app.run.common

import com.runiverse.app.phone.ComboKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunVibrationTest {

    private val players = RunPreviewSamples.party.players

    @Test
    fun `틱과 끊김과 보류는 한 번 울리고 틱, 끊김, 보류 순으로 길다`() {
        // when
        val tick = comboVibration(ComboKind.TICK, headcount = 2)
        val broken = comboVibration(ComboKind.BREAK, headcount = 2)
        val pending = goalPendingVibration()

        // then
        listOf(tick, broken, pending).forEach { assertEquals(1, it.amplitudes.count { amplitude -> amplitude > 0 }) }
        assertTrue(tick.durationMs < broken.durationMs)
        assertTrue(broken.durationMs < pending.durationMs)
    }

    @Test
    fun `눈금은 셋까지 두 번, 넷부터 세 번 울린다`() {
        // when & then
        assertEquals(2, comboVibration(ComboKind.MILESTONE, headcount = 2).amplitudes.count { it > 0 })
        assertEquals(2, comboVibration(ComboKind.MILESTONE, headcount = 3).amplitudes.count { it > 0 })
        assertEquals(3, comboVibration(ComboKind.MILESTONE, headcount = 4).amplitudes.count { it > 0 })
    }

    @Test
    fun `눈금 파형은 꺼짐으로 시작해 켜짐과 번갈아 든다`() {
        // when
        val vibration = comboVibration(ComboKind.MILESTONE, headcount = 4)

        // then
        assertEquals(listOf(0L, 90L, 120L, 90L, 120L, 90L), vibration.timingsMs)
        assertEquals(listOf(0, 255, 0, 255, 0, 255), vibration.amplitudes)
    }

    @Test
    fun `급한 순서는 보류, 끊김, 눈금, 틱이다`() {
        // when
        val priorities = listOf(
            comboVibration(ComboKind.TICK, headcount = 2),
            comboVibration(ComboKind.MILESTONE, headcount = 2),
            comboVibration(ComboKind.BREAK, headcount = 2),
            goalPendingVibration(),
        ).map { it.priority }

        // then
        assertEquals(priorities.sorted(), priorities)
        assertEquals(priorities.distinct(), priorities)
    }

    @Test
    fun `콤보 인원은 콤보 중인 사람과 지금 순간의 사람에 나를 더한다`() {
        // when & then -> 콩나물(u2)만 콤보 중이라, 상태가 아직 안 온 완두콩(u1)의 순간이면 셋이다
        assertEquals(2, comboHeadcount(players, comboUserId = "u2"))
        assertEquals(3, comboHeadcount(players, comboUserId = "u1"))
    }

    @Test
    fun `울리는 중엔 급하지 않은 진동을 버린다`() {
        // given
        val played = mutableListOf<RunVibration>()
        var nowMs = 0L
        val vibrator = RunVibrator(play = { played += it }, nowMs = { nowMs })
        val milestone = comboVibration(ComboKind.MILESTONE, headcount = 2)
        val tick = comboVibration(ComboKind.TICK, headcount = 2)

        // when -> 눈금이 울리는 300ms 안에 틱과 또 다른 눈금이 온다
        vibrator.vibrate(milestone)
        nowMs = 100
        vibrator.vibrate(tick)
        vibrator.vibrate(milestone)

        // then
        assertEquals(listOf(milestone), played)
    }

    @Test
    fun `울리는 중이라도 더 급한 진동은 울린다`() {
        // given
        val played = mutableListOf<RunVibration>()
        val vibrator = RunVibrator(play = { played += it }, nowMs = { 0L })
        val tick = comboVibration(ComboKind.TICK, headcount = 2)
        val broken = comboVibration(ComboKind.BREAK, headcount = 2)

        // when
        vibrator.vibrate(tick)
        vibrator.vibrate(broken)

        // then
        assertEquals(listOf(tick, broken), played)
    }

    @Test
    fun `다 울린 뒤엔 덜 급한 진동도 울린다`() {
        // given
        val played = mutableListOf<RunVibration>()
        var nowMs = 0L
        val vibrator = RunVibrator(play = { played += it }, nowMs = { nowMs })
        val pending = goalPendingVibration()
        val tick = comboVibration(ComboKind.TICK, headcount = 2)

        // when
        vibrator.vibrate(pending)
        nowMs = pending.durationMs
        vibrator.vibrate(tick)

        // then
        assertEquals(listOf(pending, tick), played)
    }
}
