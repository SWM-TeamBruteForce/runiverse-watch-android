package com.runiverse.app.run.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.wear.compose.material3.MaterialTheme

/** 숫자와 단위를 기준선에 맞춰 한 덩어리로 그린다 — 가운데 정렬은 이 덩어리로 한다. */
@Composable
internal fun ValueWithUnit(
    value: String,
    unit: String?,
    valueStyle: TextStyle,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(modifier) {
        AlignedText(value, valueStyle, valueColor, Modifier.alignByBaseline())
        if (unit != null) {
            Spacer(Modifier.width(RunDimensions.VALUE_TO_UNIT_GAP))
            AlignedText(
                unit,
                MaterialTheme.typography.labelSmall,
                MaterialTheme.colorScheme.onSurfaceVariant,
                Modifier.alignByBaseline(),
            )
        }
    }
}
