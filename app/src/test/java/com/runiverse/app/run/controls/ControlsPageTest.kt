package com.runiverse.app.run.controls

import androidx.compose.ui.geometry.Offset
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
import com.runiverse.app.run.common.RunCommandState
import com.runiverse.app.run.common.RunCommands
import com.runiverse.app.run.common.RunPreviewSamples
import com.runiverse.app.ui.component.PreviewFrame
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

private const val HOLD_PAST_FINISH_MS = 1_300L
private const val HOLD_SHORT_OF_FINISH_MS = 500L
private const val AFTER_RELEASE_MS = 100L
private const val HOLD_LONGER_NOTICE_PASSED_MS = 1_600L
private const val SCROLL_AWAY_PX = 200f

@RunWith(RobolectricTestRunner::class)
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
        composeRule.onNodeWithText(text(R.string.controls_hold_longer)).assertExists()

        // when
        composeRule.mainClock.advanceTimeBy(HOLD_PAST_FINISH_MS)

        // then
        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun `길게 누르라는 안내는 잠시 뒤 사라진다`() {
        // given
        show()
        press(R.string.controls_hold_to_finish, HOLD_SHORT_OF_FINISH_MS)

        // when
        composeRule.mainClock.advanceTimeBy(HOLD_LONGER_NOTICE_PASSED_MS)

        // then
        composeRule.onNodeWithText(text(R.string.controls_hold_to_finish)).assertExists()
    }

    @Test
    fun `종료를 누른 채 쓸어 넘기면 끝내지 않고 안내도 띄우지 않는다`() {
        // given
        show()
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithText(text(R.string.controls_hold_to_finish)).performTouchInput { down(center) }
        composeRule.waitForIdle()

        // when
        composeRule.onRoot().performTouchInput {
            moveBy(Offset(0f, -SCROLL_AWAY_PX))
            up()
        }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(HOLD_PAST_FINISH_MS)

        // then
        assertEquals(emptyList<String>(), calls)
        composeRule.onNodeWithText(text(R.string.controls_hold_longer)).assertDoesNotExist()
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
        composeRule.onNodeWithText("579", substring = true).assertExists()
        composeRule.onNodeWithText(text(R.string.controls_penalty_if_short)).assertExists()
    }

    @Test
    fun `멈춘 동안 벌칙 거리 전이면 끝내면 벌칙이라고 안내한다`() {
        // when
        show(party.copy(status = RunStatus.PAUSED))

        // then
        composeRule.onNodeWithText(text(R.string.controls_penalty_if_finished)).assertExists()
    }

    @Test
    fun `벌칙 거리를 넘기면 목표까지 남은 거리를 안내한다`() {
        // when
        show(party.copy(me = party.me.copy(distanceMeters = 4_500)))

        // then
        composeRule.onNodeWithText("500", substring = true).assertExists()
    }

    @Test
    fun `멈춘 동안 목표 전이면 끝내면 미완주일 수 있다고 안내한다`() {
        // when
        show(party.copy(status = RunStatus.PAUSED, me = party.me.copy(distanceMeters = 4_500)))

        // then
        composeRule.onNodeWithText(text(R.string.controls_may_not_complete)).assertExists()
    }

    @Test
    fun `목표 거리를 넘기면 거리 확인 중과 계속 달리라는 안내를 보인다`() {
        // when
        show(party.copy(me = party.me.copy(distanceMeters = 5_000)))

        // then
        composeRule.onNodeWithText(text(R.string.controls_distance_checking)).assertExists()
        composeRule.onNodeWithText(text(R.string.controls_keep_running_if_short)).assertExists()
    }

    @Test
    fun `종료 보류면 서버가 알려 준 남은 거리를 안내한다`() {
        // when
        show(party.copy(finish = FinishProgress(FinishStage.GOAL_PENDING, remainingMeters = 420)))

        // then
        composeRule.onNodeWithText("420", substring = true).assertExists()
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

    private fun text(id: Int) = RuntimeEnvironment.getApplication().getString(id)

    private fun show(
        run: RunState = solo,
        isPhoneReachable: Boolean = true,
        commandState: RunCommandState = RunCommandState(),
    ) {
        composeRule.setContent {
            PreviewFrame {
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
}
