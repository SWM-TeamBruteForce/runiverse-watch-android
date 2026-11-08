package com.runiverse.app.run.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComboBurstTest {

    private val delta = 0.001f

    @Test
    fun `덮개는 처음 10%에 덮이고 마지막 4%에 걷힌다`() {
        // when & then
        assertEquals(0f, comboBurstFrame(0f).coverAlpha, delta)
        assertEquals(1f, comboBurstFrame(0.1f).coverAlpha, delta)
        assertEquals(1f, comboBurstFrame(0.96f).coverAlpha, delta)
        assertEquals(0f, comboBurstFrame(1f).coverAlpha, delta)
    }

    @Test
    fun `글자는 28%에 1점3배로 튀고 42%에 제 크기로 앉은 뒤 조금씩 커진다`() {
        // when & then
        assertEquals(0.3f, comboBurstFrame(0f).textScale, delta)
        assertEquals(1.3f, comboBurstFrame(0.28f).textScale, delta)
        assertEquals(1f, comboBurstFrame(0.42f).textScale, delta)
        assertEquals(1.02f, comboBurstFrame(1f).textScale, delta)
    }

    @Test
    fun `글자는 덮개보다 먼저 사라진다`() {
        // when
        val textGone = comboBurstFrame(0.96f)

        // then
        assertEquals(0f, textGone.textAlpha, delta)
        assertEquals(1f, textGone.coverAlpha, delta)
        assertEquals(1f, comboBurstFrame(0.9f).textAlpha, delta)
    }

    @Test
    fun `글자는 왼쪽으로 기울어 시작해 오른쪽으로 넘어갔다가 바로 선다`() {
        // when & then
        assertEquals(-10f, comboBurstFrame(0f).textDegrees, delta)
        assertEquals(3f, comboBurstFrame(0.28f).textDegrees, delta)
        assertEquals(0f, comboBurstFrame(0.42f).textDegrees, delta)
        assertEquals(0f, comboBurstFrame(1f).textDegrees, delta)
    }

    @Test
    fun `흰 고리는 앞 56% 동안 퍼지며 사라진다`() {
        // when & then
        assertEquals(0.35f, comboBurstFrame(0f).ringScale, delta)
        assertEquals(0.95f, comboBurstFrame(0f).ringAlpha, delta)
        assertEquals(2.4f, comboBurstFrame(0.56f).ringScale, delta)
        assertEquals(0f, comboBurstFrame(0.56f).ringAlpha, delta)
        assertEquals(2.4f, comboBurstFrame(1f).ringScale, delta)
    }

    @Test
    fun `튀는 구간은 처음엔 빨리, 끝에선 천천히 커진다`() {
        // when
        val early = comboBurstFrame(0.05f).textScale - comboBurstFrame(0f).textScale
        val late = comboBurstFrame(0.28f).textScale - comboBurstFrame(0.23f).textScale

        // then
        assertTrue(early > late)
    }
}
