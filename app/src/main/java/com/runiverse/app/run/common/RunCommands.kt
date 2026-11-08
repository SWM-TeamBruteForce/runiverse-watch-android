package com.runiverse.app.run.common

data class RunCommandState(
    val isLocked: Boolean = false,
    val isFinishPending: Boolean = false,
    val isCommandOverdue: Boolean = false,
)

class RunCommands(
    val pause: () -> Unit,
    val resume: () -> Unit,
    val finish: (forced: Boolean) -> Unit,
)
