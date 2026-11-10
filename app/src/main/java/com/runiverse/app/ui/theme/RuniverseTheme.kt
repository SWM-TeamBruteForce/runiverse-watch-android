package com.runiverse.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.wear.compose.material3.MaterialTheme

// 1.3배를 넘으면 글자가 잘리는 화면이 있다 — 워치8 최대인 1.3배에서 멈춘다
private const val MAX_FONT_SCALE = 1.3f

@Composable
fun RuniverseTheme(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val cappedDensity =
        if (density.fontScale > MAX_FONT_SCALE) Density(density.density, MAX_FONT_SCALE) else density
    CompositionLocalProvider(LocalDensity provides cappedDensity) {
        MaterialTheme(colorScheme = RUNIVERSE_COLOR_SCHEME, typography = runiverseTypography(), content = content)
    }
}
