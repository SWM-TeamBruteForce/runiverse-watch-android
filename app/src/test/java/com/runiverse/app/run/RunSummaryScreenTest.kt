package com.runiverse.app.run

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.runiverse.app.R
import com.runiverse.app.phone.RunResult
import com.runiverse.app.phone.RunStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w226dp-h226dp-round-watch-xhdpi")
class RunSummaryScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val result = RunPreviewSamples.result
    private var closeCount = 0

    private fun text(id: Int) = composeRule.activity.getString(id)

    private fun show(shownResult: RunResult? = result, isPhoneReachable: Boolean = true) {
        val run = RunPreviewSamples.solo.copy(status = RunStatus.FINISHED, result = shownResult)
        composeRule.setContent {
            RunPreviewFrame { RunSummaryScreen(run, isPhoneReachable, onClose = { closeCount++ }) }
        }
    }

    @Test
    fun `받은 결과는 폰이 끊겨도 멈춘 것으로 보지 않는다`() {
        // when & then
        assertFalse(isResultStuck(RunPreviewSamples.result, isPhoneReachable = false))
    }

    @Test
    fun `결과를 받는 중 폰이 끊기면 멈춘 것으로 본다`() {
        // when & then
        assertTrue(isResultStuck(result = null, isPhoneReachable = false))
    }

    @Test
    fun `결과를 받는 중이라도 폰이 연결돼 있으면 멈춘 것으로 보지 않는다`() {
        // when & then
        assertFalse(isResultStuck(result = null, isPhoneReachable = true))
    }

    @Test
    fun `총거리·시간·평균 페이스를 보인다`() {
        // when
        show()

        // then
        composeRule.onNodeWithText(text(R.string.run_summary_completed)).assertExists()
        composeRule.onNodeWithText(formatRunDistanceKm(result.totalDistanceMeters)).assertExists()
        composeRule.onNodeWithText(formatElapsedTime(result.elapsedSeconds)).assertExists()
        composeRule.onNodeWithText(formatPace(result.averagePaceSecondsPerKm)).assertExists()
    }

    @Test
    fun `결과를 받는 동안은 값 자리에 모름 표시를 보인다`() {
        // when
        show(shownResult = null)

        // then
        val unknownCount = composeRule.onAllNodesWithText(text(R.string.run_value_unknown)).fetchSemanticsNodes().size
        assertEquals(3, unknownCount)
    }

    @Test
    fun `결과를 받는 중 폰이 끊기면 폰 연결 확인 중을 보인다`() {
        // when
        show(shownResult = null, isPhoneReachable = false)

        // then
        composeRule.onNodeWithText(text(R.string.run_phone_checking)).assertExists()
        composeRule.onNodeWithText(text(R.string.run_summary_completed)).assertDoesNotExist()
    }

    @Test
    fun `받은 결과가 있으면 폰이 끊겨도 러닝 완료를 보인다`() {
        // when
        show(isPhoneReachable = false)

        // then
        composeRule.onNodeWithText(text(R.string.run_summary_completed)).assertExists()
        composeRule.onNodeWithText(text(R.string.run_phone_checking)).assertDoesNotExist()
    }

    @Test
    fun `닫기를 누르면 닫는다`() {
        // given
        show()

        // when
        composeRule.onNodeWithText(text(R.string.run_summary_close)).performClick()

        // then
        assertEquals(1, closeCount)
    }

    @Test
    fun `결과를 받는 중에도 닫기를 누르면 닫는다`() {
        // given
        show(shownResult = null)

        // when
        composeRule.onNodeWithText(text(R.string.run_summary_close)).performClick()

        // then
        assertEquals(1, closeCount)
    }

    @Test
    fun `뒤로가기를 누르면 닫는다`() {
        // given
        show()

        // when
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        // then
        assertEquals(1, closeCount)
    }
}
