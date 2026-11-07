package com.runiverse.app.run

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunSummaryScreenTest {

    @Test
    fun `받은 결과는 폰이 끊겨도 멈춘 것으로 보지 않는다`() {
        // when & then
        assertFalse(isResultStuck(RunPreviewSamples.result, isPhoneReachable = false))
    }

    @Test
    fun `결과를 받는 중 폰이 끊기면 멈춘 것으로 본다`() {
        // when & then
        assertTrue(isResultStuck(result = null, isPhoneReachable = false))
    }

    @Test
    fun `결과를 받는 중이라도 폰이 연결돼 있으면 멈춘 것으로 보지 않는다`() {
        // when & then
        assertFalse(isResultStuck(result = null, isPhoneReachable = true))
    }
}
