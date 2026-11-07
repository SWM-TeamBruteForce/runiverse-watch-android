package com.runiverse.app.run

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.runiverse.app.R
import com.runiverse.app.phone.RunState
import com.runiverse.app.phone.RunStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RunOverviewPageTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val solo = RunPreviewSamples.solo
    private val party = RunPreviewSamples.party

    @Test
    fun `내 거리·시간·페이스와 심박을 보인다`() {
        // when
        show(solo)

        // then
        composeRule.onNodeWithText(formatRunDistanceKm(solo.me.distanceMeters)).assertExists()
        composeRule.onNodeWithText(formatElapsedTime(solo.me.elapsedSeconds)).assertExists()
        composeRule.onNodeWithText(formatPace(solo.me.currentPaceSecondsPerKm)).assertExists()
        composeRule.onNodeWithText("150").assertExists()
    }

    @Test
    fun `심박이 없으면 값 자리에 모름 표시를 보인다`() {
        // when
        show(solo, heartRateBpm = null)

        // then
        composeRule.onNodeWithText(text(R.string.run_value_unknown)).assertExists()
    }

    @Test
    fun `멈추면 페이스 대신 멈춤을 보인다`() {
        // when
        show(solo.copy(status = RunStatus.PAUSED))

        // then
        composeRule.onNodeWithText(text(R.string.run_status_paused)).assertExists()
        composeRule.onNodeWithText(formatPace(solo.me.currentPaceSecondsPerKm)).assertDoesNotExist()
    }

    @Test
    fun `파티면 심박 대신 파티원 첫 글자와 격차를 보인다`() {
        // when
        show(party)

        // then
        composeRule.onNodeWithText("완").assertExists()
        composeRule.onNodeWithText("120").assertExists()
        composeRule.onNodeWithText("150").assertDoesNotExist()
    }

    @Test
    fun `멈춘 파티원은 격차 대신 멈춤을 보인다`() {
        // when
        show(party)

        // then
        composeRule.onNodeWithText("600").assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.run_status_paused)).assertExists()
    }

    @Test
    fun `통지가 아직 없는 파티원은 격차 자리를 비운다`() {
        // given
        val notNotified = party.copy(players = listOf(RunPreviewSamples.notNotifiedPlayer))

        // when
        show(notNotified)

        // then
        assertEquals(0, composeRule.onAllNodesWithText(text(R.string.unit_m)).fetchSemanticsNodes().size)
    }

    @Test
    fun `폰이 끊기면 격차를 숨기고 폰 연결 확인 중을 보인다`() {
        // when
        show(party, isPhoneReachable = false)

        // then
        composeRule.onNodeWithText(text(R.string.run_phone_checking)).assertExists()
        composeRule.onNodeWithText("120").assertDoesNotExist()
    }

    @Test
    fun `솔로에서 폰이 끊기면 폰 연결 확인 중을 보이고 심박은 그대로 보인다`() {
        // when
        show(solo, isPhoneReachable = false)

        // then
        composeRule.onNodeWithText(text(R.string.run_phone_checking)).assertExists()
        composeRule.onNodeWithText("150").assertExists()
    }

    @Test
    fun `파티에서 서버가 끊기면 서버 연결 확인 중을 보인다`() {
        // when
        show(party.copy(isServerConnected = false))

        // then
        composeRule.onNodeWithText(text(R.string.run_server_checking)).assertExists()
    }

    @Test
    fun `솔로는 서버가 끊겨도 안내를 띄우지 않는다`() {
        // when
        show(solo.copy(isServerConnected = false))

        // then
        composeRule.onNodeWithText(text(R.string.run_server_checking)).assertDoesNotExist()
    }

    @Test
    fun `폰과 서버가 함께 끊기면 폰 안내가 먼저다`() {
        // when
        show(party.copy(isServerConnected = false), isPhoneReachable = false)

        // then
        composeRule.onNodeWithText(text(R.string.run_phone_checking)).assertExists()
        composeRule.onNodeWithText(text(R.string.run_server_checking)).assertDoesNotExist()
    }

    private fun text(id: Int) = RuntimeEnvironment.getApplication().getString(id)

    private fun show(run: RunState, isPhoneReachable: Boolean = true, heartRateBpm: Int? = 150) {
        composeRule.setContent {
            RunPreviewFrame { RunOverviewPage(run, isPhoneReachable, heartRateBpm, heartRateZone = 3) }
        }
    }
}
