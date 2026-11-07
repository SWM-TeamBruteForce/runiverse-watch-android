package com.runiverse.app.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlsPageSlotsTest {

    private val widthOf = { text: String -> text.length * 10f }

    private fun slots(longestHints: List<String>, isFontScaled: Boolean) = controlsPageSlots(
        longestHints,
        screenSize = 480f,
        edgeMargin = 25f,
        lineAdvance = 30f,
        hintStartTop = 70f,
        hintMaxShift = 50f,
        pillTopAtDefaultFont = 157f,
        hintToPillGap = 10f,
        isFontScaled = isFontScaled,
        widthOf = widthOf,
    )

    @Test
    fun `폭 안에 들어가는 안내는 한 줄로 두고, 넘치면 두 쪽 폭이 가장 고른 곳에서 나눈다`() {
        // when & then
        assertEquals(listOf("가나 다라"), splitHintLine("가나 다라", maxWidth = 50f, widthOf))
        assertEquals(listOf("가나다라", "마 바"), splitHintLine("가나다라 마 바", maxWidth = 50f, widthOf))
    }

    @Test
    fun `나눌 곳이 없는 안내는 그대로 둔다`() {
        // when & then
        assertEquals(listOf("가나다라마바"), splitHintLine("가나다라마바", maxWidth = 30f, widthOf))
    }

    @Test
    fun `안내 줄은 줄마다 그 높이의 폭으로 나누고 한 줄씩 내려간다`() {
        // given
        val widthAt = { y: Float -> if (y < 30f) 40f else 100f }

        // when
        val lines = placeHintLines(listOf("가나다 라", "사아"), top = 0f, lineAdvance = 30f, widthAt, widthOf)

        // then
        assertEquals(
            listOf(
                PlacedHintLine("가나다", 0, 0f),
                PlacedHintLine("라", 0, 30f),
                PlacedHintLine("사아", 1, 60f),
            ),
            lines,
        )
    }

    @Test
    fun `기본 글자는 안내가 길어도 첫 버튼 자리가 그대로다`() {
        // when
        val slots = slots(listOf("가".repeat(30), "나".repeat(30)), isFontScaled = false)

        // then
        assertEquals(157f, slots.pillTop)
    }

    @Test
    fun `글자를 키우면 첫 버튼이 안내 아래로 밀린다`() {
        // when
        val slots = slots(listOf("가".repeat(40), "나".repeat(40)), isFontScaled = true)

        // then
        assertTrue(slots.pillTop > 157f)
    }

    @Test
    fun `안내는 줄을 안 나눠도 되는 높이까지 내려간다`() {
        // given
        val hints = listOf("가".repeat(30), "나".repeat(30))

        // when
        val slots = slots(hints, isFontScaled = false)

        // then
        assertTrue(slots.hintTop > 70f)
        assertTrue(widthOf(hints[0]) <= usableWidthAt(slots.hintTop, 480f, 25f))
    }

    @Test
    fun `버튼은 양 끝 둥근 부분까지 화면 안쪽에 들어와야 들어온 것으로 본다`() {
        // when & then
        assertTrue(pillFitsRoundScreen(480f, 4f, pillWidth = 255f, pillHeight = 102f, pillTop = 276f))
        assertFalse(pillFitsRoundScreen(480f, 4f, pillWidth = 255f, pillHeight = 102f, pillTop = 395f))
    }
}
