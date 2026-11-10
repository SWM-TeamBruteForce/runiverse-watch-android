package com.runiverse.app.run.component

import androidx.compose.ui.unit.dp

/** 간격은 보이는 글자 사이로 잰다. 부품 하나만 쓰는 크기는 그 부품 파일에 둔다. */
internal object RunDimensions {
    // 큰 숫자의 가운데 높이와 위 줄 양끝 여백은 화면 크기에 대한 비율로 둔다
    const val CENTER_Y_FRACTION = 207f / 480f
    const val TOP_ROW_SIDE_FRACTION = 86f / 480f

    // 끊긴 동안 마지막 값에 멈춘 내용을 흐리게 그린다
    const val STALE_CONTENT_ALPHA = 0.4f

    val GROUP_GAP = 13.dp
    val LINE_GAP = 7.dp
    val VALUE_TO_UNIT_GAP = 2.dp
    val VALUE_TO_ZONE_BAR_GAP = 4.dp
    val ZONE_BAR_HEIGHT = 3.dp
    val AVATAR_SIZE = 24.5.dp
    val COMBO_BORDER_WIDTH = 2.dp
    val AVATAR_TO_BOTTOM_ROW_GAP = 4.dp

    // 링 안쪽 끝(화면 끝에서 8.5dp)에서 글자를 4dp 더 띄운다
    val TEXT_EDGE_MARGIN = 12.5.dp
}
