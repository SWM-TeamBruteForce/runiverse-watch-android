package com.runiverse.app.run

import android.app.Application
import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ComboEffectSettingTest {

    private val preferences
        get() = RuntimeEnvironment.getApplication().getSharedPreferences("test", Context.MODE_PRIVATE)

    @Test
    fun `저장한 적 없으면 켜져 있다`() {
        // when & then
        assertTrue(ComboEffectSetting(preferences).isOn.value)
    }

    @Test
    fun `끄면 바로 꺼진다`() {
        // given
        val setting = ComboEffectSetting(preferences)

        // when
        setting.set(false)

        // then
        assertFalse(setting.isOn.value)
    }

    @Test
    fun `앱을 다시 열어도 끈 설정이 남는다`() {
        // given
        ComboEffectSetting(preferences).set(false)

        // when & then
        assertFalse(ComboEffectSetting(preferences).isOn.value)
    }
}
