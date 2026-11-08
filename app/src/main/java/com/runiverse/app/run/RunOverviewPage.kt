package com.runiverse.app.run

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import androidx.wear.compose.ui.tooling.preview.WearPreviewSmallRound
import com.runiverse.app.R
import com.runiverse.app.phone.MyRecord
import com.runiverse.app.phone.Player
import com.runiverse.app.phone.RunState
import com.runiverse.app.phone.RunStatus
import com.runiverse.app.run.common.DistanceUnitLabel
import com.runiverse.app.run.common.GapDirection
import com.runiverse.app.run.common.RunPreviewSamples
import com.runiverse.app.run.common.baselineAt
import com.runiverse.app.run.common.contentBottomAt
import com.runiverse.app.run.common.contentCenterAt
import com.runiverse.app.run.common.contentTopAt
import com.runiverse.app.run.common.currentRunPageSlots
import com.runiverse.app.run.common.currentScreenWidth
import com.runiverse.app.run.common.distanceProgress
import com.runiverse.app.run.common.formatElapsedTime
import com.runiverse.app.run.common.formatGapDistance
import com.runiverse.app.run.common.formatPace
import com.runiverse.app.run.common.formatRunDistanceKm
import com.runiverse.app.run.common.gapDirection
import com.runiverse.app.run.common.hasStaleValues
import com.runiverse.app.run.component.AlignedText
import com.runiverse.app.run.component.ContentBoundsColumn
import com.runiverse.app.run.component.HeartRateZoneBar
import com.runiverse.app.run.component.ProgressRing
import com.runiverse.app.run.component.RunDimensions
import com.runiverse.app.run.component.ValueWithUnit
import com.runiverse.app.run.component.boxContentBounds
import com.runiverse.app.run.component.widestHeartRateBlockWidth
import com.runiverse.app.ui.component.PreviewFrame
import com.runiverse.app.ui.theme.MY_LANE_COLOR_SLOT
import com.runiverse.app.ui.theme.SUIT_NUMERAL_INK
import com.runiverse.app.ui.theme.laneColor
import com.runiverse.app.ui.theme.statusWordRaise
import com.runiverse.app.ui.theme.tabularNumerals
import kotlin.math.roundToInt

private const val MIN_PLAYER_CELL_WIDTH_FRACTION = 110f / 480f
private const val STATUS_OVERLAY_ALPHA = 0.45f
private const val STATUS_ICON_FRACTION = 0.42f
private val DIRECTION_INDICATOR_WIDTH = 7.5.dp
private val DIRECTION_INDICATOR_HEIGHT = 6.6.dp
private val DIRECTION_INDICATOR_TO_VALUE_GAP = 2.dp
private val MIN_LABEL_TO_CELL_GAP = 4.dp
private val NARROW_SCREEN_BELOW = 200.dp

// 값이 바뀌어도 사진이 안 움직이게 칸 폭은 지금 값이 아니라 정해 둔 넓은 격차로 잰다
private val WIDEST_GAPS_METERS = listOf(400, 4_000)

