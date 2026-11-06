package com.runiverse.app.run

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import androidx.wear.compose.ui.tooling.preview.WearPreviewSmallRound
import com.runiverse.app.R
import com.runiverse.app.phone.MyRecord
import com.runiverse.app.phone.RunState
import com.runiverse.app.ui.theme.SELF_LANE_COLOR_SLOT
import com.runiverse.app.ui.theme.SUIT_DESCENDER_INK
import com.runiverse.app.ui.theme.SUIT_NUMERAL_INK
import com.runiverse.app.ui.theme.laneColor
import com.runiverse.app.ui.theme.tabularNumerals

private const val WIDEST_CADENCE = "888"
private const val WIDEST_CALORIES = "8888"

@Composable
internal fun MyRecordPage(run: RunState, isPhoneReachable: Boolean, heartRateBpm: Int?, heartRateZone: Int?) {
    val slots = currentRunPageSlots()
    // 심박은 워치가 재는 값이라 폰이 끊겨도 흐리지 않는다
    val phoneValuesAlpha = if (isPhoneReachable) 1f else RunDimensions.STALE_CONTENT_ALPHA
    val heartRateStyle = MaterialTheme.typography.numeralExtraLarge.tabularNumerals()
    val unitSize = MaterialTheme.typography.labelSmall.fontSize
    val (barTop, rowsTop) = with(LocalDensity.current) {
        // 막대는 숫자가 아니라 `bpm`의 p 꼬리 아래에서 잰다
        val heartRateBottom = slots.bigNumberBaseline + unitSize.toPx() * SUIT_DESCENDER_INK.belowBaselineEm
        val barTop = heartRateBottom + RunDimensions.ZONE_BAR_GAP.toPx()
        val groupGap = RunDimensions.GROUP_GAP.toPx()
        val rowsTop = if (heartRateZone != null) {
            barTop + RunDimensions.ZONE_BAR_HEIGHT.toPx() + groupGap
        } else {
            heartRateBottom + groupGap
        }
        barTop to rowsTop
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        distanceProgress(run.me.distanceMeters, run.targetDistanceMeters)?.let {
            ProgressRing(it, laneColor(SELF_LANE_COLOR_SLOT), Modifier.alpha(phoneValuesAlpha))
        }
        if (!isPhoneReachable) {
            AlignedText(
                stringResource(R.string.run_phone_checking),
                MaterialTheme.typography.bodyMedium,
                MaterialTheme.colorScheme.onSurface,
                Modifier.contentBottomAt(slots.topRowBottom),
            )
        }
        ValueWithUnit(
            heartRateBpm?.toString() ?: stringResource(R.string.run_value_unknown),
            stringResource(R.string.unit_bpm),
            heartRateStyle,
            Modifier.baselineAt(slots.bigNumberBaseline),
            valueColor = MaterialTheme.colorScheme.onBackground,
        )
        if (heartRateZone != null) {
            HeartRateZoneBar(
                heartRateZone,
                Modifier.contentTopAt(barTop).width(widestHeartRateBlockWidth(heartRateStyle)),
            )
        }
        RecordRows(run.me, rowsTop, Modifier.contentTopAt(rowsTop).alpha(phoneValuesAlpha))
    }
}

// 줄 수는 지금 값이 아니라 가장 넓은 값으로 정한다 — 달리는 중 칼로리가 늘어도 모양이 안 바뀐다
@Composable
private fun RecordRows(me: MyRecord, rowsTop: Float, modifier: Modifier) {
    val style = MaterialTheme.typography.numeralSmall.tabularNumerals()
    val unknown = stringResource(R.string.run_value_unknown)
    val spmUnit = stringResource(R.string.unit_spm)
    val kcalUnit = stringResource(R.string.unit_kcal)
    val cadence: @Composable () -> Unit = { ValueWithUnit(me.cadenceSpm?.toString() ?: unknown, spmUnit, style) }
    val calories: @Composable () -> Unit = { ValueWithUnit(me.caloriesKcal?.toString() ?: unknown, kcalUnit, style) }
    if (fitsOneLine(rowsTop, style, spmUnit, kcalUnit)) {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(RunDimensions.GROUP_GAP)) {
            cadence()
            calories()
        }
    } else {
        ContentBoundsColumn(gap = RunDimensions.LINE_GAP, modifier = modifier) {
            cadence()
            calories()
        }
    }
}

@Composable
private fun fitsOneLine(rowsTop: Float, style: TextStyle, spmUnit: String, kcalUnit: String): Boolean {
    val unitStyle = MaterialTheme.typography.labelSmall
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val screenWidth = LocalWindowInfo.current.containerSize.width.toFloat()
    return remember(rowsTop, style, unitStyle, spmUnit, kcalUnit, density, screenWidth) {
        with(density) {
            fun width(text: String, textStyle: TextStyle) = measurer.measure(text, textStyle).size.width
            val widestRow = width(WIDEST_CADENCE, style) + width(spmUnit, unitStyle) +
                width(WIDEST_CALORIES, style) + width(kcalUnit, unitStyle) +
                RunDimensions.VALUE_UNIT_GAP.toPx() * 2 + RunDimensions.GROUP_GAP.toPx()
            val firstBaseline = rowsTop + style.fontSize.toPx() * SUIT_NUMERAL_INK.aboveBaselineEm
            widestRow <= usableWidthAt(firstBaseline, screenWidth, RunDimensions.TEXT_EDGE_MARGIN.toPx())
        }
    }
}

@WearPreviewLargeRound
@WearPreviewSmallRound
@Composable
private fun MyRecordPagePreview() = RunPreviewFrame {
    MyRecordPage(RunPreviewSamples.party, isPhoneReachable = true, heartRateBpm = 148, heartRateZone = 3)
}

@WearPreviewLargeRound
@Composable
private fun MyRecordPagePhoneLostPreview() = RunPreviewFrame {
    MyRecordPage(RunPreviewSamples.party, isPhoneReachable = false, heartRateBpm = 148, heartRateZone = 3)
}

@WearPreviewLargeRound
@Composable
private fun MyRecordPageNoHeartRatePreview() = RunPreviewFrame {
    MyRecordPage(
        RunPreviewSamples.party.copy(me = RunPreviewSamples.party.me.copy(cadenceSpm = null, caloriesKcal = null)),
        isPhoneReachable = true,
        heartRateBpm = null,
        heartRateZone = 0,
    )
}
