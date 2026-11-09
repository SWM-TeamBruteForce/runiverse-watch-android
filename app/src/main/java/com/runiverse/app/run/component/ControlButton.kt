package com.runiverse.app.run.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme

private const val ICON_TO_FONT_RATIO = 0.62f
private const val PAUSE_ICON_BAR_FRACTION = 0.28f
private const val PAUSE_ICON_LEFT_BAR_FRACTION = 0.1f
private const val PAUSE_ICON_RIGHT_BAR_FRACTION = 0.62f
private const val CHECK_ICON_TURN_FRACTION = 0.38f
private const val CHECK_ICON_ARM_FRACTION = 0.35f
private const val UP_ARROW_ICON_SHAFT_END_FRACTION = 0.72f
private const val UP_ARROW_ICON_HEAD_FRACTION = 0.32f
private val LINE_ICON_STROKE_WIDTH = 1.5.dp
private val ICON_TO_LABEL_GAP = 6.dp
private const val BOTTOM_PILL_WIDTH_FRACTION = 204f / 480f

// 작은 화면에서 알약이 동그래지지 않게 한다
private val MIN_BOTTOM_PILL_WIDTH = 96.dp

internal val PILL_HEIGHT = 48.dp
internal val BOTTOM_PILL_BOTTOM_PADDING = 8.dp

internal enum class ControlIcon { PAUSE, PLAY, STOP, CHECK, UP_ARROW }

@Composable
internal fun ControlButton(
    label: String,
    icon: ControlIcon?,
    background: Color,
    width: Dp,
    isEnabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(width, PILL_HEIGHT)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = isEnabled, role = Role.Button, onClick = onClick),
    ) {
        ButtonContent(label, icon, MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
internal fun ButtonContent(label: String, icon: ControlIcon?, color: Color) {
    val style = MaterialTheme.typography.bodyMedium
    val density = LocalDensity.current
    val iconSize = with(density) { style.fontSize.toDp() } * ICON_TO_FONT_RATIO
    val pillCenter = with(density) { PILL_HEIGHT.toPx() } / 2
    Row(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(ICON_TO_LABEL_GAP, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Canvas(Modifier.size(iconSize)) { drawControlIcon(icon, color) }
        AlignedText(label, style, color, Modifier.contentCenterAt(pillCenter))
    }
}

private fun DrawScope.drawControlIcon(icon: ControlIcon, color: Color) {
    when (icon) {
        ControlIcon.PAUSE -> {
            val barSize = Size(size.width * PAUSE_ICON_BAR_FRACTION, size.height)
            drawRect(color, topLeft = Offset(size.width * PAUSE_ICON_LEFT_BAR_FRACTION, 0f), size = barSize)
            drawRect(color, topLeft = Offset(size.width * PAUSE_ICON_RIGHT_BAR_FRACTION, 0f), size = barSize)
        }

        ControlIcon.PLAY -> drawPath(
            Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, size.height / 2)
                lineTo(0f, size.height)
                close()
            },
            color,
        )

        ControlIcon.STOP -> drawRect(color)

        ControlIcon.CHECK -> drawPath(
            Path().apply {
                val middle = size.height / 2
                val arm = size.height * CHECK_ICON_ARM_FRACTION
                moveTo(0f, middle)
                lineTo(size.width * CHECK_ICON_TURN_FRACTION, middle + arm)
                lineTo(size.width, middle - arm)
            },
            color,
            style = lineIconStroke(),
        )

        ControlIcon.UP_ARROW -> drawPath(
            Path().apply {
                val center = size.width / 2
                val head = size.width * UP_ARROW_ICON_HEAD_FRACTION
                moveTo(center, size.height * UP_ARROW_ICON_SHAFT_END_FRACTION)
                lineTo(center, 0f)
                moveTo(center - head, head)
                lineTo(center, 0f)
                lineTo(center + head, head)
                moveTo(0f, size.height)
                lineTo(size.width, size.height)
            },
            color,
            style = lineIconStroke(),
        )
    }
}

private fun DrawScope.lineIconStroke() =
    Stroke(LINE_ICON_STROKE_WIDTH.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

/** 화면 맨 아래에 하나만 두는 알약 버튼의 폭이다. */
internal fun bottomPillWidth(screenWidth: Dp): Dp = maxOf(screenWidth * BOTTOM_PILL_WIDTH_FRACTION, MIN_BOTTOM_PILL_WIDTH)
