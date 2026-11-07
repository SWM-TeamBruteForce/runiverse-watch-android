package com.runiverse.app.run.controls

import com.runiverse.app.run.usableWidthAt
import kotlin.math.hypot
import kotlin.math.sqrt

/** 안내와 첫 버튼의 높이(화면 위에서 px)다. 안내 길이로 안 움직인다. */
internal data class ControlsPageSlots(val hintTop: Float, val firstButtonTop: Float)

internal data class PlacedHintLine(val text: String, val hintIndex: Int, val top: Float)

/** 안 들어가면 두 쪽 폭이 가장 고른 곳에서 나눈다. */
internal fun splitHintLine(text: String, maxWidth: Float, widthOf: (String) -> Float): List<String> {
    if (widthOf(text) <= maxWidth) return listOf(text)
    val words = text.split(' ')
    val splits = (1 until words.size).map { index ->
        listOf(words.take(index).joinToString(" "), words.drop(index).joinToString(" "))
    }
    return splits.minByOrNull { parts -> parts.maxOf(widthOf) } ?: listOf(text)
}

internal fun placeHintLines(
    hints: List<String>,
    top: Float,
    lineAdvance: Float,
    widthAt: (Float) -> Float,
    widthOf: (String) -> Float,
): List<PlacedHintLine> {
    var y = top
    return hints.flatMapIndexed { hintIndex, hint ->
        splitHintLine(hint, widthAt(y), widthOf).map { line ->
            PlacedHintLine(line, hintIndex, y).also { y += lineAdvance }
        }
    }
}

/** 가장 넓은 안내 기준으로 자리를 정한다. 첫 버튼은 키운 글자에서만 밀린다. */
internal fun controlsPageSlots(
    widestHints: List<String>,
    screenSize: Float,
    edgeMargin: Float,
    lineAdvance: Float,
    hintStartTop: Float,
    maxHintShift: Float,
    firstButtonTopAtDefaultFont: Float,
    hintToButtonGap: Float,
    isFontScaled: Boolean,
    widthOf: (String) -> Float,
): ControlsPageSlots {
    val widthAt = { y: Float -> usableWidthAt(y, screenSize, edgeMargin) }
    val hintTop = firstTopWhereHintsFit(widestHints, hintStartTop, maxHintShift, lineAdvance, widthAt, widthOf)
        ?: topWhereFirstLineFits(widthOf(widestHints.first()), screenSize, edgeMargin, hintStartTop)
    val lines = placeHintLines(widestHints, hintTop, lineAdvance, widthAt, widthOf)
    val hintsEnd = lines.last().top + lineAdvance
    val firstButtonTop = if (isFontScaled) {
        maxOf(firstButtonTopAtDefaultFont, hintsEnd + hintToButtonGap)
    } else {
        firstButtonTopAtDefaultFont
    }
    return ControlsPageSlots(hintTop, firstButtonTop)
}

private fun firstTopWhereHintsFit(
    hints: List<String>,
    startTop: Float,
    maxShift: Float,
    lineAdvance: Float,
    widthAt: (Float) -> Float,
    widthOf: (String) -> Float,
): Float? {
    var top = startTop
    while (top <= startTop + maxShift) {
        val everyHintFits = hints.withIndex().all { (index, hint) ->
            widthOf(hint) <= widthAt(top + index * lineAdvance)
        }
        if (everyHintFits) return top
        top += 1f
    }
    return null
}

// 다 안 들어가면 첫 줄만이라도 나누지 않는다
private fun topWhereFirstLineFits(firstHintWidth: Float, screenSize: Float, edgeMargin: Float, startTop: Float): Float {
    val radius = screenSize / 2 - edgeMargin
    val halfWidth = firstHintWidth / 2
    if (halfWidth >= radius) return startTop
    return maxOf(startTop, screenSize / 2 - sqrt(radius * radius - halfWidth * halfWidth) + 0.5f)
}

/** 양 끝 둥근 부분까지 [edgeMargin] 안쪽에 들어오는지 본다. */
internal fun pillFitsRoundScreen(
    screenSize: Float,
    edgeMargin: Float,
    pillWidth: Float,
    pillHeight: Float,
    pillTop: Float,
): Boolean {
    val capCenterFromScreenCenterX = pillWidth / 2 - pillHeight / 2
    val capCenterFromScreenCenterY = pillTop + pillHeight / 2 - screenSize / 2
    val farthestFromCenter = hypot(capCenterFromScreenCenterX, capCenterFromScreenCenterY) + pillHeight / 2
    return farthestFromCenter <= screenSize / 2 - edgeMargin
}
