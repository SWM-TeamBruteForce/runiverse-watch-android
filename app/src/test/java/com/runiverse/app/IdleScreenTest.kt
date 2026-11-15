package com.runiverse.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.runiverse.app.ui.component.PreviewFrame
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class IdleScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun `폰이 연결돼 있으면 폰에서 러닝을 시작하라고 보인다`() {
        // when
        composeRule.setContent { PreviewFrame { IdleScreen(isPhoneReachable = true) } }

        // then
        composeRule.onNodeWithText(context.getString(R.string.idle_start_on_phone)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.idle_run_screen_shown_automatically)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.run_phone_checking)).assertDoesNotExist()
    }

    @Test
    fun `폰 응답이 없으면 러닝 시작 안내 대신 폰 연결 확인 중을 보인다`() {
        // when
        composeRule.setContent { PreviewFrame { IdleScreen(isPhoneReachable = false) } }

        // then
        composeRule.onNodeWithText(context.getString(R.string.run_phone_checking)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.idle_start_on_phone)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.idle_run_screen_shown_automatically)).assertDoesNotExist()
    }

    @Test
    fun `폰 답을 기다리는 동안은 문구 없이 그림만 보인다`() {
        // when
        composeRule.setContent { PreviewFrame { IdleScreen(isPhoneReachable = null) } }

        // then
        composeRule.onNodeWithContentDescription(context.getString(R.string.app_name)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.idle_start_on_phone)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.run_phone_checking)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.idle_run_screen_shown_automatically)).assertDoesNotExist()
    }

    @Test
    fun `화면 읽기는 아이콘을 앱 이름으로 읽는다`() {
        // when
        composeRule.setContent { PreviewFrame { IdleScreen(isPhoneReachable = true) } }

        // then
        composeRule.onNodeWithContentDescription(context.getString(R.string.app_name)).assertExists()
    }

    @Test
    fun `폰과 연결돼도 첫 줄은 폰 연결 확인 중과 같은 높이에 있다`() {
        // given
        var isPhoneReachable by mutableStateOf(false)
        composeRule.setContent { PreviewFrame { IdleScreen(isPhoneReachable) } }
        val checkingTop = topOf(R.string.run_phone_checking)

        // when
        isPhoneReachable = true
        composeRule.waitForIdle()

        // then
        assertEquals(checkingTop, topOf(R.string.idle_start_on_phone))
    }

    @Test
    fun `문구가 없어도 그림은 같은 높이에 있다`() {
        // given
        var isPhoneReachable by mutableStateOf<Boolean?>(null)
        composeRule.setContent { PreviewFrame { IdleScreen(isPhoneReachable) } }
        val awaitingMarkTop = markTop()

        // when
        isPhoneReachable = true
        composeRule.waitForIdle()

        // then
        assertEquals(awaitingMarkTop, markTop())
    }

    @Test
    fun `업데이트 필요 화면으로 바뀌어도 두 줄이 같은 높이에 있다`() {
        // given
        var isUpdateRequired by mutableStateOf(false)
        composeRule.setContent {
            PreviewFrame {
                if (isUpdateRequired) {
                    UpdateRequiredScreen(openStore = { true })
                } else {
                    IdleScreen(isPhoneReachable = true)
                }
            }
        }
        val firstLineTop = topOf(R.string.idle_start_on_phone)
        val secondLineTop = topOf(R.string.idle_run_screen_shown_automatically)

        // when
        isUpdateRequired = true
        composeRule.waitForIdle()

        // then
        assertEquals(firstLineTop, topOf(R.string.update_required))
        assertEquals(secondLineTop, topOf(R.string.update_cannot_connect))
    }

    private fun topOf(id: Int): Float =
        composeRule.onNodeWithText(context.getString(id)).fetchSemanticsNode().boundsInRoot.top

    private fun markTop(): Float =
        composeRule.onNodeWithContentDescription(context.getString(R.string.app_name))
            .fetchSemanticsNode().boundsInRoot.top
}
