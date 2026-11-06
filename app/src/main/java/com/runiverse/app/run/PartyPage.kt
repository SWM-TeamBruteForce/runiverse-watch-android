package com.runiverse.app.run

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import androidx.wear.compose.ui.tooling.preview.WearPreviewSmallRound
import com.runiverse.app.R
import com.runiverse.app.phone.Player
import com.runiverse.app.phone.RunState
import com.runiverse.app.ui.theme.SUIT_HANGUL_INK
import com.runiverse.app.ui.theme.SUIT_NUMERAL_INK
import com.runiverse.app.ui.theme.laneColor
import com.runiverse.app.ui.theme.tabularNumerals

private const val WIDEST_PACE = "88'88\""
private val LABEL_TO_PACE_GAP = 3.dp
private val BETWEEN_PACES_GAP = 8.dp
private val DISTANCE_TO_NAME_MIN_GAP = 8.dp

@Composable
internal fun PartyPage(run: RunState, player: Player) {
    val slots = currentRunPageSlots()
    val paceStyle = MaterialTheme.typography.numeralSmall
    val paceFitsOneLine = paceFitsOneLine(slots.belowRowTop, paceStyle)
    val comboTop = with(LocalDensity.current) {
        val paceInk = paceStyle.fontSize.toPx() * SUIT_NUMERAL_INK.aboveBaselineEm
        val labelDescent = MaterialTheme.typography.labelSmall.fontSize.toPx() * SUIT_HANGUL_INK.belowBaselineEm
        val paceBottom = if (paceFitsOneLine) {
            slots.belowRowTop + paceInk + labelDescent
        } else {
            slots.belowRowTop + paceInk * 2 + RunDimensions.LINE_GAP.toPx() + labelDescent
        }
        // 페이스가 두 줄이면 맨 아래 줄 자리까지 내려오므로 페이스 바로 아래로 간다
        maxOf(slots.bottomRowTop, paceBottom + RunDimensions.LINE_GAP.toPx())
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        distanceProgress(player.distanceMeters, run.targetDistanceMeters)?.let {
            ProgressRing(it, laneColor(player.colorSlot))
        }
        DistanceNameRow(player, Modifier.contentBottomAt(slots.topRowBottom))
        GapOrStatus(player, slots)
        PaceRows(
            player,
            run.me.currentPaceSecondsPerKm,
            paceFitsOneLine,
            Modifier.contentTopAt(slots.belowRowTop),
        )
        AlignedText(
            if (player.comboCount > 0) {
                stringResource(R.string.run_combo_current, player.comboCount)
            } else {
                stringResource(R.string.run_combo_best, player.maxComboCount)
            },
            MaterialTheme.typography.labelSmall,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Modifier.contentTopAt(comboTop),
        )
    }
}

