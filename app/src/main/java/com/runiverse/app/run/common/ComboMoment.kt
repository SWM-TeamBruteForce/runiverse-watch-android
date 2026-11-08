package com.runiverse.app.run.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.runiverse.app.phone.ComboKind
import kotlin.time.Duration.Companion.milliseconds

/** 작은 순간은 그 사람 자리에서만, 큰 순간은 화면 전체를 덮는다. */
internal enum class ComboMomentSize { SMALL, BIG }

// 폰 배지가 튀는 시간과 같다
internal val SMALL_COMBO_MOMENT_DURATION = 320.milliseconds

private const val FLASH_RISE_END = 0.25f
private const val FLASH_HOLD_END = 0.70f
private const val BUMP_PEAK_AT = 0.45f

// 폰 배지가 튀는 크기와 같다
private const val BUMP_PEAK_SCALE = 1.55f

/** 효과를 끄면 큰 순간 터짐만 빠지고 작은 순간은 남는다. 끊김은 진동만 한다. */
internal fun comboMomentSizeOrNull(kind: ComboKind, isEffectOn: Boolean): ComboMomentSize? = when (kind) {
    ComboKind.TICK -> ComboMomentSize.SMALL
    ComboKind.MILESTONE -> if (isEffectOn) ComboMomentSize.BIG else ComboMomentSize.SMALL
    ComboKind.BREAK -> null
}

/** [momentId]가 올 때마다 0에서 1로 간다. 순간이 없으면 0이다. */
@Composable
internal fun smallComboMomentProgress(momentId: String?): State<Float> {
    val progress = remember { Animatable(0f) }
    // 그리기가 늦게 시작해도 지워질 때 끊기지 않게, 시작한 순간은 끝까지 간다
    var playingId by remember { mutableStateOf<String?>(null) }
    if (momentId != null) playingId = momentId
    LaunchedEffect(playingId) {
        if (playingId == null) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(SMALL_COMBO_MOMENT_DURATION.inWholeMilliseconds.toInt(), easing = LinearEasing))
    }
    return progress.asState()
}

/** 작은 순간 [progress](0~1)에서 고리에 덮는 흰색의 투명도다. */
internal fun comboFlashAlpha(progress: Float): Float = when {
    progress <= 0f || progress >= 1f -> 0f
    progress < FLASH_RISE_END -> progress / FLASH_RISE_END
    progress < FLASH_HOLD_END -> 1f
    else -> (1f - progress) / (1f - FLASH_HOLD_END)
}

/** 작은 순간 [progress](0~1)에서 파티원 장 콤보 글자의 배율이다. */
internal fun comboBumpScale(progress: Float): Float {
    if (progress <= 0f || progress >= 1f) return 1f
    if (progress < BUMP_PEAK_AT) return 1f + (BUMP_PEAK_SCALE - 1f) * (progress / BUMP_PEAK_AT)
    val settling = (progress - BUMP_PEAK_AT) / (1f - BUMP_PEAK_AT)
    val eased = 1f - (1f - settling) * (1f - settling)
    return BUMP_PEAK_SCALE + (1f - BUMP_PEAK_SCALE) * eased
}
