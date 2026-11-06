package com.runiverse.app.run

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
private val MIN_DISTANCE_TO_NAME_GAP = 8.dp

@Composable
internal fun PartyPage(run: RunState, player: Player, isPhoneReachable: Boolean) {
    val slots = currentRunPageSlots()
    val notice = connectionNotice(run, isPhoneReachable)
    val playerValuesAlpha = if (player.hasStaleValues(run, isPhoneReachable)) RunDimensions.STALE_CONTENT_ALPHA else 1f
    // 서버가 끊겨도 내 페이스는 폰이 계속 재는 값이라 폰이 끊길 때만 흐린다
    val phoneValuesAlpha = if (isPhoneReachable) 1f else RunDimensions.STALE_CONTENT_ALPHA
    val paceStyle = MaterialTheme.typography.numeralSmall
    val playerLabel = stringResource(R.string.run_pace_player)
    val myLabel = stringResource(R.string.run_pace_me)
    val playerPaceCellWidth = widestLabeledPaceWidth(playerLabel, paceStyle)
    val myPaceCellWidth = widestLabeledPaceWidth(myLabel, paceStyle)
    val paceFitsOneLine = paceFitsOneLine(
        slots.belowRowTop,
        paceStyle,
        playerPaceCellWidth + BETWEEN_PACES_GAP + myPaceCellWidth,
    )
    val comboTop = with(LocalDensity.current) {
        partyComboRowTop(
            slots,
            paceInkHeight = paceStyle.fontSize.toPx() * SUIT_NUMERAL_INK.aboveBaselineEm,
            labelDescent = MaterialTheme.typography.labelSmall.fontSize.toPx() * SUIT_HANGUL_INK.belowBaselineEm,
            paceRowCount = if (paceFitsOneLine) 1 else 2,
            lineGap = RunDimensions.LINE_GAP.toPx(),
        )
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        distanceProgress(player.distanceMeters, run.targetDistanceMeters)?.let {
            ProgressRing(it, laneColor(player.colorSlot), Modifier.alpha(playerValuesAlpha))
        }
        DistanceNameRow(player, Modifier.contentBottomAt(slots.topRowBottom).alpha(playerValuesAlpha))
        GapOrStatus(player, slots, playerValuesAlpha)
        val myPace: @Composable (Modifier) -> Unit = { modifier ->
            LabeledPace(
                myLabel,
                run.me.currentPaceSecondsPerKm,
                paceStyle.tabularNumerals(),
                modifier.alpha(phoneValuesAlpha),
            )
        }
        val playerPace: @Composable (Modifier) -> Unit = { modifier ->
            LabeledPace(
                playerLabel,
                player.shownPaceSecondsPerKm(),
                paceStyle,
                modifier.alpha(playerValuesAlpha),
            )
        }
        val paceRowsModifier = Modifier.contentTopAt(slots.belowRowTop)
        if (notice != null) {
            // 끊긴 동안은 첫 화면이 오래된 격차를 숨기듯 상대 페이스를 숨기고, 콤보 자리는 안내가 대신한다
            myPace(paceRowsModifier)
            AlignedText(
                notice,
                MaterialTheme.typography.bodyMedium,
                MaterialTheme.colorScheme.onSurface,
                Modifier.contentTopAt(slots.bottomRowTop),
            )
        } else {
            if (paceFitsOneLine) {
                // 상대 값이 바뀌어도 내 페이스가 안 움직이게 칸 폭은 지금 값이 아니라 가장 넓은 페이스로 잡는다
                Row(paceRowsModifier, horizontalArrangement = Arrangement.spacedBy(BETWEEN_PACES_GAP)) {
                    Box(Modifier.width(playerPaceCellWidth), contentAlignment = Alignment.CenterEnd) {
                        playerPace(Modifier)
                    }
                    Box(Modifier.width(myPaceCellWidth), contentAlignment = Alignment.CenterStart) {
                        myPace(Modifier)
                    }
                }
            } else {
                ContentBoundsColumn(gap = RunDimensions.LINE_GAP, modifier = paceRowsModifier) {
                    playerPace(Modifier)
                    myPace(Modifier)
                }
            }
            AlignedText(
                if (player.comboCount > 0) {
                    stringResource(R.string.run_combo_current, player.comboCount)
                } else {
                    stringResource(R.string.run_combo_best, player.maxComboCount)
                },
                MaterialTheme.typography.labelSmall,
                MaterialTheme.colorScheme.onSurfaceVariant,
                Modifier.contentTopAt(comboTop).alpha(playerValuesAlpha),
            )
        }
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
                .padding(start = MIN_DISTANCE_TO_NAME_GAP),
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun Measured.contentCenter(): Int = (this[CONTENT_TOP] + this[CONTENT_BOTTOM]) / 2

@Composable
private fun GapOrStatus(player: Player, slots: RunPageSlots, alpha: Float) {
    val color = MaterialTheme.colorScheme.onBackground
    val statusWord = playerStatusWord(player.status)
    if (statusWord != null) {
        AlignedText(
            statusWord,
            MaterialTheme.typography.numeralLarge,
            color,
            Modifier.contentCenterAt(slots.center).alpha(alpha),
        )
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
            Modifier.baselineAt(slots.bigNumberBaseline).alpha(alpha),
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
        Modifier.baselineAt(slots.bigNumberBaseline).alpha(alpha),
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
private fun LabeledPace(label: String, secondsPerKm: Int?, style: TextStyle, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(LABEL_TO_PACE_GAP)) {
        AlignedText(
            label,
            MaterialTheme.typography.labelSmall,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Modifier.alignByBaseline(),
        )
        AlignedText(formatPace(secondsPerKm), style, MaterialTheme.colorScheme.onSurface, Modifier.alignByBaseline())
    }
}

@Composable
private fun widestLabeledPaceWidth(label: String, paceStyle: TextStyle): Dp {
    val labelStyle = MaterialTheme.typography.labelSmall
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(label, paceStyle, labelStyle, density) {
        with(density) {
            val textWidth = measurer.measure(label, labelStyle).size.width +
                measurer.measure(WIDEST_PACE, paceStyle.tabularNumerals()).size.width
            textWidth.toDp() + LABEL_TO_PACE_GAP
        }
    }
}

// 줄 수는 지금 값이 아니라 가장 넓은 값으로 정한다 — 달리는 중 페이스가 바뀌어도 모양이 안 흔들린다
@Composable
private fun paceFitsOneLine(rowsTop: Float, paceStyle: TextStyle, widestRow: Dp): Boolean {
    val screenWidth = LocalWindowInfo.current.containerSize.width.toFloat()
    return with(LocalDensity.current) {
        val firstBaseline = rowsTop + paceStyle.fontSize.toPx() * SUIT_NUMERAL_INK.aboveBaselineEm
        widestRow.toPx() <= usableWidthAt(firstBaseline, screenWidth, RunDimensions.TEXT_EDGE_MARGIN.toPx())
    }
}

@WearPreviewLargeRound
@WearPreviewSmallRound
@Composable
private fun PartyPageAheadPreview() = RunPreviewFrame {
    PartyPage(RunPreviewSamples.party, RunPreviewSamples.party.players[0], isPhoneReachable = true)
}

@WearPreviewLargeRound
@Composable
private fun PartyPageBehindComboPreview() = RunPreviewFrame {
    PartyPage(RunPreviewSamples.party, RunPreviewSamples.party.players[1], isPhoneReachable = true)
}

@WearPreviewLargeRound
@Composable
private fun PartyPagePausedPreview() = RunPreviewFrame {
    PartyPage(RunPreviewSamples.party, RunPreviewSamples.party.players[2], isPhoneReachable = true)
}

@WearPreviewLargeRound
@Composable
private fun PartyPageFinishedPreview() = RunPreviewFrame {
    PartyPage(RunPreviewSamples.party, RunPreviewSamples.finishedPlayer, isPhoneReachable = true)
}

@WearPreviewLargeRound
@Composable
private fun PartyPageNotNotifiedPreview() = RunPreviewFrame {
    PartyPage(RunPreviewSamples.party, RunPreviewSamples.notNotifiedPlayer, isPhoneReachable = true)
}

@WearPreviewLargeRound
@WearPreviewSmallRound
@Composable
private fun PartyPageLongestPreview() = RunPreviewFrame {
    PartyPage(RunPreviewSamples.longValues, RunPreviewSamples.longestPlayer, isPhoneReachable = true)
}

@WearPreviewLargeRound
@Composable
private fun PartyPageStalePreview() = RunPreviewFrame {
    val player = RunPreviewSamples.party.players[0].copy(isStale = true)
    PartyPage(RunPreviewSamples.party, player, isPhoneReachable = true)
}

@WearPreviewLargeRound
@WearPreviewSmallRound
@Composable
private fun PartyPageServerLostPreview() = RunPreviewFrame {
    val run = RunPreviewSamples.party.copy(isServerConnected = false)
    PartyPage(run, run.players[0], isPhoneReachable = true)
}

@WearPreviewLargeRound
@Composable
private fun PartyPagePhoneLostPreview() = RunPreviewFrame {
    PartyPage(RunPreviewSamples.party, RunPreviewSamples.party.players[0], isPhoneReachable = false)
}
