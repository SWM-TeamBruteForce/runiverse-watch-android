package com.runiverse.app.run

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.layout.layout
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
import com.runiverse.app.phone.RunStatus
import com.runiverse.app.run.common.GapDirection
import com.runiverse.app.run.common.RunPageSlots
import com.runiverse.app.run.common.RunPreviewSamples
import com.runiverse.app.run.common.comboBumpScale
import com.runiverse.app.run.common.connectionNotice
import com.runiverse.app.run.common.currentRunPageSlots
import com.runiverse.app.run.common.distanceProgress
import com.runiverse.app.run.common.distanceUnitText
import com.runiverse.app.run.common.formatGapDistance
import com.runiverse.app.run.common.formatPace
import com.runiverse.app.run.common.formatRunDistanceKm
import com.runiverse.app.run.common.gapDirection
import com.runiverse.app.run.common.hasStaleValues
import com.runiverse.app.run.common.partyComboRowTop
import com.runiverse.app.run.common.playerStatusWord
import com.runiverse.app.run.common.shownMyPaceSecondsPerKm
import com.runiverse.app.run.common.shownPaceSecondsPerKm
import com.runiverse.app.run.common.smallComboMomentProgress
import com.runiverse.app.run.common.usableWidthAt
import com.runiverse.app.run.component.AlignedText
import com.runiverse.app.run.component.CONTENT_BOTTOM
import com.runiverse.app.run.component.CONTENT_TOP
import com.runiverse.app.run.component.ContentBoundsColumn
import com.runiverse.app.run.component.ProgressRing
import com.runiverse.app.run.component.RunDimensions
import com.runiverse.app.run.component.ValueWithUnit
import com.runiverse.app.run.component.baselineAt
import com.runiverse.app.run.component.contentBottom
import com.runiverse.app.run.component.contentBottomAt
import com.runiverse.app.run.component.contentCenterAt
import com.runiverse.app.run.component.contentTop
import com.runiverse.app.run.component.contentTopAt
import com.runiverse.app.ui.component.PreviewFrame
import com.runiverse.app.ui.theme.SUIT_HANGUL_INK
import com.runiverse.app.ui.theme.SUIT_NUMERAL_INK
import com.runiverse.app.ui.theme.laneColor
import com.runiverse.app.ui.theme.tabularNumerals

private const val WIDEST_PACE = "88'88\""
private val LABEL_TO_PACE_GAP = 3.dp
private val PACE_GAP = 8.dp
private val MIN_DISTANCE_TO_NAME_GAP = 8.dp

@Composable
internal fun PartyPage(run: RunState, player: Player, isPhoneReachable: Boolean, smallComboMomentId: String? = null) {
    val slots = currentRunPageSlots()
    val notice = connectionNotice(run, isPhoneReachable)
    val playerValuesAlpha = if (player.hasStaleValues(run, isPhoneReachable)) RunDimensions.STALE_CONTENT_ALPHA else 1f
    // 서버가 끊겨도 내 페이스는 폰이 계속 재는 값이라 폰이 끊길 때만 흐린다
    val phoneValuesAlpha = if (isPhoneReachable) 1f else RunDimensions.STALE_CONTENT_ALPHA
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        distanceProgress(player.distanceMeters, run.targetDistanceMeters)?.let {
            ProgressRing(it, laneColor(player.colorSlot), Modifier.alpha(playerValuesAlpha))
        }
        DistanceNameRow(player, Modifier.contentBottomAt(slots.topRowBottom).alpha(playerValuesAlpha))
        GapOrStatus(player, slots, playerValuesAlpha)
        if (notice != null) {
            // 끊긴 동안은 오래된 상대 페이스를 숨기고, 콤보 자리는 안내가 대신한다
            MyPace(
                run.shownMyPaceSecondsPerKm(),
                Modifier.contentTopAt(slots.belowRowTop).alpha(phoneValuesAlpha),
            )
            AlignedText(
                notice,
                MaterialTheme.typography.bodyMedium,
                MaterialTheme.colorScheme.onSurface,
                Modifier.contentTopAt(slots.bottomRowTop),
            )
        } else {
            PaceAndComboRows(
                player,
                run.shownMyPaceSecondsPerKm(),
                slots,
                smallComboMomentId,
                playerValuesAlpha,
                phoneValuesAlpha,
            )
        }
    }
}

