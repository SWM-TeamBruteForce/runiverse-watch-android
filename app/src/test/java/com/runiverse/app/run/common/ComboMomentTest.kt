package com.runiverse.app.run.common

import com.runiverse.app.phone.ComboKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComboMomentTest {

    @Test
    fun `틱은 효과 설정과 상관없이 작은 순간이다`() {
        // when & then
        assertEquals(ComboMomentSize.SMALL, comboMomentSizeOrNull(ComboKind.TICK, isEffectOn = true))
        assertEquals(ComboMomentSize.SMALL, comboMomentSizeOrNull(ComboKind.TICK, isEffectOn = false))
    }

    @Test
    fun `눈금은 효과를 켜면 큰 순간, 끄면 작은 순간이다`() {
        // when & then
        assertEquals(ComboMomentSize.BIG, comboMomentSizeOrNull(ComboKind.MILESTONE, isEffectOn = true))
        assertEquals(ComboMomentSize.SMALL, comboMomentSizeOrNull(ComboKind.MILESTONE, isEffectOn = false))
    }

    @Test
    fun `끊김은 화면에 띄우지 않는다`() {
        // when & then
        assertNull(comboMomentSizeOrNull(ComboKind.BREAK, isEffectOn = true))
        assertNull(comboMomentSizeOrNull(ComboKind.BREAK, isEffectOn = false))
    }

    @Test
    fun `흰 테두리는 4분의 1 지점까지 밝아지고 70% 지점부터 걷힌다`() {
        // when & then
        assertEquals(0f, comboFlashAlpha(0f), 0.001f)
        assertEquals(0.5f, comboFlashAlpha(0.125f), 0.001f)
        assertEquals(1f, comboFlashAlpha(0.25f), 0.001f)
        assertEquals(1f, comboFlashAlpha(0.6f), 0.001f)
        assertEquals(0.5f, comboFlashAlpha(0.85f), 0.001f)
        assertEquals(0f, comboFlashAlpha(1f), 0.001f)
    }

    @Test
    fun `콤보 글자는 45% 지점에서 1점55배로 가장 크고 끝나면 제 크기다`() {
        // when & then
        assertEquals(1f, comboBumpScale(0f), 0.001f)
        assertEquals(1.55f, comboBumpScale(0.45f), 0.001f)
        assertEquals(1f, comboBumpScale(1f), 0.001f)
    }

    @Test
    fun `콤보 글자는 돌아올 때 처음엔 빨리, 끝에선 천천히 작아진다`() {
        // when
        val early = comboBumpScale(0.45f) - comboBumpScale(0.55f)
        val late = comboBumpScale(0.9f) - comboBumpScale(1f - 0.0001f)

        // then
        assertTrue(early > late)
    }
}
