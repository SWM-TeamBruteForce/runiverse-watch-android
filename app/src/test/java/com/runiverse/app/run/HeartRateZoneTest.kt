package com.runiverse.app.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class HeartRateZoneTest {

    // 만 30세 — 최대 심박 190, 경계 95·114·133·152·171
    private val birthday = LocalDate.of(1995, 3, 10)
    private val today = LocalDate.of(2025, 3, 10)

    @Test
    fun `최대 심박의 50% 미만이면 0칸이다`() {
        // when & then
        assertEquals(0, calculateHeartRateZone(94, birthday, today))
    }

    @Test
    fun `경계와 같으면 그 구간으로 센다`() {
        // when & then
        assertEquals(1, calculateHeartRateZone(95, birthday, today))
        assertEquals(3, calculateHeartRateZone(133, birthday, today))
        assertEquals(5, calculateHeartRateZone(171, birthday, today))
    }

    @Test
    fun `경계는 반올림하지 않는다`() {
        // given -> 만 29세, 최대 심박 191이라 60% 경계는 114.6
        val beforeBirthday = LocalDate.of(2025, 3, 9)

        // when & then
        assertEquals(1, calculateHeartRateZone(114, birthday, beforeBirthday))
        assertEquals(2, calculateHeartRateZone(115, birthday, beforeBirthday))
    }

    @Test
    fun `생일은 yyyy-MM-dd로 읽고, 없거나 깨졌으면 null이다`() {
        // when & then
        assertEquals(LocalDate.of(1995, 3, 10), parseBirthday("1995-03-10"))
        assertNull(parseBirthday(null))
        assertNull(parseBirthday("1995/03/10"))
    }
}