// 콤보 줄 자리가 페이스 줄 수를 따라가서 같이 놓는다
@Composable
private fun PaceAndComboRows(
    player: Player,
    myPaceSecondsPerKm: Int?,
    slots: RunPageSlots,
    smallComboMomentId: String?,
    playerValuesAlpha: Float,
    phoneValuesAlpha: Float,
) {
    val paceStyle = MaterialTheme.typography.numeralSmall
    val playerCellWidth = widestLabeledPaceWidth(stringResource(R.string.run_pace_player), paceStyle)
    val myCellWidth = widestLabeledPaceWidth(stringResource(R.string.run_pace_me), paceStyle)
    val fitsOneLine = paceFitsOneLine(slots.belowRowTop, paceStyle, playerCellWidth + PACE_GAP + myCellWidth)
    val paceRowsModifier = Modifier.contentTopAt(slots.belowRowTop)
    if (fitsOneLine) {
        // 상대 값이 바뀌어도 내 페이스가 안 움직이게 칸 폭을 가장 넓은 페이스로 잡는다
        Row(paceRowsModifier, horizontalArrangement = Arrangement.spacedBy(PACE_GAP)) {
            Box(Modifier.width(playerCellWidth), contentAlignment = Alignment.CenterEnd) {
                // 상대는 `40'00"`처럼 칸보다 넓을 수 있다 — 끝 글자가 안 잘리게 왼쪽으로 넘긴다
                PlayerPace(
                    player,
                    Modifier.wrapContentWidth(Alignment.End, unbounded = true).alpha(playerValuesAlpha),
                )
            }
            Box(Modifier.width(myCellWidth), contentAlignment = Alignment.CenterStart) {
                MyPace(myPaceSecondsPerKm, Modifier.alpha(phoneValuesAlpha))
            }
        }
    } else {
        ContentBoundsColumn(contentGap = RunDimensions.LINE_GAP, modifier = paceRowsModifier) {
            PlayerPace(player, Modifier.alpha(playerValuesAlpha))
            MyPace(myPaceSecondsPerKm, Modifier.alpha(phoneValuesAlpha))
        }
    }
    val comboRowTop = with(LocalDensity.current) {
        partyComboRowTop(
            slots,
            paceInkHeight = paceStyle.fontSize.toPx() * SUIT_NUMERAL_INK.aboveBaselineEm,
            labelDescent = MaterialTheme.typography.labelSmall.fontSize.toPx() * SUIT_HANGUL_INK.belowBaselineEm,
            paceRowCount = if (fitsOneLine) 1 else 2,
            lineGap = RunDimensions.LINE_GAP.toPx(),
        )
    }
    val momentProgress = smallComboMomentProgress(smallComboMomentId)
    AlignedText(
        if (player.comboCount > 0) {
            stringResource(R.string.run_combo_current, player.comboCount)
        } else {
            stringResource(R.string.run_combo_max, player.maxComboCount)
        },
        MaterialTheme.typography.labelSmall,
        MaterialTheme.colorScheme.onSurfaceVariant,
        // alpha가 상자 밖을 잘라서 키우기를 바깥에 둔다
        Modifier.contentTopAt(comboRowTop)
            .scaledAtContentCenter { comboBumpScale(momentProgress.value) }
            .alpha(playerValuesAlpha),
    )
}

