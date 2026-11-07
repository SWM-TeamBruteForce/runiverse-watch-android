package com.runiverse.app.ui.theme

import androidx.compose.ui.graphics.Color

/** 나는 늘 0번 레인 색이다. */
const val MY_LANE_COLOR_SLOT = 0

/** 폰과 같아야 같은 사람이 같은 색으로 보인다. 0은 나, 1~3은 파티원이다. */
private val LANE_COLORS = listOf(
    Color(0xFF227DFF),
    Color(0xFFFF8335),
    Color(0xFFFF4FA3),
    Color(0xFF33D17A),
)

fun laneColor(colorSlot: Int): Color = LANE_COLORS[colorSlot.mod(LANE_COLORS.size)]
