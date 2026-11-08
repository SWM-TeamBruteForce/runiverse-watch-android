package com.runiverse.app.run.common

import kotlin.time.Duration.Companion.milliseconds

// 폰 콤보 임팩트와 같은 길이다
internal val BIG_COMBO_MOMENT_DURATION = 1200.milliseconds

/** 큰 순간 한 장면이다. 각도는 시계 방향이 양수다. */
internal data class ComboBurstFrame(
    val coverAlpha: Float,
    val textScale: Float,
    val textAlpha: Float,
    val textDegrees: Float,
    val ringScale: Float,
    val ringAlpha: Float,
)

/** 구간 비중과 시작·끝 값이다. Flutter TweenSequence처럼 이어 붙인다. */
private class Segment(val weight: Float, val from: Float, val to: Float, val easesOut: Boolean = false)

// 폰 임팩트를 따르고 끝만 다르다 — 날아갈 배지 자리가 없어 글자가 먼저 사라지고 덮개가 빨리 걷힌다
private val COVER = listOf(Segment(10f, 0f, 1f), Segment(86f, 1f, 1f), Segment(4f, 1f, 0f))
private val TEXT_SCALE = listOf(Segment(28f, 0.3f, 1.3f, easesOut = true), Segment(14f, 1.3f, 1f), Segment(58f, 1f, 1.02f))
private val TEXT_ALPHA = listOf(Segment(20f, 0f, 1f), Segment(70f, 1f, 1f), Segment(6f, 1f, 0f), Segment(4f, 0f, 0f))
private val TEXT_DEGREES = listOf(Segment(28f, -10f, 3f), Segment(14f, 3f, 0f), Segment(58f, 0f, 0f))

private const val RING_END = 0.56f
private const val RING_START_SCALE = 0.35f
private const val RING_END_SCALE = 2.4f
private const val RING_START_ALPHA = 0.95f

internal fun comboBurstFrame(progress: Float): ComboBurstFrame {
    val ringProgress = (progress / RING_END).coerceIn(0f, 1f)
    return ComboBurstFrame(
        coverAlpha = COVER.valueAt(progress),
        textScale = TEXT_SCALE.valueAt(progress),
        textAlpha = TEXT_ALPHA.valueAt(progress),
        textDegrees = TEXT_DEGREES.valueAt(progress),
        ringScale = RING_START_SCALE + (RING_END_SCALE - RING_START_SCALE) * easeOut(ringProgress),
        ringAlpha = RING_START_ALPHA * (1f - ringProgress),
    )
}

private fun List<Segment>.valueAt(progress: Float): Float {
    val clamped = progress.coerceIn(0f, 1f)
    val total = sumOf { it.weight.toDouble() }.toFloat()
    var start = 0f
    for (segment in this) {
        val end = start + segment.weight / total
        if (clamped <= end) {
            val local = if (end > start) (clamped - start) / (end - start) else 1f
            val eased = if (segment.easesOut) easeOut(local) else local
            return segment.from + (segment.to - segment.from) * eased
        }
        start = end
    }
    return last().to
}

private fun easeOut(progress: Float): Float = 1f - (1f - progress) * (1f - progress)
