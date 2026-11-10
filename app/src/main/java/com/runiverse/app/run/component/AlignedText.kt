package com.runiverse.app.run.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.Text
import com.runiverse.app.ui.theme.suitInkFor

/** 한 줄 글자다. 간격 계산에 쓰는 잉크 윗변·아랫변을 함께 내보낸다. */
@Composable
internal fun AlignedText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text,
        modifier.textContentBounds(style.fontSize, suitInkFor(text)),
        color = color,
        style = style,
        overflow = overflow,
        maxLines = 1,
    )
}