@Composable
internal fun RunOverviewPage(run: RunState, isPhoneReachable: Boolean, heartRateBpm: Int?, heartRateZone: Int?) {
    val slots = currentRunPageSlots()
    // 키운 글자로는 가장 긴 값이 한 줄에 안 들어간다
    val abbreviateUnits = LocalDensity.current.fontScale > 1f
    // 폰 소식이 끊기면 받은 값 전부를 마지막 값에서 멈춘 채 흐리게 둔다
    val phoneValuesAlpha = if (isPhoneReachable) 1f else RunDimensions.STALE_CONTENT_ALPHA
    val notice = connectionNotice(run, isPhoneReachable)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        distanceProgress(run.me.distanceMeters, run.targetDistanceMeters)?.let {
            ProgressRing(it, laneColor(MY_LANE_COLOR_SLOT), Modifier.alpha(phoneValuesAlpha))
        }
        DistanceTimeRow(
            run.me,
            abbreviateUnits,
            Modifier.contentBottomAt(slots.topRowBottom).alpha(phoneValuesAlpha),
        )
        // 멈춘 동안은 지금 페이스가 없다
        if (run.status == RunStatus.PAUSED) {
            AlignedText(
                stringResource(R.string.run_status_paused),
                MaterialTheme.typography.numeralLarge,
                MaterialTheme.colorScheme.onBackground,
                Modifier.contentCenterAt(slots.center).alpha(phoneValuesAlpha),
            )
        } else {
            AlignedText(
                formatPace(run.me.currentPaceSecondsPerKm),
                MaterialTheme.typography.numeralExtraLarge.tabularNumerals(),
                MaterialTheme.colorScheme.onBackground,
                Modifier.contentCenterAt(slots.center).alpha(phoneValuesAlpha),
            )
        }
        if (run.players.isEmpty()) {
            SoloHeartRate(heartRateBpm, heartRateZone, Modifier.contentTopAt(slots.belowRowTop))
        } else {
            // 좁은 화면은 가장 긴 격차가 링과 겹쳐 단위를 줄이고, 글자까지 키우면 km 소수도 뺀다
            val isNarrowScreen = currentScreenWidth() < NARROW_SCREEN_BELOW
            val abbreviateGapUnits = abbreviateUnits || isNarrowScreen
            val dropGapKmDecimal = abbreviateUnits && isNarrowScreen
            val cellWidth = playerCellWidth(abbreviateGapUnits, dropGapKmDecimal)
            AvatarRow(run, isPhoneReachable, cellWidth, Modifier.contentTopAt(slots.belowRowTop))
            // 끊긴 동안의 격차는 오래된 값이라 숨기고 그 자리에 안내를 띄운다
            if (notice == null) {
                // 상태 글자는 윗변이 높아 윗변으로 맞추면 옆 사람 격차까지 밀린다 — 숫자 기준선에 맞춘다
                val gapInkHeight = with(LocalDensity.current) {
                    MaterialTheme.typography.numeralExtraSmall.fontSize.toPx() * SUIT_NUMERAL_INK.aboveBaselineEm
                }
                GapLabelRow(
                    run.players,
                    abbreviateGapUnits,
                    dropGapKmDecimal,
                    cellWidth,
                    Modifier.baselineAt(slots.bottomRowTop + gapInkHeight),
                )
            }
        }
        if (notice != null) {
            AlignedText(
                notice,
                MaterialTheme.typography.bodyMedium,
                MaterialTheme.colorScheme.onSurface,
                Modifier.contentTopAt(slots.bottomRowTop),
            )
        }
    }
}

@Composable
private fun DistanceTimeRow(me: MyRecord, abbreviateUnits: Boolean, modifier: Modifier) {
    val style = MaterialTheme.typography.numeralSmall.tabularNumerals()
    Row(
        modifier.fillMaxWidth(1f - RunDimensions.TOP_ROW_SIDE_FRACTION * 2),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ValueWithUnit(
            formatRunDistanceKm(me.distanceMeters),
            if (abbreviateUnits) null else stringResource(R.string.unit_km),
            style,
        )
        AlignedText(formatElapsedTime(me.elapsedSeconds), style, MaterialTheme.colorScheme.onSurface)
    }
}

// 심박은 워치가 재는 값이라 폰이 끊겨도 흐리지 않는다
@Composable
private fun SoloHeartRate(heartRateBpm: Int?, heartRateZone: Int?, modifier: Modifier) {
    val style = MaterialTheme.typography.numeralSmall.tabularNumerals()
    ContentBoundsColumn(contentGap = RunDimensions.VALUE_TO_ZONE_BAR_GAP, modifier = modifier) {
        ValueWithUnit(
            heartRateBpm?.toString() ?: stringResource(R.string.run_value_unknown),
            stringResource(R.string.unit_bpm),
            style,
        )
        if (heartRateZone != null) {
            HeartRateZoneBar(heartRateZone, Modifier.width(widestHeartRateBlockWidth(style)))
        }
    }
}

@Composable
private fun playerCellWidth(abbreviateUnits: Boolean, dropKmDecimal: Boolean): Dp {
    val widestLabels = WIDEST_GAPS_METERS.map { meters ->
        val gap = formatGapDistance(meters, abbreviateUnits, dropKmDecimal)
        gap.value to gap.unit?.let { distanceUnitText(it) }
    }
    val valueStyle = MaterialTheme.typography.numeralExtraSmall
    val unitStyle = MaterialTheme.typography.labelSmall
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val screenWidth = currentScreenWidth()
    return remember(widestLabels, valueStyle, unitStyle, density, screenWidth) {
        val widestLabelWidth = widestLabels.maxOf { (value, unit) ->
            val valueWidth = with(density) { measurer.measure(value, valueStyle).size.width.toDp() }
            val unitWidth = unit?.let {
                with(density) { measurer.measure(it, unitStyle).size.width.toDp() } + RunDimensions.VALUE_TO_UNIT_GAP
            } ?: 0.dp
            DIRECTION_INDICATOR_WIDTH + DIRECTION_INDICATOR_TO_VALUE_GAP + valueWidth + unitWidth
        }
        maxOf(screenWidth * MIN_PLAYER_CELL_WIDTH_FRACTION, widestLabelWidth + MIN_LABEL_TO_CELL_GAP)
    }
}

