package com.runiverse.app.run

import com.runiverse.app.phone.FinishStage
import com.runiverse.app.phone.RunState
import com.runiverse.app.phone.RunStatus

/** 폰 연결이 판정한 명령 상태 중 조작 장이 쓰는 값이다. */
data class RunCommandState(
    val isLocked: Boolean = false,
    val isFinishPending: Boolean = false,
    val isOverdue: Boolean = false,
)

/** 조작 장 버튼이 폰으로 보내는 명령이다. */
class RunCommands(
    val pause: () -> Unit,
    val resume: () -> Unit,
    val finish: (forced: Boolean) -> Unit,
)

enum class FinishButtonStage { HOLD, TAP, FINISHING, CHECKING }

/** 달리는 중엔 길게 눌러야 끝난다. 멈췄으면 이미 한 번 멈춘 뜻이라 한 번에 끝낸다. */
fun finishButtonStage(run: RunState, commandState: RunCommandState): FinishButtonStage = when {
    commandState.isFinishPending && commandState.isOverdue -> FinishButtonStage.CHECKING
    commandState.isFinishPending || run.finish?.stage == FinishStage.FINISHING -> FinishButtonStage.FINISHING
    run.status == RunStatus.PAUSED -> FinishButtonStage.TAP
    else -> FinishButtonStage.HOLD
}

/** 폰이 서버 답을 기다리는 동안엔 폰도 명령을 무시하므로 같이 잠근다. */
fun areControlsLocked(run: RunState, isPhoneReachable: Boolean, commandState: RunCommandState): Boolean =
    !isPhoneReachable || commandState.isLocked || run.finish?.stage == FinishStage.FINISHING

/** 답을 금방 받는 잠금까지 흐리면 누를 때마다 깜빡인다 — 오래 걸리는 잠금만 흐린다. */
fun areControlsDimmed(run: RunState, isPhoneReachable: Boolean, commandState: RunCommandState): Boolean =
    !isPhoneReachable ||
        commandState.isFinishPending ||
        commandState.isOverdue ||
        run.finish?.stage == FinishStage.FINISHING

/** 폰 종료 버튼과 같다 — 화면 거리가 목표에 못 미치면 일찍 끝내는 것이다. 목표 없는 솔로는 해당하지 않는다. */
fun finishForced(run: RunState): Boolean {
    val target = run.targetDistanceMeters ?: return false
    return run.me.distanceMeters < target
}
