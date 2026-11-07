package com.runiverse.app.run

import com.runiverse.app.phone.FinishStage
import com.runiverse.app.phone.RunState
import com.runiverse.app.phone.RunStatus

enum class ControlsFirstHint { PHONE_CHECKING, SERVER_CHECKING, CHECKING_DISTANCE, TO_PENALTY_LINE, TO_GOAL }

enum class ControlsSecondHint {
    CONTROLS_AFTER_RECONNECT,
    RECORDING,
    KEEP_RUNNING_IF_SHORT,
    PENALTY_IF_SHORT,
    PENALTY_IF_FINISHED,
    MAY_NOT_COMPLETE,
}

/** [remainingMeters]는 [first]가 남은 거리를 말할 때만 있다. */
data class ControlsHint(
    val first: ControlsFirstHint? = null,
    val remainingMeters: Int? = null,
    val second: ControlsSecondHint? = null,
)

private const val PENALTY_LINE_PERCENT = 80
private const val METERS_PER_KM = 1000
private const val METERS_PER_TENTH_KM = 100

/** 서버가 답한 남은 거리를 화면 거리보다 먼저 쓴다. */
fun controlsHint(run: RunState, isPhoneReachable: Boolean): ControlsHint {
    val isPaused = run.status == RunStatus.PAUSED
    val target = run.targetDistanceMeters
    val distance = run.me.distanceMeters
    val serverRemaining = run.finish?.takeIf { it.stage == FinishStage.GOAL_PENDING }?.remainingMeters
    return when {
        !isPhoneReachable ->
            ControlsHint(ControlsFirstHint.PHONE_CHECKING, second = ControlsSecondHint.CONTROLS_AFTER_RECONNECT)
        !run.isServerConnected && run.players.isNotEmpty() ->
            ControlsHint(ControlsFirstHint.SERVER_CHECKING, second = ControlsSecondHint.RECORDING)
        serverRemaining != null -> toGoalHint(serverRemaining, isPaused)
        target == null -> ControlsHint()
        distance >= target -> ControlsHint(
            ControlsFirstHint.CHECKING_DISTANCE,
            second = if (isPaused) null else ControlsSecondHint.KEEP_RUNNING_IF_SHORT,
        )
        run.isPenaltyApplicable && distance < target * PENALTY_LINE_PERCENT / 100 -> ControlsHint(
            ControlsFirstHint.TO_PENALTY_LINE,
            target * PENALTY_LINE_PERCENT / 100 - distance,
            if (isPaused) ControlsSecondHint.PENALTY_IF_FINISHED else ControlsSecondHint.PENALTY_IF_SHORT,
        )
        else -> toGoalHint(target - distance, isPaused)
    }
}

private fun toGoalHint(remainingMeters: Int, isPaused: Boolean) = ControlsHint(
    ControlsFirstHint.TO_GOAL,
    remainingMeters,
    if (isPaused) ControlsSecondHint.MAY_NOT_COMPLETE else null,
)

/** 남은 거리를 적게 말하지 않게 폰처럼 올린다. */
fun formatRemainingDistance(meters: Int): FormattedDistance {
    if (meters < METERS_PER_KM) return FormattedDistance("$meters", DistanceUnitLabel.METER)
    val tenthsOfKm = (meters + METERS_PER_TENTH_KM - 1) / METERS_PER_TENTH_KM
    return FormattedDistance("${tenthsOfKm / 10}.${tenthsOfKm % 10}", DistanceUnitLabel.KILOMETER)
}
