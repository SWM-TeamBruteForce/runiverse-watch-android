package com.runiverse.app.run.component

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
import com.runiverse.app.run.RunDimensions

private const val ZONE_COUNT = 5
private val ZONE_GAP = 3.dp

// 막대를 가장 긴 심박 폭으로 고정해 100을 넘어도 길이가 안 바뀌게 한다
private const val WIDEST_HEART_RATE = "888"

/** 가장 넓은 심박 덩어리(세 자리 숫자 + `bpm`) 폭이다. 구간 막대 길이로 쓴다. */
@Composable
internal fun widestHeartRateBlockWidth(valueStyle: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val unitStyle = MaterialTheme.typography.labelSmall
    val unit = stringResource(R.string.unit_bpm)
    val density = LocalDensity.current
    return remember(valueStyle, unitStyle, unit, density) {
        val valueWidth = measurer.measure(WIDEST_HEART_RATE, valueStyle).size.width
        val unitWidth = measurer.measure(unit, unitStyle).size.width
        with(density) { valueWidth.toDp() + RunDimensions.VALUE_TO_UNIT_GAP + unitWidth.toDp() }
    }
}

// 사람 색(레인 색)과 섞이지 않게 흰색 한 가지로 지금 구간까지 채운다
@Composable
internal fun HeartRateZoneBar(filledZones: Int, modifier: Modifier = Modifier) {
    val filledColor = MaterialTheme.colorScheme.onBackground
    val emptyColor = MaterialTheme.colorScheme.outline
    Canvas(modifier.boxContentBounds().height(RunDimensions.ZONE_BAR_HEIGHT)) {
        val zoneGap = ZONE_GAP.toPx()
        val zoneWidth = (size.width - zoneGap * (ZONE_COUNT - 1)) / ZONE_COUNT
        val radius = CornerRadius(size.height / 2)
        repeat(ZONE_COUNT) { index ->
            drawRoundRect(
                color = if (index < filledZones) filledColor else emptyColor,
                topLeft = Offset(index * (zoneWidth + zoneGap), 0f),
                size = Size(zoneWidth, size.height),
                cornerRadius = radius,
            )
        }
    }
}
