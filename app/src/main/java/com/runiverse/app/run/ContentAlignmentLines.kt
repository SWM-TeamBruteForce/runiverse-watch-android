package com.runiverse.app.run

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.TextUnit
import com.runiverse.app.ui.theme.InkExtent
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// 시안은 간격을 보이는 내용 사이로 쟀다 — 글자 상자엔 글꼴 위아래 여백이 들어 있다
internal val CONTENT_TOP = HorizontalAlignmentLine(::min)
internal val CONTENT_BOTTOM = HorizontalAlignmentLine(::max)

internal fun Modifier.textContentBounds(fontSize: TextUnit, ink: InkExtent): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val firstBaseline = placeable[FirstBaseline]
        if (firstBaseline == AlignmentLine.Unspecified) {
            return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
        val fontSizePx = fontSize.toPx()
        val bounds = mapOf<AlignmentLine, Int>(
            CONTENT_TOP to firstBaseline - (fontSizePx * ink.aboveBaselineEm).roundToInt(),
            CONTENT_BOTTOM to placeable[LastBaseline] + (fontSizePx * ink.belowBaselineEm).roundToInt(),
        )
        layout(placeable.width, placeable.height, bounds) { placeable.place(0, 0) }
    }

/** 글자가 아닌 것(사진·막대)은 상자 위아래가 곧 보이는 윗변·아랫변이다. */
internal fun Modifier.boxContentBounds(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val bounds = mapOf<AlignmentLine, Int>(CONTENT_TOP to 0, CONTENT_BOTTOM to placeable.height)
    layout(placeable.width, placeable.height, bounds) { placeable.place(0, 0) }
}

internal fun Placeable.contentTop(): Int = this[CONTENT_TOP].takeIf { it != AlignmentLine.Unspecified } ?: 0

internal fun Placeable.contentBottom(): Int = this[CONTENT_BOTTOM].takeIf { it != AlignmentLine.Unspecified } ?: height
