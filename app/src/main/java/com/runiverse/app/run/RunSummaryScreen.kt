package com.runiverse.app.run

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import androidx.wear.compose.ui.tooling.preview.WearPreviewSmallRound
import com.runiverse.app.R
import com.runiverse.app.phone.RunResult
import com.runiverse.app.phone.RunState
import com.runiverse.app.run.common.RunPreviewSamples
import com.runiverse.app.run.common.currentRunPageSlots
import com.runiverse.app.run.common.currentScreenWidth
import com.runiverse.app.run.common.formatElapsedTime
import com.runiverse.app.run.common.formatPace
import com.runiverse.app.run.common.formatRunDistanceKm
import com.runiverse.app.run.component.AlignedText
import com.runiverse.app.run.component.BOTTOM_PILL_BOTTOM_PADDING
import com.runiverse.app.run.component.ControlButton
import com.runiverse.app.run.component.ControlIcon
import com.runiverse.app.run.component.RunDimensions
import com.runiverse.app.run.component.ValueWithUnit
import com.runiverse.app.run.component.baselineAt
import com.runiverse.app.run.component.bottomPillWidth
import com.runiverse.app.run.component.contentBottomAt
import com.runiverse.app.run.component.contentTopAt
import com.runiverse.app.ui.component.PreviewFrame

private const val TIME_PACE_ROW_WIDTH_FRACTION = 270f / 480f
private val MIN_TIME_TO_PACE_GAP = 10.dp

/** 받은 결과는 더 안 바뀌어 폰이 끊겨도 멈춘 것으로 보지 않는다. */
fun isResultStuck(result: RunResult?, isPhoneReachable: Boolean): Boolean = result == null && !isPhoneReachable

@Composable
internal fun RunSummaryScreen(run: RunState, isPhoneReachable: Boolean, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    ScreenScaffold {
        RunSummaryContent(run.result, isPhoneReachable, onClose)
    }
}

@Composable
private fun RunSummaryContent(result: RunResult?, isPhoneReachable: Boolean, onClose: () -> Unit) {
    val slots = currentRunPageSlots()
    val unknown = stringResource(R.string.run_value_unknown)
    val isStuck = isResultStuck(result, isPhoneReachable)
    val valuesAlpha = if (isStuck) RunDimensions.STALE_CONTENT_ALPHA else 1f
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        if (isStuck) {
            AlignedText(
                stringResource(R.string.run_phone_checking),
                MaterialTheme.typography.bodyMedium,
                MaterialTheme.colorScheme.onSurface,
                Modifier.contentBottomAt(slots.topRowBottom),
            )
        } else {
            AlignedText(
                stringResource(R.string.run_summary_completed),
                MaterialTheme.typography.bodyMedium,
                MaterialTheme.colorScheme.onSurfaceVariant,
                Modifier.contentBottomAt(slots.topRowBottom),
            )
        }
        ValueWithUnit(
            result?.let { formatRunDistanceKm(it.totalDistanceMeters) } ?: unknown,
            stringResource(R.string.unit_km),
            MaterialTheme.typography.numeralExtraLarge,
            Modifier.baselineAt(slots.bigNumberBaseline).alpha(valuesAlpha),
            valueColor = MaterialTheme.colorScheme.onBackground,
        )
        TimePaceRow(
            result?.let { formatElapsedTime(it.elapsedSeconds) } ?: unknown,
            result?.let { formatPace(it.averagePaceSecondsPerKm) } ?: unknown,
            Modifier.contentTopAt(slots.belowRowTop).alpha(valuesAlpha),
        )
        ControlButton(
            stringResource(R.string.run_summary_close),
            ControlIcon.CHECK,
            MaterialTheme.colorScheme.outlineVariant,
            bottomPillWidth(currentScreenWidth()),
            isEnabled = true,
            Modifier.align(Alignment.BottomCenter).padding(bottom = BOTTOM_PILL_BOTTOM_PADDING),
            onClick = onClose,
        )
    }
}

@Composable
private fun TimePaceRow(time: String, pace: String, modifier: Modifier) {
    val style = MaterialTheme.typography.numeralSmall
    val color = MaterialTheme.colorScheme.onSurface
    Row(
        modifier.widthIn(min = currentScreenWidth() * TIME_PACE_ROW_WIDTH_FRACTION),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AlignedText(time, style, color)
        Spacer(Modifier.width(MIN_TIME_TO_PACE_GAP))
        AlignedText(pace, style, color)
    }
}

@WearPreviewLargeRound
@WearPreviewSmallRound
@WearPreviewFontScales
@Composable
private fun RunSummaryScreenConfirmedPreview() = PreviewFrame {
    RunSummaryContent(RunPreviewSamples.result, isPhoneReachable = true, onClose = {})
}

@WearPreviewLargeRound
@WearPreviewSmallRound
@WearPreviewFontScales
@Composable
private fun RunSummaryScreenLongValuesPreview() = PreviewFrame {
    RunSummaryContent(RunPreviewSamples.longResult, isPhoneReachable = true, onClose = {})
}

@WearPreviewLargeRound
@Composable
private fun RunSummaryScreenConfirmedPhoneLostPreview() = PreviewFrame {
    RunSummaryContent(RunPreviewSamples.result, isPhoneReachable = false, onClose = {})
}

@WearPreviewLargeRound
@Composable
private fun RunSummaryScreenPendingPreview() = PreviewFrame {
    RunSummaryContent(result = null, isPhoneReachable = true, onClose = {})
}

@WearPreviewLargeRound
@Composable
private fun RunSummaryScreenPhoneLostPreview() = PreviewFrame {
    RunSummaryContent(result = null, isPhoneReachable = false, onClose = {})
}
