package com.runiverse.app.run.common

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.runiverse.app.phone.ComboKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ComboMomentTest {

    @get:Rule
    val composeRule = createComposeRule()

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

    @Test
    fun `밝은 동안엔 순간 번호가 지워져도 끝까지 간다`() {
        // given
        var momentId by mutableStateOf<String?>("e1")
        lateinit var progress: State<Float>
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { progress = smallComboMomentProgress(momentId, isDisplayOn = true) }
        composeRule.mainClock.advanceTimeBy(100)
        assertTrue(progress.value > 0f)

        // when
        momentId = null
        composeRule.mainClock.advanceTimeBy(400)

        // then
        assertEquals(1f, progress.value, 0f)
    }

    @Test
    fun `작은 순간 중에 화면이 어두워지면 바로 끊는다`() {
        // given
        var momentId by mutableStateOf<String?>("e1")
        var isDisplayOn by mutableStateOf(true)
        lateinit var progress: State<Float>
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { progress = smallComboMomentProgress(momentId, isDisplayOn) }
        composeRule.mainClock.advanceTimeBy(100)
        assertTrue(progress.value > 0f)

        // when -> 어두워지면 MainActivity가 순간 번호도 함께 지운다
        momentId = null
        isDisplayOn = false
        composeRule.mainClock.advanceTimeBy(50)

        // then
        assertEquals(0f, progress.value, 0f)
    }
}