@Composable
private fun AvatarRow(run: RunState, isPhoneReachable: Boolean, cellWidth: Dp, modifier: Modifier) {
    Row(modifier) {
        run.players.forEach { player ->
            val hasStaleValues = player.hasStaleValues(run, isPhoneReachable)
            Box(Modifier.width(cellWidth), contentAlignment = Alignment.Center) {
                PlayerAvatar(player, Modifier.alpha(if (hasStaleValues) RunDimensions.STALE_CONTENT_ALPHA else 1f))
            }
        }
    }
}

@Composable
private fun GapLabelRow(
    players: List<Player>,
    abbreviateUnits: Boolean,
    dropKmDecimal: Boolean,
    cellWidth: Dp,
    modifier: Modifier,
) {
    Row(modifier) {
        players.forEach { player ->
            Box(
                Modifier.width(cellWidth)
                    .alignByBaseline()
                    .alpha(if (player.isStale) RunDimensions.STALE_CONTENT_ALPHA else 1f),
                contentAlignment = Alignment.TopCenter,
            ) {
                PlayerGapLabel(player, abbreviateUnits, dropKmDecimal)
            }
        }
    }
}

@Composable
private fun PlayerGapLabel(player: Player, abbreviateUnits: Boolean, dropKmDecimal: Boolean) {
    val statusWord = playerStatusWord(player.status)
    if (statusWord != null) {
        val statusStyle = MaterialTheme.typography.bodySmall
        val numberStyle = MaterialTheme.typography.numeralExtraSmall
        AlignedText(
            statusWord,
            statusStyle,
            MaterialTheme.colorScheme.onSurface,
            Modifier.raisedAboveRowBaseline(statusStyle.fontSize, numberStyle.fontSize),
        )
        return
    }
    // 그 사람 통지가 아직 없으면 폰도 격차 줄을 그리지 않는다
    val gapMeters = player.gapMeters ?: return
    val gap = formatGapDistance(gapMeters, abbreviateUnits, dropKmDecimal)
    Row(horizontalArrangement = Arrangement.spacedBy(DIRECTION_INDICATOR_TO_VALUE_GAP)) {
        when (gapDirection(gapMeters)) {
            GapDirection.AHEAD -> GapDirectionIndicator(pointsUp = true, Modifier.alignByBaseline())
            GapDirection.BEHIND -> GapDirectionIndicator(pointsUp = false, Modifier.alignByBaseline())
            GapDirection.LEVEL -> Unit
        }
        ValueWithUnit(
            gap.value,
            gap.unit?.let { distanceUnitText(it) },
            MaterialTheme.typography.numeralExtraSmall,
            Modifier.alignByBaseline(),
        )
    }
}

// 상태 글자의 세로 가운데를 옆 숫자에 맞춘다 — 기준선을 그만큼 낮춰 알려 위로 올린다
private fun Modifier.raisedAboveRowBaseline(statusSize: TextUnit, numberSize: TextUnit): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val raise = statusWordRaise(statusSize.toPx(), numberSize.toPx()).roundToInt()
        val baseline = mapOf<AlignmentLine, Int>(FirstBaseline to placeable[FirstBaseline] + raise)
        layout(placeable.width, placeable.height, baseline) { placeable.place(0, 0) }
    }

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

// 앞/뒤에 색을 주면 레인 색과 겹치고 좋다/나쁘다로 읽혀서 흰색으로 둔다
@Composable
private fun GapDirectionIndicator(pointsUp: Boolean, modifier: Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(modifier.bottomAsBaseline().size(DIRECTION_INDICATOR_WIDTH, DIRECTION_INDICATOR_HEIGHT)) {
        val path = Path().apply {
            if (pointsUp) {
                moveTo(size.width / 2, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
            } else {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2, size.height)
            }
            close()
        }
        drawPath(path, color)
    }
}

private fun Modifier.bottomAsBaseline(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height, mapOf<AlignmentLine, Int>(FirstBaseline to placeable.height)) {
        placeable.place(0, 0)
    }
}

