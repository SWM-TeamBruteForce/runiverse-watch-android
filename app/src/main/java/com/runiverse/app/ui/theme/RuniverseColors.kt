package com.runiverse.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme

/**
 * 러닝 화면 색이다. 페이스 같은 큰 수만 순백(onBackground), 수치는 #FAFAFA(onSurface)다.
 * 가이드 라벨색 #575757은 대비가 2.9:1이라 단위·보조 글자는 흰색 70%(onSurfaceVariant)로 쓴다.
 */
internal val RUNIVERSE_COLOR_SCHEME = ColorScheme(
    background = Color.Black,
    onBackground = Color.White,
    onSurface = Color(0xFFFAFAFA),
    onSurfaceVariant = Color.White.copy(alpha = 0.7f),
    outline = Color.White.copy(alpha = 0.25f),
    outlineVariant = Color(0xFF2A2A2A),
)
