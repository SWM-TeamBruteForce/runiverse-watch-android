package com.runiverse.app.run

import android.app.Application
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.runiverse.app.R
import com.runiverse.app.phone.FinishProgress
import com.runiverse.app.phone.FinishStage
import com.runiverse.app.phone.RunState
import com.runiverse.app.phone.RunStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

// 눌러 끝내는 시간 1.2초를 넘긴다
private const val HOLD_PAST_FINISH_MS = 1_300L
private const val HOLD_SHORT_OF_FINISH_MS = 500L
private const val AFTER_RELEASE_MS = 100L

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w226dp-h226dp-round-watch-xhdpi")
class ControlsPageTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()
    private val comboChanges = mutableListOf<Boolean>()
    private val commands = RunCommands(
        pause = { calls += "pause" },
        resume = { calls += "resume" },
        finish = { forced -> calls += "finish forced=$forced" },
    )
    private val solo = RunPreviewSamples.solo
    private val party = RunPreviewSamples.party

    private fun text(id: Int) = RuntimeEnvironment.getApplication().getString(id)

    private fun show(
        run: RunState = solo,
        isPhoneReachable: Boolean = true,
        commandState: RunCommandState = RunCommandState(),
    ) {
        composeRule.setContent {
            RunPreviewFrame {
                ControlsPage(run, isPhoneReachable, commandState, commands, isComboEffectOn = true) {
                    comboChanges += it
                }
            }
        }
    }

    // 시계를 멈추지 않으면 누르자마자 시간이 흘러 끝까지 누른 것이 된다
    private fun press(textId: Int, heldMs: Long) {
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithText(text(textId)).performTouchInput { down(center) }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(heldMs)
        composeRule.onRoot().performTouchInput { up() }
        composeRule.mainClock.advanceTimeBy(AFTER_RELEASE_MS)
    }

    @Test
    fun `일시정지를 누르면 일시정지 명령을 부른다`() {
        // given
        show()

        // when
        composeRule.onNodeWithText(text(R.string.controls_pause)).performClick()

        // then
        assertEquals(listOf("pause"), calls)
    }

    @Test
    fun `멈춘 동안 계속 달리기를 누르면 재개 명령을 부른다`() {
        // given
        show(solo.copy(status = RunStatus.PAUSED))

        // when
        composeRule.onNodeWithText(text(R.string.controls_resume)).performClick()

        // then
        assertEquals(listOf("resume"), calls)
    }

    @Test
    fun `명령 답을 기다리는 동안 일시정지가 눌리지 않는다`() {
        // when
        show(commandState = RunCommandState(isLocked = true))

        // then
        composeRule.onNodeWithText(text(R.string.controls_pause)).assertIsNotEnabled()
    }

    @Test
    fun `폰이 끊기면 조작을 막고 안내를 보인다`() {
        // when
        show(isPhoneReachable = false)

        // then
        composeRule.onNodeWithText(text(R.string.controls_pause)).assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.run_phone_checking)).assertExists()
        composeRule.onNodeWithText(text(R.string.controls_after_reconnect)).assertExists()
    }

    @Test
    fun `달리는 중 종료를 끝까지 누르면 종료 명령을 부른다`() {
        // given
        show()

        // when
        press(R.string.controls_hold_to_finish, HOLD_PAST_FINISH_MS)

        // then
        assertEquals(listOf("finish forced=false"), calls)
    }

    @Test
    fun `달리는 중 종료를 짧게 누르면 끝내지 않고 길게 누르라고 안내한다`() {
        // given
        show()

        // when
        press(R.string.controls_hold_to_finish, HOLD_SHORT_OF_FINISH_MS)

        // then
        assertEquals(emptyList<String>(), calls)
        composeRule.onNodeWithText(text(R.string.controls_hold_longer)).assertExists()
    }

    @Test
    fun `종료를 누르는 동안 계속 누르기를 보인다`() {
        // given
        show()

        composeRule.mainClock.autoAdvance = false

        // when
        composeRule.onNodeWithText(text(R.string.controls_hold_to_finish)).performTouchInput { down(center) }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(HOLD_SHORT_OF_FINISH_MS)

        // then
        composeRule.onNodeWithText(text(R.string.controls_keep_pressing)).assertExists()
        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun `멈춘 동안 종료는 한 번 누르면 끝난다`() {
        // given
        show(solo.copy(status = RunStatus.PAUSED))

        // when
        composeRule.onNodeWithText(text(R.string.controls_finish)).performClick()

        // then
        assertEquals(listOf("finish forced=false"), calls)
    }

    @Test
    fun `목표에 못 미쳐 끝내면 강제 종료로 부른다`() {
        // given
        show(party.copy(status = RunStatus.PAUSED))

        // when
        composeRule.onNodeWithText(text(R.string.controls_finish)).performClick()

        // then
        assertEquals(listOf("finish forced=true"), calls)
    }

    @Test
    fun `종료 답을 기다리면 종료 중을 보인다`() {
        // when
        show(commandState = RunCommandState(isLocked = true, isFinishPending = true))

        // then
        composeRule.onNodeWithText(text(R.string.controls_finishing)).assertExists()
    }

    @Test
    fun `종료 답이 3초 넘게 없으면 종료 확인 중을 보인다`() {
        // when
        show(commandState = RunCommandState(isLocked = true, isFinishPending = true, isCommandOverdue = true))

        // then
        composeRule.onNodeWithText(text(R.string.controls_finish_checking)).assertExists()
    }

    @Test
    fun `폰이 서버 답을 기다리면 종료 중을 보이고 조작을 막는다`() {
        // when
        show(solo.copy(finish = FinishProgress(FinishStage.FINISHING)))

        // then
        composeRule.onNodeWithText(text(R.string.controls_finishing)).assertExists()
        composeRule.onNodeWithText(text(R.string.controls_pause)).assertIsNotEnabled()
    }

    @Test
    fun `벌칙 거리 전이면 남은 거리와 벌칙을 안내한다`() {
        // when
        show(party)

        // then
        composeRule.onNodeWithText(text(R.string.controls_penalty_if_short)).assertExists()
    }

    @Test
    fun `솔로는 목표가 없으면 안내를 띄우지 않는다`() {
        // when
        show()

        // then
        composeRule.onNodeWithText(text(R.string.controls_penalty_if_short)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.controls_run_recording)).assertDoesNotExist()
    }

    @Test
    fun `파티에서 서버가 끊기면 서버 안내와 기록 중을 보인다`() {
        // when
        show(party.copy(isServerConnected = false))

        // then
        composeRule.onNodeWithText(text(R.string.run_server_checking)).assertExists()
        composeRule.onNodeWithText(text(R.string.controls_run_recording)).assertExists()
    }

    @Test
    fun `스크롤해 콤보 효과를 누르면 설정을 바꾼다`() {
        // given
        show()
        composeRule.onRoot().performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        // when
        composeRule.onNodeWithText(text(R.string.controls_combo_effect)).performClick()

        // then
        assertEquals(listOf(false), comboChanges)
    }
}
