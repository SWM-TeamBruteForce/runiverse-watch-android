package com.runiverse.app.run

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.MaterialTheme
import com.runiverse.app.ui.theme.RuniverseTheme

@Composable
internal fun RunPreviewFrame(content: @Composable () -> Unit) {
    RuniverseTheme {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
    }
}
