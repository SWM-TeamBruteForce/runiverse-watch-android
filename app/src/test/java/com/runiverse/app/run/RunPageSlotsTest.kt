package com.runiverse.app.run

import org.junit.Assert.assertEquals
import org.junit.Test

class RunPageSlotsTest {

    @Test
    fun `줄 높이는 큰 숫자 위아래로 묶음 간격을 두고, 맨 아래 줄은 사진 칸 아래다`() {
        // when
        val slots = runPageSlots(screenHeight = 480f, bigNumberInkHeight = 80f, groupGap = 30f, avatarRowHeight = 60f)

        // then -> 가운데 480 × 207/480 = 207
        assertEquals(
            RunPageSlots(
                topRowBottom = 137f,
                center = 207f,
                bigNumberBaseline = 247f,
                belowRowTop = 277f,
                bottomRowTop = 337f,
            ),
            slots,
        )
    }

    @Test
    fun `둥근 화면 폭은 가운데가 가장 넓고 위아래로 갈수록 좁다`() {
        // when & then -> 반지름 100, 가운데에서 60 떨어지면 2 × √(100² − 60²) = 160
        assertEquals(200f, usableWidthAt(y = 110f, screenSize = 220f, edgeMargin = 10f), 0.001f)
        assertEquals(160f, usableWidthAt(y = 170f, screenSize = 220f, edgeMargin = 10f), 0.001f)
        assertEquals(0f, usableWidthAt(y = 215f, screenSize = 220f, edgeMargin = 10f), 0.001f)
    }
}