// 거리와 이름은 글자 종류·크기가 달라 기준선 대신 보이는 글자의 세로 가운데를 맞춘다
@Composable
private fun DistanceNameRow(player: Player, modifier: Modifier) {
    val abbreviateUnits = LocalDensity.current.fontScale > 1f
    Row(
        modifier.fillMaxWidth(1f - RunDimensions.TOP_ROW_SIDE_FRACTION * 2),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ValueWithUnit(
            formatRunDistanceKm(player.distanceMeters),
            if (abbreviateUnits) null else stringResource(R.string.unit_km),
            MaterialTheme.typography.numeralSmall,
            Modifier.alignBy(Measured::contentCenter),
        )
        AlignedText(
            player.nickname,
            MaterialTheme.typography.bodyMedium,
            MaterialTheme.colorScheme.onSurface,
            Modifier.alignBy(Measured::contentCenter)
                .weight(1f, fill = false)
                .padding(start = DISTANCE_TO_NAME_MIN_GAP),
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun Measured.contentCenter(): Int = (this[CONTENT_TOP] + this[CONTENT_BOTTOM]) / 2

@Composable
private fun GapOrStatus(player: Player, slots: RunPageSlots) {
    val color = MaterialTheme.colorScheme.onBackground
    val statusWord = playerStatusWord(player.status)
    if (statusWord != null) {
        AlignedText(statusWord, MaterialTheme.typography.numeralLarge, color, Modifier.contentCenterAt(slots.center))
        return
    }
    val style = MaterialTheme.typography.numeralExtraLarge
    val gapMeters = player.gapMeters
    if (gapMeters == null) {
        // `0m`과 같은 꼴이라 통지가 와도 덩어리 가운데가 안 움직인다
        ValueWithUnit(
            stringResource(R.string.run_value_unknown),
            stringResource(R.string.unit_m),
            style,
            Modifier.baselineAt(slots.bigNumberBaseline),
            valueColor = color,
        )
        return
    }
    val gap = formatGapDistance(gapMeters, abbreviateUnits = false)
    val directionWord = when (gapDirection(gapMeters)) {
        GapDirection.AHEAD -> stringResource(R.string.run_gap_ahead)
        GapDirection.BEHIND -> stringResource(R.string.run_gap_behind)
        GapDirection.LEVEL -> null
    }
    Row(
        Modifier.baselineAt(slots.bigNumberBaseline),
        horizontalArrangement = Arrangement.spacedBy(RunDimensions.VALUE_UNIT_GAP),
    ) {
        if (directionWord != null) {
            AlignedText(
                directionWord,
                MaterialTheme.typography.bodyMedium,
                MaterialTheme.colorScheme.onSurfaceVariant,
                Modifier.alignByBaseline(),
            )
        }
        ValueWithUnit(
            gap.value,
            gap.unit?.let { distanceUnitText(it) },
            style,
            Modifier.alignByBaseline(),
            valueColor = color,
        )
    }
}

@Composable
private fun PaceRows(player: Player, myPaceSecondsPerKm: Int?, fitsOneLine: Boolean, modifier: Modifier) {
    val partnerPace: @Composable () -> Unit = {
        LabeledPace(
            stringResource(R.string.run_pace_partner),
            player.currentPaceSecondsPerKm,
            MaterialTheme.typography.numeralSmall,
        )
    }
    val myPace: @Composable () -> Unit = {
        LabeledPace(
            stringResource(R.string.run_pace_me),
            myPaceSecondsPerKm,
            MaterialTheme.typography.numeralSmall.tabularNumerals(),
        )
    }
    if (fitsOneLine) {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(BETWEEN_PACES_GAP)) {
            partnerPace()
            myPace()
        }
    } else {
        ContentBoundsColumn(gap = RunDimensions.LINE_GAP, modifier = modifier) {
            partnerPace()
            myPace()
        }
    }
}

@Composable
private fun LabeledPace(label: String, secondsPerKm: Int?, style: TextStyle) {
    Row(horizontalArrangement = Arrangement.spacedBy(LABEL_TO_PACE_GAP)) {
        AlignedText(
            label,
            MaterialTheme.typography.labelSmall,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Modifier.alignByBaseline(),
        )
        AlignedText(formatPace(secondsPerKm), style, MaterialTheme.colorScheme.onSurface, Modifier.alignByBaseline())
    }
}

// 줄 수는 지금 값이 아니라 가장 넓은 값으로 정한다 — 달리는 중 페이스가 바뀌어도 모양이 안 흔들린다
@Composable
private fun paceFitsOneLine(rowTop: Float, paceStyle: TextStyle): Boolean {
    val labelStyle = MaterialTheme.typography.labelSmall
    val partnerLabel = stringResource(R.string.run_pace_partner)
    val myLabel = stringResource(R.string.run_pace_me)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val screenWidth = LocalWindowInfo.current.containerSize.width.toFloat()
    return remember(rowTop, paceStyle, labelStyle, partnerLabel, myLabel, density, screenWidth) {
        with(density) {
            fun width(text: String, textStyle: TextStyle) = measurer.measure(text, textStyle).size.width
            val widestRow = width(partnerLabel, labelStyle) + width(myLabel, labelStyle) +
                width(WIDEST_PACE, paceStyle.tabularNumerals()) * 2 +
                LABEL_TO_PACE_GAP.toPx() * 2 + BETWEEN_PACES_GAP.toPx()
            val baseline = rowTop + paceStyle.fontSize.toPx() * SUIT_NUMERAL_INK.aboveBaselineEm
            widestRow <= usableWidthAt(baseline, screenWidth, RunDimensions.TEXT_EDGE_MARGIN.toPx())
        }
    }
}

@WearPreviewLargeRound
@WearPreviewSmallRound
@Composable
private fun PartyPageAheadPreview() = RunPreviewFrame {
    PartyPage(RunPreviewSamples.party, RunPreviewSamples.party.players[0])
}

@WearPreviewLargeRound
@Composable
private fun PartyPageBehindComboPreview() = RunPreviewFrame {
    PartyPage(RunPreviewSamples.party, RunPreviewSamples.party.players[1])
}

@WearPreviewLargeRound
@Composable
private fun PartyPagePausedPreview() = RunPreviewFrame {
    PartyPage(RunPreviewSamples.party, RunPreviewSamples.party.players[2])
}

@WearPreviewLargeRound
@Composable
private fun PartyPageFinishedPreview() = RunPreviewFrame {
    PartyPage(RunPreviewSamples.party, RunPreviewSamples.finishedPlayer)
}

@WearPreviewLargeRound
@Composable
private fun PartyPageNotNotifiedPreview() = RunPreviewFrame {
    PartyPage(RunPreviewSamples.party, RunPreviewSamples.notNotifiedPlayer)
}

@WearPreviewLargeRound
@WearPreviewSmallRound
@Composable
private fun PartyPageLongestPreview() = RunPreviewFrame {
    PartyPage(RunPreviewSamples.longValues, RunPreviewSamples.longestPlayer)
}
