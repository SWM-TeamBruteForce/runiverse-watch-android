package com.runiverse.app.run

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import com.runiverse.app.R
import com.runiverse.app.phone.RunState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RunScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val solo = RunPreviewSamples.solo
    private val party = RunPreviewSamples.party
    private val elapsedTime = formatElapsedTime(party.me.elapsedSeconds)
    private val firstPlayerName = party.players.first().nickname
    private val backDispatcher get() = composeRule.activity.onBackPressedDispatcher

    @Test
    fun `왼쪽으로 쓸면 내 기록 장으로 간다`() {
        // given
        show(solo)

        // when
        swipe { swipeLeft() }

        // then
        composeRule.onNodeWithText(text(R.string.unit_kcal)).assertIsDisplayed()
    }

    @Test
    fun `오른쪽으로 쓸면 조작 장으로 간다`() {
        // given
        show(solo)

        // when
        swipe { swipeRight() }

        // then
        composeRule.onNodeWithText(text(R.string.controls_pause)).assertIsDisplayed()
    }

    @Test
    fun `조작 장에서 왼쪽으로 쓸면 첫 화면으로 돌아온다`() {
        // given
        show(solo)
        swipe { swipeRight() }

        // when
        swipe { swipeLeft() }

        // then
        composeRule.onNodeWithText(elapsedTime).assertIsDisplayed()
    }

    @Test
    fun `내 기록 장에서 오른쪽으로 쓸면 첫 화면으로 돌아온다`() {
        // given
        show(solo)
        swipe { swipeLeft() }

        // when
        swipe { swipeRight() }

        // then
        composeRule.onNodeWithText(elapsedTime).assertIsDisplayed()
    }

    @Test
    fun `위로 쓸면 파티원 장으로 가고 아래로 쓸면 돌아온다`() {
        // given
        show(party)

        // when
        swipe { swipeUp() }

        // then
        composeRule.onNodeWithText(firstPlayerName).assertIsDisplayed()

        // when
        swipe { swipeDown() }

        // then
        composeRule.onNodeWithText(firstPlayerName).assertIsNotDisplayed()
        composeRule.onNodeWithText(elapsedTime).assertIsDisplayed()
    }

    @Test
    fun `위로 계속 쓸면 다음 파티원 장으로 간다`() {
        // given
        show(party)
        swipe { swipeUp() }

        // when
        swipe { swipeUp() }

        // then
        composeRule.onNodeWithText(party.players[1].nickname).assertIsDisplayed()
    }

    @Test
    fun `솔로는 위로 쓸어도 첫 화면에 머문다`() {
        // given
        show(solo)

        // when
        swipe { swipeUp() }

        // then
        composeRule.onNodeWithText(elapsedTime).assertIsDisplayed()
    }

    @Test
    fun `파티원 장에서는 좌우로 쓸어도 넘어가지 않는다`() {
        // given
        show(party)
        swipe { swipeUp() }

        // when
        swipe { swipeLeft() }

        // then
        composeRule.onNodeWithText(firstPlayerName).assertIsDisplayed()

        // when
        swipe { swipeRight() }

        // then
        composeRule.onNodeWithText(firstPlayerName).assertIsDisplayed()
    }

    @Test
    fun `파티원 장에서 돌아오면 좌우로 다시 넘어간다`() {
        // given
        show(party)
        swipe { swipeUp() }
        swipe { swipeDown() }

        // when
        swipe { swipeLeft() }

        // then
        composeRule.onNodeWithText(text(R.string.unit_kcal)).assertIsDisplayed()
    }

    @Test
    fun `첫 화면에서는 뒤로가기를 가로채지 않는다`() {
        // when
        show(solo)
        composeRule.waitForIdle()

        // then -> 가로채지 않아야 시계 화면으로 나간다
        assertFalse(backDispatcher.hasEnabledCallbacks())
    }

    @Test
    fun `조작 장에서 뒤로가면 첫 화면으로 돌아온다`() {
        // given
        show(solo)
        swipe { swipeRight() }
        assertTrue(backDispatcher.hasEnabledCallbacks())

        // when
        pressBack()

        // then
        composeRule.onNodeWithText(elapsedTime).assertIsDisplayed()
        assertFalse(backDispatcher.hasEnabledCallbacks())
    }

    @Test
    fun `내 기록 장에서 뒤로가면 첫 화면으로 돌아온다`() {
        // given
        show(solo)
        swipe { swipeLeft() }

        // when
        pressBack()

        // then
        composeRule.onNodeWithText(elapsedTime).assertIsDisplayed()
    }

    @Test
    fun `파티원 장에서 뒤로가면 첫 화면으로 돌아온다`() {
        // given
        show(party)
        swipe { swipeUp() }

        // when
        pressBack()

        // then
        composeRule.onNodeWithText(elapsedTime).assertIsDisplayed()
    }

    private fun text(id: Int) = composeRule.activity.getString(id)

    private fun show(run: RunState) {
        composeRule.setContent {
            RunPreviewFrame {
                RunScreen(
                    run,
                    isPhoneReachable = true,
                    heartRateBpm = 150,
                    birthday = null,
                    RunCommandState(),
                    RunCommands(pause = {}, resume = {}, finish = {}),
                    isComboEffectOn = true,
                    onComboEffectChange = {},
                )
            }
        }
    }

    private fun swipe(gesture: TouchInjectionScope.() -> Unit) {
        composeRule.onRoot().performTouchInput(gesture)
        composeRule.waitForIdle()
    }

    private fun pressBack() {
        composeRule.runOnUiThread { backDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }
}
