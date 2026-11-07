package com.runiverse.app.run

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp

/** 앞 줄의 보이는 아랫변과 다음 줄의 보이는 윗변 사이를 [contentGap]으로 두고 쌓는다. */
@Composable
internal fun ContentBoundsColumn(contentGap: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(loose) }
        val contentGapPx = contentGap.roundToPx()
        val tops = ArrayList<Int>(placeables.size)
        var previousBottom = 0
        placeables.forEachIndexed { index, placeable ->
            val top = if (index == 0) 0 else previousBottom + contentGapPx - placeable.contentTop()
            tops += top
            previousBottom = top + placeable.contentBottom()
        }
        val minTop = tops.minOrNull() ?: 0
        val width = placeables.maxOfOrNull { it.width } ?: 0
        val height = placeables.indices.maxOfOrNull { tops[it] + placeables[it].height - minTop } ?: 0
        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                placeable.place((width - placeable.width) / 2, tops[index] - minTop)
            }
        }
    }
}
