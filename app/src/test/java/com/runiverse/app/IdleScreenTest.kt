package com.runiverse.app

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.runiverse.app.run.RunPreviewFrame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w226dp-h226dp-round-watch-xhdpi")
class IdleScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun `폰이 연결돼 있으면 앱 이름을 보인다`() {
        // when
        composeRule.setContent { RunPreviewFrame { IdleScreen(isPhoneReachable = true) } }

        // then
        composeRule.onNodeWithText(context.getString(R.string.app_name)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.run_phone_checking)).assertDoesNotExist()
    }

    @Test
    fun `폰 응답이 없으면 앱 이름 대신 폰 연결 확인 중을 보인다`() {
        // when
        composeRule.setContent { RunPreviewFrame { IdleScreen(isPhoneReachable = false) } }

        // then
        composeRule.onNodeWithText(context.getString(R.string.run_phone_checking)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.app_name)).assertDoesNotExist()
    }
}
