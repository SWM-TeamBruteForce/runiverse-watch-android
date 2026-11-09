package com.runiverse.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.runiverse.app.ui.component.PreviewFrame
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
        composeRule.onNodeWithText(context.getString(R.string.run_phone_checking)).assertDoesNotExist()
    }

    @Test
    fun `폰 응답이 없으면 러닝 시작 안내 대신 폰 연결 확인 중을 보인다`() {
        // when
        composeRule.setContent { PreviewFrame { IdleScreen(isPhoneReachable = false) } }

        // then
        composeRule.onNodeWithText(context.getString(R.string.run_phone_checking)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.idle_start_on_phone)).assertDoesNotExist()
    }
}
