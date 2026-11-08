package com.runiverse.app.run

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.runiverse.app.R
import com.runiverse.app.phone.Player
import com.runiverse.app.phone.RunStatus
import com.runiverse.app.run.common.RunPreviewSamples
import com.runiverse.app.run.common.formatPace
import com.runiverse.app.run.common.formatRunDistanceKm
import com.runiverse.app.ui.component.PreviewFrame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PartyPageTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val run = RunPreviewSamples.party
    private val player = run.players.first()

    @Test
    fun `파티원 이름과 거리를 보인다`() {
        // when
        show()

        // then
        composeRule.onNodeWithText(player.nickname).assertExists()
        composeRule.onNodeWithText(formatRunDistanceKm(player.distanceMeters)).assertExists()
    }

    @Test
    fun `격차가 양수면 앞을 보인다`() {
        // when
        show(player.copy(gapMeters = 120))

        // then
        composeRule.onNodeWithText("120").assertExists()
        composeRule.onNodeWithText(text(R.string.run_gap_ahead)).assertExists()
    }

    @Test
    fun `격차가 음수면 뒤를 보인다`() {
        // when
        show(player.copy(gapMeters = -20))

        // then
        composeRule.onNodeWithText("20").assertExists()
        composeRule.onNodeWithText(text(R.string.run_gap_behind)).assertExists()
    }

    @Test
    fun `격차가 없으면 앞뒤를 보이지 않는다`() {
        // when
        show(player.copy(gapMeters = 0))

        // then
        composeRule.onNodeWithText("0").assertExists()
        composeRule.onNodeWithText(text(R.string.run_gap_ahead)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.run_gap_behind)).assertDoesNotExist()
    }

    @Test
    fun `통지가 아직 없으면 격차 자리에 모름 표시를 보인다`() {
        // when
        show(player.copy(gapMeters = null))

        // then
        composeRule.onNodeWithText(text(R.string.run_value_unknown)).assertExists()
    }

    @Test
    fun `멈춘 파티원은 격차 대신 멈춤을 보인다`() {
        // when
        show(player.copy(status = RunStatus.PAUSED, gapMeters = 120))

        // then
        composeRule.onNodeWithText(text(R.string.run_status_paused)).assertExists()
        composeRule.onNodeWithText("120").assertDoesNotExist()
    }

    @Test
    fun `끊긴 파티원은 끊김을 보인다`() {
        // when
        show(player.copy(status = RunStatus.DISCONNECTED))

        // then
        composeRule.onNodeWithText(text(R.string.run_status_disconnected)).assertExists()
    }

    @Test
    fun `완주한 파티원은 마침을 보인다`() {
        // when
        show(player.copy(status = RunStatus.FINISHED))

        // then
        composeRule.onNodeWithText(text(R.string.run_status_finished)).assertExists()
    }

    @Test
    fun `콤보 중이면 현재 콤보를 보인다`() {
        // when
        show(player.copy(comboCount = 12))

        // then
        composeRule.onNodeWithText(text(R.string.run_combo_current, 12)).assertExists()
    }

    @Test
    fun `콤보가 없으면 최고 콤보를 보인다`() {
        // when
        show(player.copy(comboCount = 0, maxComboCount = 20))

        // then
        composeRule.onNodeWithText(text(R.string.run_combo_max, 20)).assertExists()
    }

    @Test
    fun `상대와 내 페이스 표시를 보인다`() {
        // when
        show()

        // then
        composeRule.onNodeWithText(text(R.string.run_pace_player)).assertExists()
        composeRule.onNodeWithText(formatPace(player.currentPaceSecondsPerKm)).assertExists()
        composeRule.onNodeWithText(text(R.string.run_pace_me)).assertExists()
        composeRule.onNodeWithText(formatPace(run.me.currentPaceSecondsPerKm)).assertExists()
    }

    @Test
    fun `멈춘 파티원은 페이스를 모름으로 보인다`() {
        // when
        show(player.copy(status = RunStatus.PAUSED))

        // then
        composeRule.onNodeWithText(formatPace(null)).assertExists()
        composeRule.onNodeWithText(formatPace(player.currentPaceSecondsPerKm)).assertDoesNotExist()
    }

    @Test
    fun `폰이 끊기면 상대 페이스와 콤보 대신 폰 연결 확인 중을 보인다`() {
        // when
        show(player.copy(comboCount = 12), isPhoneReachable = false)

        // then
        composeRule.onNodeWithText(text(R.string.run_phone_checking)).assertExists()
        composeRule.onNodeWithText(text(R.string.run_pace_player)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.run_combo_current, 12)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.run_pace_me)).assertExists()
    }

    @Test
    fun `서버가 끊기면 서버 연결 확인 중을 보인다`() {
        // when
        show(isServerConnected = false)

        // then
        composeRule.onNodeWithText(text(R.string.run_server_checking)).assertExists()
    }

    private fun text(id: Int, vararg args: Any) = RuntimeEnvironment.getApplication().getString(id, *args)

    private fun show(shownPlayer: Player = player, isPhoneReachable: Boolean = true, isServerConnected: Boolean = true) {
        composeRule.setContent {
            PreviewFrame {
                PartyPage(run.copy(isServerConnected = isServerConnected), shownPlayer, isPhoneReachable)
            }
        }
    }
}
