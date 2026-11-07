package com.runiverse.app.run.controls

import com.runiverse.app.run.usableWidthAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlsPageSlotsTest {

    private val widthOf = { text: String -> text.length * 10f }

    private fun slots(widestHints: List<String>, isFontScaled: Boolean) = controlsPageSlots(
        widestHints,
        screenSize = 480f,
        edgeMargin = 25f,
        lineAdvance = 30f,
        hintStartTop = 70f,
        maxHintShift = 50f,
        firstButtonTopAtDefaultFont = 157f,
        hintToButtonGap = 10f,
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
        val lines = placeHintLines(listOf("가나다 라", "사아 자차아"), top = 0f, lineAdvance = 30f, widthAt, widthOf)

        // then -> 둘째 안내는 첫 높이 폭 40엔 안 들어가지만 제 높이 폭 100엔 들어간다
        assertEquals(
            listOf(
                PlacedHintLine("가나다", 0, 0f),
                PlacedHintLine("라", 0, 30f),
                PlacedHintLine("사아 자차아", 1, 60f),
            ),
            lines,
        )
    }

    @Test
    fun `기본 글자는 안내가 길어도 첫 버튼 자리가 그대로다`() {
        // when
        val slots = slots(listOf("가".repeat(30), "나".repeat(30)), isFontScaled = false)

        // then
        assertEquals(157f, slots.firstButtonTop)
    }

    @Test
    fun `글자를 키우면 첫 버튼이 안내 아래로 밀린다`() {
        // when
        val slots = slots(listOf("가".repeat(40), "나".repeat(40)), isFontScaled = true)

        // then -> 안내 두 줄 60과 안내↔버튼 간격 10 아래로 간다
        assertTrue(slots.firstButtonTop > 157f)
        assertEquals(slots.hintTop + 70f, slots.firstButtonTop, 0.01f)
    }

    @Test
    fun `안내가 화면 지름보다 길면 시작 높이에 둔다`() {
        // when
        val slots = slots(listOf("가".repeat(60), "나".repeat(60)), isFontScaled = true)

        // then
        assertEquals(70f, slots.hintTop)
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
        assertTrue(pillFitsRoundScreen(480f, 4f, pillWidth = 255f, pillHeight = 102f, pillTop = 349f))
        // 세로로만 재면 들어오지만 둥근 끝이 화면 곡선을 넘는다
        assertFalse(pillFitsRoundScreen(480f, 4f, pillWidth = 255f, pillHeight = 102f, pillTop = 359f))
    }
}
