package com.runiverse.app.run

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.runiverse.app.run.common.RunPreviewSamples
import com.runiverse.app.ui.component.PreviewFrame
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val BEFORE_END_MS = 1_100L
private const val AFTER_END_MS = 200L

@RunWith(RobolectricTestRunner::class)
class ComboBurstScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val player = RunPreviewSamples.party.players[1]

    @Test
    fun `콤보 수와 그 사람 이름을 읽어 준다`() {
        // when
        show(onFinished = {})

        // then
        composeRule.onNodeWithContentDescription("10 COMBO ${player.nickname}").assertExists()
    }

    @Test
    fun `1점2초를 다 그린 뒤에 끝났다고 알린다`() {
        // given
        var isFinished = false
        show(onFinished = { isFinished = true })

        // when
        composeRule.mainClock.advanceTimeBy(BEFORE_END_MS)

        // then
        assertFalse(isFinished)
        composeRule.mainClock.advanceTimeBy(AFTER_END_MS)
        assertTrue(isFinished)
    }

    private fun show(onFinished: () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { PreviewFrame { ComboBurstScreen(10, player, onFinished) } }
        composeRule.mainClock.advanceTimeByFrame()
    }
}
