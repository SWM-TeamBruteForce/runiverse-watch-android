package com.runiverse.app.run

import com.runiverse.app.phone.FinishStage
import com.runiverse.app.phone.RunState
import com.runiverse.app.phone.RunStatus

enum class ControlsFirstHint { PHONE_CHECKING, SERVER_CHECKING, DISTANCE_CHECKING, TO_PENALTY_LINE, TO_GOAL }

enum class ControlsSecondHint {
    CONTROLS_AFTER_RECONNECT,
    RUN_RECORDING,
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

/** 서버가 답한 남은 거리를 화면 거리보다 먼저 쓴다. */
fun controlsHint(run: RunState, isPhoneReachable: Boolean): ControlsHint {
    val isPaused = run.status == RunStatus.PAUSED
    val targetDistanceMeters = run.targetDistanceMeters
    val distanceMeters = run.me.distanceMeters
    val serverRemainingMeters = run.finish?.takeIf { it.stage == FinishStage.GOAL_PENDING }?.remainingMeters
    return when {
        !isPhoneReachable ->
            ControlsHint(ControlsFirstHint.PHONE_CHECKING, second = ControlsSecondHint.CONTROLS_AFTER_RECONNECT)
        !run.isServerConnected && run.players.isNotEmpty() ->
            ControlsHint(ControlsFirstHint.SERVER_CHECKING, second = ControlsSecondHint.RUN_RECORDING)
        serverRemainingMeters != null -> toGoalHint(serverRemainingMeters, isPaused)
        targetDistanceMeters == null -> ControlsHint()
        distanceMeters >= targetDistanceMeters -> ControlsHint(
            ControlsFirstHint.DISTANCE_CHECKING,
            second = if (isPaused) null else ControlsSecondHint.KEEP_RUNNING_IF_SHORT,
        )
        run.isPenaltyApplicable && distanceMeters < targetDistanceMeters * PENALTY_LINE_PERCENT / 100 -> ControlsHint(
            ControlsFirstHint.TO_PENALTY_LINE,
            targetDistanceMeters * PENALTY_LINE_PERCENT / 100 - distanceMeters,
            if (isPaused) ControlsSecondHint.PENALTY_IF_FINISHED else ControlsSecondHint.PENALTY_IF_SHORT,
        )
        else -> toGoalHint(targetDistanceMeters - distanceMeters, isPaused)
    }
}

private fun toGoalHint(remainingMeters: Int, isPaused: Boolean) = ControlsHint(
    ControlsFirstHint.TO_GOAL,
    remainingMeters,
    if (isPaused) ControlsSecondHint.MAY_NOT_COMPLETE else null,
)
