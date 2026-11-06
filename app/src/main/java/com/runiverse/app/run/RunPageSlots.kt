package com.runiverse.app.run

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.wear.compose.material3.MaterialTheme
import com.runiverse.app.ui.theme.SUIT_NUMERAL_INK
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** 장 공통 줄 높이(화면 위에서 px)다. 가운데 글자가 무엇이든 큰 숫자 높이로 잡아 장을 넘겨도 줄이 제자리에 있다. */
internal data class RunPageSlots(
    val topRowBottom: Float,
    val center: Float,
    val bigNumberBaseline: Float,
    val belowRowTop: Float,
    val bottomRowTop: Float,
)

internal fun runPageSlots(
    screenHeight: Float,
    bigNumberInkHeight: Float,
    groupGap: Float,
    avatarRowHeight: Float,
): RunPageSlots {
    val center = screenHeight * RunDimensions.CENTER_Y_FRACTION
    val bigNumberBaseline = center + bigNumberInkHeight / 2
    val belowRowTop = bigNumberBaseline + groupGap
    return RunPageSlots(
        topRowBottom = center - bigNumberInkHeight / 2 - groupGap,
        center = center,
        bigNumberBaseline = bigNumberBaseline,
        belowRowTop = belowRowTop,
        bottomRowTop = belowRowTop + avatarRowHeight,
    )
}

/** 둥근 화면 높이 [y]에서 좌우 여백을 뺀 폭이다. */
internal fun usableWidthAt(y: Float, screenSize: Float, edgeMargin: Float): Float {
    val radius = screenSize / 2 - edgeMargin
    val fromCenter = abs(y - screenSize / 2)
    return if (fromCenter >= radius) 0f else 2 * sqrt(radius * radius - fromCenter * fromCenter)
}

@Composable
internal fun currentRunPageSlots(): RunPageSlots {
    val bigNumberSize = MaterialTheme.typography.numeralExtraLarge.fontSize
    val screenHeight = LocalWindowInfo.current.containerSize.height.toFloat()
    return with(LocalDensity.current) {
        runPageSlots(
            screenHeight = screenHeight,
            bigNumberInkHeight = bigNumberSize.toPx() * SUIT_NUMERAL_INK.aboveBaselineEm,
            groupGap = RunDimensions.GROUP_GAP.toPx(),
            // 콤보가 아닐 때도 고리 자리를 비워 둬 고리가 생겨도 아래 줄이 안 움직인다
            avatarRowHeight = (RunDimensions.AVATAR_SIZE + RunDimensions.COMBO_BORDER_WIDTH +
                RunDimensions.AVATAR_TO_BOTTOM_ROW_GAP).toPx(),
        )
    }
}

internal fun Modifier.contentBottomAt(y: Float): Modifier = placedAt { y - it.contentBottom() }

internal fun Modifier.contentTopAt(y: Float): Modifier = placedAt { y - it.contentTop() }

internal fun Modifier.contentCenterAt(y: Float): Modifier = placedAt { y - (it.contentTop() + it.contentBottom()) / 2f }

/** `bpm`의 p 꼬리처럼 숫자 아래로 내려가는 글자가 있어도 숫자 높이를 다른 장과 맞출 때 쓴다. */
internal fun Modifier.baselineAt(y: Float): Modifier = placedAt { y - it[FirstBaseline] }

private fun Modifier.placedAt(placedTop: (Placeable) -> Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val y = placedTop(placeable).roundToInt()
    layout(placeable.width, constraints.maxHeight) { placeable.place(0, y) }
}