// 상자 가운데로 키우면 꼬리 글자는 아래로 치우쳐 커진다 — 보이는 글자 가운데를 붙잡는다
private fun Modifier.scaledAtContentCenter(scale: () -> Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val pivotY = (placeable.contentTop() + placeable.contentBottom()) / 2f / placeable.height
    layout(placeable.width, placeable.height) {
        placeable.placeWithLayer(0, 0) {
            val current = scale()
            transformOrigin = TransformOrigin(0.5f, pivotY)
            scaleX = current
            scaleY = current
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
        horizontalArrangement = Arrangement.spacedBy(RunDimensions.VALUE_TO_UNIT_GAP),
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
private fun PlayerPace(player: Player, modifier: Modifier) {
    LabeledPace(
        stringResource(R.string.run_pace_player),
        player.shownPaceSecondsPerKm(),
        MaterialTheme.typography.numeralSmall,
        modifier,
    )
}

@Composable
private fun MyPace(secondsPerKm: Int?, modifier: Modifier) {
    LabeledPace(
        stringResource(R.string.run_pace_me),
        secondsPerKm,
        MaterialTheme.typography.numeralSmall.tabularNumerals(),
        modifier,
    )
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

// 줄 수는 가장 넓은 값으로 정한다 — 달리는 중 페이스가 바뀌어도 모양이 안 바뀐다
@Composable
private fun paceFitsOneLine(rowsTop: Float, paceStyle: TextStyle, widestRowWidth: Dp): Boolean {
    val screenSize = LocalWindowInfo.current.containerSize.width.toFloat()
    return with(LocalDensity.current) {
        val firstBaseline = rowsTop + paceStyle.fontSize.toPx() * SUIT_NUMERAL_INK.aboveBaselineEm
        widestRowWidth.toPx() <= usableWidthAt(firstBaseline, screenSize, RunDimensions.TEXT_EDGE_MARGIN.toPx())
    }
}

@WearPreviewLargeRound
@WearPreviewSmallRound
@Composable
private fun PartyPageAheadPreview() = PreviewFrame {
    PartyPage(RunPreviewSamples.party, RunPreviewSamples.party.players[0], isPhoneReachable = true)
}

@WearPreviewLargeRound
@Composable
private fun PartyPageBehindComboPreview() = PreviewFrame {
    PartyPage(RunPreviewSamples.party, RunPreviewSamples.party.players[1], isPhoneReachable = true)
}

@WearPreviewLargeRound
@Composable
private fun PartyPagePausedPreview() = PreviewFrame {
    PartyPage(RunPreviewSamples.party, RunPreviewSamples.party.players[2], isPhoneReachable = true)
}

@WearPreviewLargeRound
@Composable
private fun PartyPageMePausedPreview() = PreviewFrame {
    val run = RunPreviewSamples.party.copy(status = RunStatus.PAUSED)
    PartyPage(run, run.players[0], isPhoneReachable = true)
}

@WearPreviewLargeRound
@Composable
private fun PartyPageFinishedPreview() = PreviewFrame {
    PartyPage(RunPreviewSamples.party, RunPreviewSamples.finishedPlayer, isPhoneReachable = true)
}

@WearPreviewLargeRound
@Composable
private fun PartyPageNotNotifiedPreview() = PreviewFrame {
    PartyPage(RunPreviewSamples.party, RunPreviewSamples.notNotifiedPlayer, isPhoneReachable = true)
}

@WearPreviewLargeRound
@WearPreviewSmallRound
@Composable
private fun PartyPageLongestPreview() = PreviewFrame {
    PartyPage(RunPreviewSamples.longValues, RunPreviewSamples.longestPlayer, isPhoneReachable = true)
}

@WearPreviewLargeRound
@Composable
private fun PartyPageStalePreview() = PreviewFrame {
    val player = RunPreviewSamples.party.players[0].copy(isStale = true)
    PartyPage(RunPreviewSamples.party, player, isPhoneReachable = true)
}

@WearPreviewLargeRound
@WearPreviewSmallRound
@Composable
private fun PartyPageServerLostPreview() = PreviewFrame {
    val run = RunPreviewSamples.party.copy(isServerConnected = false)
    PartyPage(run, run.players[0], isPhoneReachable = true)
}

@WearPreviewLargeRound
@Composable
private fun PartyPagePhoneLostPreview() = PreviewFrame {
    PartyPage(RunPreviewSamples.party, RunPreviewSamples.party.players[0], isPhoneReachable = false)
}
