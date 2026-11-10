package com.runiverse.app.run

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.withTranslation
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import androidx.wear.compose.ui.tooling.preview.WearPreviewSmallRound
import com.runiverse.app.R
import com.runiverse.app.phone.Player
import com.runiverse.app.run.common.BIG_COMBO_MOMENT_DURATION
import com.runiverse.app.run.common.ComboBurstFrame
import com.runiverse.app.run.common.RING_END_SCALE
import com.runiverse.app.run.common.RunPreviewSamples
import com.runiverse.app.run.common.comboBurstFrame
import com.runiverse.app.run.common.currentRunPageSlots
import com.runiverse.app.run.component.RunDimensions
import com.runiverse.app.ui.component.PreviewFrame
import com.runiverse.app.ui.theme.MY_LANE_COLOR_SLOT
import com.runiverse.app.ui.theme.laneColor
import com.runiverse.app.ui.theme.suitInkFor
import kotlin.math.roundToInt
import kotlin.math.tan

// 흰 고리가 다 퍼졌을 때 화면 끝에 닿는다
private const val RING_RADIUS_TO_SCREEN_RATIO = 0.5f / RING_END_SCALE
private val RING_STROKE_WIDTH = 1.5.dp
private val COUNT_TO_WORD_GAP = 8.dp
private val AVATAR_TO_NAME_GAP = 6.dp
private const val MAX_NAME_WIDTH_FRACTION = 0.62f

// SUIT엔 기울임 글꼴이 없어 기계가 기울이는 각도로 폰의 기울임 글꼴을 흉내 낸다
private const val ITALIC_DEGREES = 14f

/** 콤보 눈금에 닿은 순간 화면 전체를 1.2초 덮는다. 다 그리면 [onFinished]를 부른다. */
@Composable
internal fun ComboBurstScreen(comboCount: Int, player: Player, photo: ImageBitmap?, onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val currentOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(BIG_COMBO_MOMENT_DURATION.inWholeMilliseconds.toInt(), easing = LinearEasing))
        currentOnFinished()
    }
    ComboBurstCanvas(comboCount, player, photo, comboBurstFrame(progress.value))
}

@Composable
private fun ComboBurstCanvas(comboCount: Int, player: Player, photo: ImageBitmap?, frame: ComboBurstFrame) {
    val slots = currentRunPageSlots()
    val measurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val countStyle = typography.numeralExtraLarge
    val wordStyle = typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
    val nameStyle = typography.bodyMedium.copy(color = colors.onSurface)
    val initialStyle = typography.labelMedium.copy(color = colors.onBackground)
    val countBrush = Brush.horizontalGradient(
        listOf(laneColor(MY_LANE_COLOR_SLOT), colors.onBackground, laneColor(player.colorSlot)),
    )
    val wordText = stringResource(R.string.run_combo_burst_word)
    val description = stringResource(R.string.run_combo_burst_description, comboCount, player.nickname)
    Canvas(
        Modifier
            .fillMaxSize()
            // 덮개가 검어 뒤 장이 안 보인다 — 터치를 받아 덮인 채 장이 넘어가지 않게 한다
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume() } }
            .semantics { contentDescription = description },
    ) {
        drawRect(colors.background, alpha = frame.coverAlpha)
        drawSpreadingRing(frame, colors.onBackground)

        val count = measurer.measure(comboCount.toString(), countStyle)
        val word = measurer.measure(wordText, wordStyle.copy(color = colors.onBackground))
        val gap = COUNT_TO_WORD_GAP.toPx()
        val countInkTop = countStyle.fontSize.toPx() * suitInkFor(comboCount.toString()).aboveBaselineEm
        // 숫자 잉크의 세로 가운데를 큰 숫자 자리 가운데에 둔다
        val baseline = slots.center + countInkTop / 2
        val left = (size.width - (count.size.width + gap + word.size.width)) / 2
        val alpha = frame.textAlpha * frame.coverAlpha
        rotate(frame.textDegrees, Offset(size.width / 2, slots.center)) {
            scale(frame.textScale, Offset(size.width / 2, slots.center)) {
                italic(baseline) {
                    drawText(count, countBrush, Offset(left, baseline - count.firstBaseline), alpha)
                    drawText(word, topLeft = Offset(left + count.size.width + gap, baseline - word.firstBaseline), alpha = alpha)
                }
            }
        }
        drawPlayerRow(measurer, player, photo, nameStyle, initialStyle, slots.belowRowTop, alpha)
    }
}

// 고리는 그 화면 모양을 따른다 — 갤럭시는 원이다
private fun DrawScope.drawSpreadingRing(frame: ComboBurstFrame, color: Color) {
    if (frame.ringAlpha <= 0f) return
    drawCircle(
        color,
        radius = size.minDimension * RING_RADIUS_TO_SCREEN_RATIO * frame.ringScale,
        alpha = frame.ringAlpha * frame.coverAlpha,
        style = Stroke(RING_STROKE_WIDTH.toPx() * frame.ringScale),
    )
}

