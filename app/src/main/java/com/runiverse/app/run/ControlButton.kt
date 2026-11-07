package com.runiverse.app.run

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme

private const val ICON_TO_FONT_RATIO = 0.62f
private const val PAUSE_ICON_BAR_FRACTION = 0.28f
private const val PAUSE_ICON_LEFT_BAR_FRACTION = 0.1f
private const val PAUSE_ICON_RIGHT_BAR_FRACTION = 0.62f
private val ICON_TO_LABEL_GAP = 6.dp

internal val PILL_HEIGHT = 48.dp

internal enum class ControlIcon { PAUSE, PLAY, STOP }

@Composable
internal fun ControlButton(
    label: String,
    icon: ControlIcon?,
    background: Color,
    width: Dp,
    isEnabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(width, PILL_HEIGHT)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = isEnabled, role = Role.Button, onClick = onClick),
    ) {
        ButtonContent(label, icon, MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
internal fun ButtonContent(label: String, icon: ControlIcon?, color: Color) {
    val style = MaterialTheme.typography.bodyMedium
    val density = LocalDensity.current
    val iconSize = with(density) { style.fontSize.toDp() } * ICON_TO_FONT_RATIO
    val pillCenter = with(density) { PILL_HEIGHT.toPx() } / 2
    Row(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(ICON_TO_LABEL_GAP, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Canvas(Modifier.size(iconSize)) { drawControlIcon(icon, color) }
        AlignedText(label, style, color, Modifier.contentCenterAt(pillCenter))
    }
}

private fun DrawScope.drawControlIcon(icon: ControlIcon, color: Color) {
    when (icon) {
        ControlIcon.PAUSE -> {
            val barSize = Size(size.width * PAUSE_ICON_BAR_FRACTION, size.height)
            drawRect(color, topLeft = Offset(size.width * PAUSE_ICON_LEFT_BAR_FRACTION, 0f), size = barSize)
            drawRect(color, topLeft = Offset(size.width * PAUSE_ICON_RIGHT_BAR_FRACTION, 0f), size = barSize)
        }

        ControlIcon.PLAY -> drawPath(
            Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, size.height / 2)
                lineTo(0f, size.height)
                close()
            },
            color,
        )

        ControlIcon.STOP -> drawRect(color)
    }
}