// TODO: 파티원 사진 — 폰이 사진을 보내면 photos로 그린다. 그 전엔 색 원에 첫 글자를 쓴다
// 칸을 고리까지 잡는다 — alpha가 칸 밖을 잘라 소식이 늦은 사람의 고리가 잘린다
@Composable
private fun PlayerAvatar(player: Player, modifier: Modifier) {
    val lane = laneColor(player.colorSlot)
    val myLane = laneColor(MY_LANE_COLOR_SLOT)
    val isCombo = player.comboCount > 0
    Box(
        modifier
            .size(RunDimensions.AVATAR_SIZE + RunDimensions.COMBO_BORDER_WIDTH * 2)
            .drawBehind {
                if (isCombo) {
                    // 나와 상대의 레인 색을 섞어 누구와 붙었는지 알 수 있게 한다
                    val border = RunDimensions.COMBO_BORDER_WIDTH.toPx()
                    drawCircle(
                        Brush.linearGradient(listOf(myLane, lane)),
                        radius = RunDimensions.AVATAR_SIZE.toPx() / 2 + border / 2,
                        style = Stroke(border),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.boxContentBounds()
                .size(RunDimensions.AVATAR_SIZE)
                .clip(CircleShape)
                .background(lane),
            contentAlignment = Alignment.Center,
        ) {
            if (player.status == RunStatus.RUNNING) {
                Text(
                    player.nickname.take(1),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            } else {
                val iconColor = MaterialTheme.colorScheme.onBackground
                Box(
                    Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = STATUS_OVERLAY_ALPHA)),
                )
                Canvas(Modifier.fillMaxSize(STATUS_ICON_FRACTION)) {
                    when (player.status) {
                        RunStatus.PAUSED -> drawPauseIcon(iconColor)
                        RunStatus.DISCONNECTED -> drawDisconnectedIcon(iconColor)
                        RunStatus.FINISHED -> drawFinishedIcon(iconColor)
                        RunStatus.RUNNING -> Unit
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawPauseIcon(color: Color) {
    val barWidth = size.width * 0.28f
    val barSize = Size(barWidth, size.height)
    drawRect(color, topLeft = Offset(size.width * 0.12f, 0f), size = barSize)
    drawRect(color, topLeft = Offset(size.width * 0.88f - barWidth, 0f), size = barSize)
}

private fun DrawScope.drawFinishedIcon(color: Color) {
    val path = Path().apply {
        moveTo(size.width * 0.08f, size.height * 0.52f)
        lineTo(size.width * 0.4f, size.height * 0.82f)
        lineTo(size.width * 0.94f, size.height * 0.2f)
    }
    drawPath(path, color, style = Stroke(size.width * 0.16f, cap = StrokeCap.Round))
}

private fun DrawScope.drawDisconnectedIcon(color: Color) {
    val stroke = Stroke(size.width * 0.12f, cap = StrokeCap.Round)
    val center = Offset(size.width / 2, size.height * 0.9f)
    listOf(0.85f, 0.55f).forEach { radiusFraction ->
        val arcRadius = size.width * radiusFraction
        drawArc(
            color,
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
            size = Size(arcRadius * 2, arcRadius * 2),
            style = stroke,
        )
    }
    drawCircle(color, radius = size.width * 0.09f, center = center)
    drawLine(color, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth = stroke.width, cap = StrokeCap.Round)
}

@WearPreviewLargeRound
@WearPreviewSmallRound
@Composable
private fun RunOverviewPagePartyPreview() = PreviewFrame {
    RunOverviewPage(RunPreviewSamples.party, isPhoneReachable = true, heartRateBpm = 148, heartRateZone = 3)
}

@WearPreviewLargeRound
@Composable
private fun RunOverviewPageLongValuesPreview() = PreviewFrame {
    RunOverviewPage(RunPreviewSamples.longValues, isPhoneReachable = true, heartRateBpm = 148, heartRateZone = 3)
}

@WearPreviewLargeRound
@Composable
private fun RunOverviewPagePhoneLostPreview() = PreviewFrame {
    RunOverviewPage(RunPreviewSamples.party, isPhoneReachable = false, heartRateBpm = 148, heartRateZone = 3)
}

@WearPreviewLargeRound
@Composable
private fun RunOverviewPageServerLostPreview() = PreviewFrame {
    RunOverviewPage(
        RunPreviewSamples.party.copy(isServerConnected = false),
        isPhoneReachable = true,
        heartRateBpm = 148,
        heartRateZone = 3,
    )
}

@WearPreviewLargeRound
@WearPreviewFontScales
@Composable
private fun RunOverviewPageSoloPreview() = PreviewFrame {
    RunOverviewPage(RunPreviewSamples.solo, isPhoneReachable = true, heartRateBpm = 148, heartRateZone = 3)
}

@WearPreviewLargeRound
@WearPreviewSmallRound
@Composable
private fun RunOverviewPagePausedPreview() = PreviewFrame {
    RunOverviewPage(
        RunPreviewSamples.party.copy(status = RunStatus.PAUSED),
        isPhoneReachable = true,
        heartRateBpm = 92,
        heartRateZone = 1,
    )
}

@WearPreviewLargeRound
@Composable
private fun RunOverviewPageSoloPausedPreview() = PreviewFrame {
    RunOverviewPage(
        RunPreviewSamples.solo.copy(status = RunStatus.PAUSED),
        isPhoneReachable = true,
        heartRateBpm = 92,
        heartRateZone = 1,
    )
}
