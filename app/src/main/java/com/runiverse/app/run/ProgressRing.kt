package com.runiverse.app.run

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import kotlin.math.asin

private val RING_STROKE_WIDTH = 5.dp
private val RING_CENTER_INSET = 6.dp

// 위쪽 시계와 겹치지 않게 12시 양옆 22.5도씩 비운다(시안에서 잰 값)
private const val RING_OPENING_DEGREES = 45f
private const val TWELVE_O_CLOCK_DEGREES = -90f
private const val RING_START_DEGREES = TWELVE_O_CLOCK_DEGREES + RING_OPENING_DEGREES / 2
private const val RING_SWEEP_DEGREES = 360f - RING_OPENING_DEGREES

/** 12시 오른쪽 끝에서 시계 방향으로 [progress](0~1)만큼 [color]로 채운다. */
@Composable
internal fun ProgressRing(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.fillMaxSize()) {
        val stroke = RING_STROKE_WIDTH.toPx()
        val inset = RING_CENTER_INSET.toPx()
        val topLeft = Offset(inset, inset)
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        drawArc(
            trackColor,
            RING_START_DEGREES,
            RING_SWEEP_DEGREES,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(stroke),
        )
        // 둥근 끝은 두께 절반만큼 더 나간다 — 빈칸을 지키려고 그 각도만큼 안쪽에서 시작·끝낸다
        val capDegrees = Math.toDegrees(asin((stroke / 2) / (arcSize.width / 2)).toDouble()).toFloat()
        val sweep = progress.coerceIn(0f, 1f) * (RING_SWEEP_DEGREES - capDegrees * 2)
        if (sweep > 0f) {
            drawArc(
                color,
                RING_START_DEGREES + capDegrees,
                sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
    }
}
