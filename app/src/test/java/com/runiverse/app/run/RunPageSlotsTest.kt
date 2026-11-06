package com.runiverse.app.run

import org.junit.Assert.assertEquals
import org.junit.Test

class RunPageSlotsTest {

    @Test
    fun `줄 높이는 큰 숫자 위아래로 묶음 간격을 두고, 맨 아래 줄은 사진 칸 아래다`() {
        // when
        val slots = runPageSlots(screenHeight = 480f, bigNumberInkHeight = 80f, groupGap = 30f, avatarRowHeight = 60f)

        // then -> 가운데 480 × 207/480 = 207
        assertEquals(RunPageSlots(topRowBottom = 137f, center = 207f, belowRowTop = 277f, bottomRowTop = 337f), slots)
    }
}
