package com.runiverse.app.ui.theme

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RuniverseThemeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `시스템 글자가 1점3배를 넘으면 화면 밀도는 두고 글자만 1점3배로 그린다`() {
        // given
        var density: Density? = null

        // when
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f, fontScale = 2f)) {
                RuniverseTheme { density = LocalDensity.current }
            }
        }

        // then
        assertEquals(2f, density!!.density)
        assertEquals(1.3f, density!!.fontScale)
    }

    @Test
    fun `시스템 글자가 1점3배 이하면 시스템 밀도를 그대로 쓴다`() {
        // given
        val systemDensity = Density(2f, fontScale = 1.24f)
        var density: Density? = null

        // when
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides systemDensity) {
                RuniverseTheme { density = LocalDensity.current }
            }
        }

        // then
        assertSame(systemDensity, density)
    }
}
