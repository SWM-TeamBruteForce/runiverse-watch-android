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
    fun `폰이 연결돼 있으면 앱 이름을 보인다`() {
        // when
        composeRule.setContent { PreviewFrame { IdleScreen(isPhoneReachable = true) } }

        // then
        composeRule.onNodeWithText(context.getString(R.string.app_name)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.run_phone_checking)).assertDoesNotExist()
    }

    @Test
    fun `폰 응답이 없으면 앱 이름 대신 폰 연결 확인 중을 보인다`() {
        // when
        composeRule.setContent { PreviewFrame { IdleScreen(isPhoneReachable = false) } }

        // then
        composeRule.onNodeWithText(context.getString(R.string.run_phone_checking)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.app_name)).assertDoesNotExist()
    }
}
