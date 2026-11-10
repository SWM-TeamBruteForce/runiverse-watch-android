package com.runiverse.app.run.common

import com.runiverse.app.phone.ComboKind
import com.runiverse.app.phone.Player

/** 울릴 진동이다. 꺼짐·켜짐을 번갈아 적은 [timingsMs]와 그때의 세기 [amplitudes]다. */
internal data class RunVibration(
    val timingsMs: List<Long>,
    val amplitudes: List<Int>,
    /** 급할수록 크다 — 울리는 중인 것보다 급하지 않은 진동은 버린다. */
    val priority: Int,
) {
    val durationMs: Long get() = timingsMs.sum()
}

// 보류는 할 일이 생긴 신호라 가장 급하고, 끊김은 다시 붙으라는 신호라 칭찬(눈금)보다 급하다
private const val TICK_PRIORITY = 0
private const val MILESTONE_PRIORITY = 1
private const val BREAK_PRIORITY = 2
private const val GOAL_PENDING_PRIORITY = 3

// 세기를 줄이면 달릴 때 안 느껴져 최대로 두고 길이로 가른다
private const val PULSE_AMPLITUDE = 255
private const val TICK_MS = 90L
private const val BREAK_MS = 180L
private const val MILESTONE_PULSE_MS = 90L
private const val MILESTONE_PULSE_GAP_MS = 120L

// 콤보의 짧은 여러 번과 구분되게 한 번만 길게 울린다
private const val GOAL_PENDING_MS = 300L

// 폰과 같다 — 두 번 울리고 넷부터는 한 번 더 울린다
private const val MILESTONE_PULSE_COUNT = 2
private const val EXTRA_PULSE_FROM_HEADCOUNT = 4

internal fun goalPendingVibration(): RunVibration = singlePulse(GOAL_PENDING_MS, GOAL_PENDING_PRIORITY)

internal fun comboVibration(kind: ComboKind, headcount: Int): RunVibration = when (kind) {
    ComboKind.TICK -> singlePulse(TICK_MS, TICK_PRIORITY)
    ComboKind.BREAK -> singlePulse(BREAK_MS, BREAK_PRIORITY)
    ComboKind.MILESTONE -> {
        val pulseCount = MILESTONE_PULSE_COUNT + if (headcount >= EXTRA_PULSE_FROM_HEADCOUNT) 1 else 0
        RunVibration(
            timingsMs = buildList {
                repeat(pulseCount) { pulse ->
                    add(if (pulse == 0) 0L else MILESTONE_PULSE_GAP_MS)
                    add(MILESTONE_PULSE_MS)
                }
            },
            amplitudes = buildList {
                repeat(pulseCount) {
                    add(0)
                    add(PULSE_AMPLITUDE)
                }
            },
            priority = MILESTONE_PRIORITY,
        )
    }
}

private fun singlePulse(durationMs: Long, priority: Int) =
    RunVibration(timingsMs = listOf(0L, durationMs), amplitudes = listOf(0, PULSE_AMPLITUDE), priority = priority)

/** 나와 콤보 중인 사람 수에 나를 더한다. 상태가 아직 안 와도 지금 순간의 그 사람은 센다. */
internal fun comboHeadcount(players: List<Player>, comboUserId: String): Int =
    players.count { it.comboCount > 0 || it.userId == comboUserId } + 1

/** 진동이 겹치지 않게 거른다 — 울리는 중인 것보다 급하지 않으면 버린다. */
internal class RunVibrator(
    private val play: (RunVibration) -> Unit,
    private val nowMs: () -> Long,
) {
    private var playingUntilMs = 0L
    private var playingPriority = 0

    fun vibrate(vibration: RunVibration) {
        val currentMs = nowMs()
        if (currentMs < playingUntilMs && vibration.priority <= playingPriority) return
        playingUntilMs = currentMs + vibration.durationMs
        playingPriority = vibration.priority
        play(vibration)
    }
}
