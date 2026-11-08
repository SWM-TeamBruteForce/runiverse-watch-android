package com.runiverse.app.run.controls

import com.runiverse.app.phone.FinishStage
import com.runiverse.app.phone.RunState
import com.runiverse.app.phone.RunStatus
import com.runiverse.app.run.common.RunCommandState

enum class FinishButtonStage { HOLD, TAP, FINISHING, CHECKING }

/** 멈췄으면 이미 한 번 멈춘 뜻이라 한 번에 끝낸다. */
fun finishButtonStage(run: RunState, commandState: RunCommandState): FinishButtonStage = when {
    commandState.isFinishPending && commandState.isCommandOverdue -> FinishButtonStage.CHECKING
    commandState.isFinishPending || run.finish?.stage == FinishStage.FINISHING -> FinishButtonStage.FINISHING
    run.status == RunStatus.PAUSED -> FinishButtonStage.TAP
    else -> FinishButtonStage.HOLD
}

/** 폰이 서버 답을 기다리는 동안엔 폰도 명령을 무시한다. */
fun areControlsLocked(run: RunState, isPhoneReachable: Boolean, commandState: RunCommandState): Boolean =
    !isPhoneReachable || commandState.isLocked || run.finish?.stage == FinishStage.FINISHING

/** 답을 금방 받는 잠금까지 흐리면 누를 때마다 깜빡인다. */
fun areControlsDimmed(run: RunState, isPhoneReachable: Boolean, commandState: RunCommandState): Boolean =
    !isPhoneReachable ||
        commandState.isFinishPending ||
        commandState.isCommandOverdue ||
        run.finish?.stage == FinishStage.FINISHING

/** 폰 종료 버튼과 같은 규칙이다. */
fun isFinishForced(run: RunState): Boolean {
    val targetDistanceMeters = run.targetDistanceMeters ?: return false
    return run.me.distanceMeters < targetDistanceMeters
}
