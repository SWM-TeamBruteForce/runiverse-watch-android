package com.runiverse.app.run.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.runiverse.app.R
import com.runiverse.app.phone.RunState
import com.runiverse.app.phone.RunStatus

@Composable
internal fun connectionNotice(run: RunState, isPhoneReachable: Boolean): String? = when {
    !isPhoneReachable -> stringResource(R.string.run_phone_checking)
    !run.isServerConnected && run.players.isNotEmpty() -> stringResource(R.string.run_server_checking)
    else -> null
}

@Composable
internal fun playerStatusWord(status: RunStatus): String? = when (status) {
    RunStatus.PAUSED -> stringResource(R.string.run_status_paused)
    RunStatus.DISCONNECTED -> stringResource(R.string.run_status_disconnected)
    RunStatus.FINISHED -> stringResource(R.string.run_status_finished)
    RunStatus.RUNNING -> null
}

@Composable
internal fun distanceUnitText(unit: DistanceUnitLabel): String = when (unit) {
    DistanceUnitLabel.METER -> stringResource(R.string.unit_m)
    DistanceUnitLabel.KILOMETER -> stringResource(R.string.unit_km)
    DistanceUnitLabel.SHORT_KILOMETER -> stringResource(R.string.unit_short_km)
}