// 기준선을 축으로 위쪽을 민다 — 안드로이드 캔버스는 각도가 아니라 기울기(tan)를 받는다
private fun DrawScope.italic(baseline: Float, block: DrawScope.() -> Unit) {
    drawContext.canvas.nativeCanvas.withTranslation(0f, baseline) {
        skew(-tan(Math.toRadians(ITALIC_DEGREES.toDouble())).toFloat(), 0f)
        translate(0f, -baseline)
        this@italic.block()
    }
}

// 끝나면 첫 화면의 그 사람 사진으로 이어지게 사진 줄 자리에 사진과 이름을 둔다
private fun DrawScope.drawPlayerRow(
    measurer: TextMeasurer,
    player: Player,
    photo: ImageBitmap?,
    nameStyle: TextStyle,
    initialStyle: TextStyle,
    rowTop: Float,
    alpha: Float,
) {
    val avatar = RunDimensions.AVATAR_SIZE.toPx()
    val gap = AVATAR_TO_NAME_GAP.toPx()
    val name = measurer.measure(
        player.nickname,
        nameStyle,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        constraints = Constraints(maxWidth = (size.width * MAX_NAME_WIDTH_FRACTION).toInt()),
    )
    val left = (size.width - (avatar + gap + name.size.width)) / 2
    val center = Offset(left + avatar / 2, rowTop + avatar / 2)
    val lane = laneColor(player.colorSlot)
    // 사진과 따로 반투명하게 겹치면 나타나는 동안 레인 색이 사진 너머로 비친다
    if (photo == null) drawCircle(lane, avatar / 2, center, alpha = alpha)
    val border = RunDimensions.COMBO_BORDER_WIDTH.toPx()
    val ringRadius = avatar / 2 + border / 2
    drawCircle(
        Brush.linearGradient(
            listOf(laneColor(MY_LANE_COLOR_SLOT), lane),
            start = center - Offset(ringRadius, ringRadius),
            end = center + Offset(ringRadius, ringRadius),
        ),
        ringRadius,
        center,
        alpha = alpha,
        style = Stroke(border),
    )
    if (photo != null) {
        drawPhotoInCircle(photo, center, avatar / 2, alpha)
    } else {
        val initialText = player.nickname.take(1)
        val initial = measurer.measure(initialText, initialStyle)
        val initialStart = Offset(center.x - initial.size.width / 2, center.y)
        drawTextInkCenteredAt(initial, initialStyle, initialText, initialStart, alpha)
    }
    drawTextInkCenteredAt(name, nameStyle, player.nickname, Offset(left + avatar + gap, center.y), alpha)
}

// 첫 화면 사진처럼 가운데를 잘라 원을 꽉 채운다
private fun DrawScope.drawPhotoInCircle(photo: ImageBitmap, center: Offset, radius: Float, alpha: Float) {
    val side = minOf(photo.width, photo.height)
    val diameter = (radius * 2).roundToInt()
    clipPath(Path().apply { addOval(Rect(center, radius)) }) {
        drawImage(
            photo,
            srcOffset = IntOffset((photo.width - side) / 2, (photo.height - side) / 2),
            srcSize = IntSize(side, side),
            dstOffset = IntOffset((center.x - radius).roundToInt(), (center.y - radius).roundToInt()),
            dstSize = IntSize(diameter, diameter),
            alpha = alpha,
        )
    }
}

// 글자 상자가 아니라 보이는 글자의 세로 가운데를 [start].y에 맞춘다
private fun DrawScope.drawTextInkCenteredAt(
    layout: TextLayoutResult,
    style: TextStyle,
    text: String,
    start: Offset,
    alpha: Float,
) {
    val ink = suitInkFor(text)
    val fontSize = style.fontSize.toPx()
    val baseline = start.y + (ink.aboveBaselineEm - ink.belowBaselineEm) * fontSize / 2
    translate(start.x, baseline - layout.firstBaseline) { drawText(layout, alpha = alpha) }
}

@WearPreviewLargeRound
@WearPreviewSmallRound
@Composable
private fun ComboBurstSettledPreview() = PreviewFrame {
    ComboBurstCanvas(10, RunPreviewSamples.party.players[1], photo = null, comboBurstFrame(0.5f))
}

@WearPreviewLargeRound
@Composable
private fun ComboBurstPhotoPreview() = PreviewFrame {
    val player = RunPreviewSamples.party.players[1]
    ComboBurstCanvas(10, player, RunPreviewSamples.brightPhotos[player.userId], comboBurstFrame(0.5f))
}

@WearPreviewLargeRound
@WearPreviewSmallRound
@Composable
private fun ComboBurstLongestPeakPreview() = PreviewFrame {
    ComboBurstCanvas(888, RunPreviewSamples.longestPlayer, photo = null, comboBurstFrame(0.28f))
}
