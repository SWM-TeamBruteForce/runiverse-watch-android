package com.runiverse.app.run

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.wear.compose.material3.Text
import com.runiverse.app.ui.theme.suitInkFor

private const val DOUBLE_HYPHEN = "--"
private const val KERNING_OFF = "'kern' 0"

/** 한 줄 글자다. 간격 계산에 쓰는 잉크 윗변·아랫변을 함께 내보낸다 — 높이는 글자 내용(한글·꼬리 글자)으로 고른다. */
@Composable
internal fun AlignedText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    // SUIT는 하이픈 둘을 자간으로 붙여 막대처럼 보인다
    val textStyle = if (DOUBLE_HYPHEN in text) {
        style.copy(fontFeatureSettings = listOfNotNull(style.fontFeatureSettings, KERNING_OFF).joinToString(", "))
    } else {
        style
    }
    Text(
        text,
        modifier.textContentBounds(style.fontSize, suitInkFor(text)),
        color = color,
        style = textStyle,
        maxLines = 1,
    )
}
