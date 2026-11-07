package com.runiverse.app.run

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.runiverse.app.R
import com.runiverse.app.phone.MyRecord
import com.runiverse.app.phone.RunState
import com.runiverse.app.phone.RunStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w226dp-h226dp-round-watch-xhdpi")
class MyRecordPageTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val run = RunPreviewSamples.solo

    private fun text(id: Int) = RuntimeEnvironment.getApplication().getString(id)

    private fun show(shownRun: RunState = run, isPhoneReachable: Boolean = true, heartRateBpm: Int? = 150) {
        composeRule.setContent {
            RunPreviewFrame { MyRecordPage(shownRun, isPhoneReachable, heartRateBpm, heartRateZone = 3) }
        }
    }

    @Test
    fun `심박·케이던스·칼로리를 보인다`() {
        // when
        show()

        // then
        composeRule.onNodeWithText("150").assertExists()
        composeRule.onNodeWithText(run.me.cadenceSpm.toString()).assertExists()
        composeRule.onNodeWithText(run.me.caloriesKcal.toString()).assertExists()
    }

    @Test
    fun `값이 없으면 각 자리에 모름 표시를 보인다`() {
        // given
        val noValues = run.copy(me = MyRecord(distanceMeters = 0, elapsedSeconds = 0))

        // when
        show(noValues, heartRateBpm = null)

        // then
        val unknownCount = composeRule.onAllNodesWithText(text(R.string.run_value_unknown)).fetchSemanticsNodes().size
        assertEquals(3, unknownCount)
    }

    @Test
    fun `멈추면 위에 멈춤을 보인다`() {
        // when
        show(run.copy(status = RunStatus.PAUSED))

        // then
        composeRule.onNodeWithText(text(R.string.run_status_paused)).assertExists()
    }

    @Test
    fun `멈춘 채 폰이 끊기면 끊김 안내가 먼저다`() {
        // when
        show(run.copy(status = RunStatus.PAUSED), isPhoneReachable = false)

        // then
        composeRule.onNodeWithText(text(R.string.run_phone_checking)).assertExists()
        composeRule.onNodeWithText(text(R.string.run_status_paused)).assertDoesNotExist()
    }

    @Test
    fun `폰이 끊겨도 워치가 재는 심박은 그대로 보인다`() {
        // when
        show(isPhoneReachable = false)

        // then
        composeRule.onNodeWithText("150").assertExists()
    }
}
