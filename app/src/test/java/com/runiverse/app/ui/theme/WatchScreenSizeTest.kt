package com.runiverse.app.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchScreenSizeTest {

    @Test
    fun `215dp 미만이면 작은 화면이다`() {
        // when & then -> 384px·2.0과 갤럭시 40mm 438px·2.125 화면이다
        assertTrue(isSmallScreenWidth(192.dp))
        assertTrue(isSmallScreenWidth(206.1.dp))
    }

    @Test
    fun `215dp부터는 큰 화면이다 — 밀도가 낮은 454px 기기도 dp로 보면 크다`() {
        // when & then -> 경계와 갤럭시 44mm 480px·2.125, 454px·2.0 화면이다
        assertFalse(isSmallScreenWidth(215.dp))
        assertFalse(isSmallScreenWidth(225.9.dp))
        assertFalse(isSmallScreenWidth(227.dp))
    }
}
