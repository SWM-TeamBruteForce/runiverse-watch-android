package com.runiverse.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class RuniverseTypographyTest {

    @Test
    fun `숫자와 꼬리 없는 단위는 숫자 높이로 잰다`() {
        // when & then
        assertEquals(SUIT_NUMERAL_INK, suitInkFor("5'32\""))
        assertEquals(SUIT_NUMERAL_INK, suitInkFor("km"))
        assertEquals(SUIT_NUMERAL_INK, suitInkFor("kcal"))
    }

    @Test
    fun `꼬리 있는 라틴 글자는 기준선 아래 깊이를 더한다`() {
        // when & then
        assertEquals(SUIT_DESCENDER_INK, suitInkFor("bpm"))
        assertEquals(SUIT_DESCENDER_INK, suitInkFor("spm"))
    }

    @Test
    fun `한글이 들어 있으면 한글 높이로 잰다`() {
        // when & then
        assertEquals(SUIT_HANGUL_INK, suitInkFor("폰 연결 확인 중"))
        assertEquals(SUIT_HANGUL_INK, suitInkFor("멈춤"))
    }

    @Test
    fun `한글과 꼬리 글자가 섞이면 위아래 각각 큰 쪽을 쓴다`() {
        // when & then -> 위는 한글, 아래는 p 꼬리
        assertEquals(InkExtent(aboveBaselineEm = 0.814f, belowBaselineEm = 0.216f), suitInkFor("심박 bpm"))
    }
}
