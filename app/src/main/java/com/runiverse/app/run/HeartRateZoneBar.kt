package com.runiverse.app.run

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import com.runiverse.app.R

private const val ZONE_COUNT = 5
private val SEGMENT_GAP = 3.dp

// 심박은 세 자리가 가장 길다 — 막대를 이 폭으로 고정해 100을 넘는 순간 길이가 바뀌지 않게 한다
private const val LONGEST_HEART_RATE = "888"

/** 가장 긴 심박 덩어리(세 자리 숫자 + `bpm`) 폭이다. 구간 막대 길이로 쓴다. */
@Composable
internal fun longestHeartRateBlockWidth(valueStyle: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val unitStyle = MaterialTheme.typography.labelSmall
    val unit = stringResource(R.string.unit_bpm)
    val density = LocalDensity.current
    return remember(valueStyle, unitStyle, unit, density) {
        val valueWidth = measurer.measure(LONGEST_HEART_RATE, valueStyle).size.width
        val unitWidth = measurer.measure(unit, unitStyle).size.width
        with(density) { valueWidth.toDp() + RunDimensions.VALUE_UNIT_GAP + unitWidth.toDp() }
    }
}

// 사람 색(레인 색)과 섞이지 않게 흰색 한 가지로 지금 구간까지 채운다
@Composable
internal fun HeartRateZoneBar(filledZones: Int, modifier: Modifier = Modifier) {
    val filledColor = MaterialTheme.colorScheme.onBackground
    val emptyColor = MaterialTheme.colorScheme.outline
    Canvas(modifier.boxContentBounds().height(RunDimensions.ZONE_BAR_HEIGHT)) {
        val gap = SEGMENT_GAP.toPx()
        val segment = (size.width - gap * (ZONE_COUNT - 1)) / ZONE_COUNT
        val radius = CornerRadius(size.height / 2)
        repeat(ZONE_COUNT) { index ->
            drawRoundRect(
                color = if (index < filledZones) filledColor else emptyColor,
                topLeft = Offset(index * (segment + gap), 0f),
                size = Size(segment, size.height),
                cornerRadius = radius,
            )
        }
    }
}
