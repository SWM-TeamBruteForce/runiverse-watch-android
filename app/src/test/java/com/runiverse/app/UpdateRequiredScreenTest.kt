package com.runiverse.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.runiverse.app.ui.component.PreviewFrame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class UpdateRequiredScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun `제목·설명·업데이트 버튼을 보인다`() {
        // when
        composeRule.setContent { PreviewFrame { UpdateRequiredScreen(openStore = { true }) } }

        // then
        composeRule.onNodeWithText(text(R.string.update_required)).assertExists()
        composeRule.onNodeWithText(text(R.string.update_cannot_connect)).assertExists()
        composeRule.onNodeWithText(text(R.string.update_open_store)).assertExists()
    }

    @Test
    fun `스토어를 못 열면 설명을 스토어 열기 실패로 바꾼다`() {
        // given
        composeRule.setContent { PreviewFrame { UpdateRequiredScreen(openStore = { false }) } }

        // when
        composeRule.onNodeWithText(text(R.string.update_open_store)).performClick()

        // then
        composeRule.onNodeWithText(text(R.string.update_open_store_failed)).assertExists()
        composeRule.onNodeWithText(text(R.string.update_cannot_connect)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.update_open_store)).assertExists()
    }

    @Test
    fun `다시 눌러 스토어가 열리면 실패 문구를 지운다`() {
        // given
        var canOpenStore = false
        composeRule.setContent { PreviewFrame { UpdateRequiredScreen(openStore = { canOpenStore }) } }
        composeRule.onNodeWithText(text(R.string.update_open_store)).performClick()

        // when
        canOpenStore = true
        composeRule.onNodeWithText(text(R.string.update_open_store)).performClick()

        // then
        composeRule.onNodeWithText(text(R.string.update_cannot_connect)).assertExists()
        composeRule.onNodeWithText(text(R.string.update_open_store_failed)).assertDoesNotExist()
    }

    private fun text(id: Int) = context.getString(id)
}
