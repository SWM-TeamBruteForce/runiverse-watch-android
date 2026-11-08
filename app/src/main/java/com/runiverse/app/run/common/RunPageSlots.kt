package com.runiverse.app.run.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.wear.compose.material3.MaterialTheme
import com.runiverse.app.run.component.RunDimensions
import com.runiverse.app.ui.theme.SUIT_NUMERAL_INK
import kotlin.math.abs
import kotlin.math.sqrt

/** 장 공통 줄 높이(화면 위에서 px)다. 큰 숫자 높이로 잡아 장을 넘겨도 줄이 제자리에 있다. */
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

/** 페이스가 두 줄이면 맨 아래 줄 자리까지 내려오므로 콤보 줄은 페이스 바로 아래로 간다. */
internal fun partyComboRowTop(
    slots: RunPageSlots,
    paceInkHeight: Float,
    labelDescent: Float,
    paceRowCount: Int,
    lineGap: Float,
): Float {
    val paceRowsHeight = (paceInkHeight + labelDescent) * paceRowCount + lineGap * (paceRowCount - 1)
    return maxOf(slots.bottomRowTop, slots.belowRowTop + paceRowsHeight + lineGap)
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

@Composable
internal fun currentScreenWidth(): Dp = with(LocalDensity.current) {
    LocalWindowInfo.current.containerSize.width.toDp()
}
