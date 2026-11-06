package com.runiverse.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

@Composable
fun RuniverseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RUNIVERSE_COLOR_SCHEME, typography = runiverseTypography(), content = content)
}
