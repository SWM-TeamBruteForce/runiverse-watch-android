package com.runiverse.app.run

import org.junit.Assert.assertEquals
import org.junit.Test

class RunFormatTest {

    @Test
    fun `내 거리는 10km 전까지 소수 둘째 자리로 버린다`() {
        // when & then
        assertEquals("3.42", formatRunDistanceKm(3429))
        assertEquals("4.99", formatRunDistanceKm(4999))
        assertEquals("0.05", formatRunDistanceKm(55))
        assertEquals("9.99", formatRunDistanceKm(9999))
    }

    @Test
    fun `내 거리는 10km부터 소수 한 자리로 버린다`() {
        // when & then
        assertEquals("10.0", formatRunDistanceKm(10_000))
        assertEquals("42.1", formatRunDistanceKm(42_195))
    }

    @Test
    fun `격차는 1000m 미만이면 m로 쓴다`() {
        // when & then
        assertEquals(FormattedDistance("120", DistanceUnitLabel.METER), formatGapDistance(120, abbreviateUnits = false))
        assertEquals(FormattedDistance("20", DistanceUnitLabel.METER), formatGapDistance(-20, abbreviateUnits = false))
        assertEquals(FormattedDistance("0", DistanceUnitLabel.METER), formatGapDistance(0, abbreviateUnits = false))
        assertEquals(FormattedDistance("999", DistanceUnitLabel.METER), formatGapDistance(999, abbreviateUnits = false))
    }

    @Test
    fun `격차는 10km 미만이면 소수 한 자리 km, 그 위는 정수 km로 버린다`() {
        // when & then
        assertEquals(
            FormattedDistance("1.2", DistanceUnitLabel.KILOMETER),
            formatGapDistance(1299, abbreviateUnits = false),
        )
        assertEquals(
            FormattedDistance("9.9", DistanceUnitLabel.KILOMETER),
            formatGapDistance(-9999, abbreviateUnits = false),
        )
        assertEquals(
            FormattedDistance("42", DistanceUnitLabel.KILOMETER),
            formatGapDistance(42_195, abbreviateUnits = false),
        )
    }

    @Test
    fun `글자를 키우면 격차의 m는 숫자만, km는 k로 줄인다`() {
        // when & then
        assertEquals(FormattedDistance("999", null), formatGapDistance(999, abbreviateUnits = true))
        assertEquals(
            FormattedDistance("1.2", DistanceUnitLabel.SHORT_KILOMETER),
            formatGapDistance(1200, abbreviateUnits = true),
        )
        assertEquals(
            FormattedDistance("42", DistanceUnitLabel.SHORT_KILOMETER),
            formatGapDistance(42_000, abbreviateUnits = true),
        )
    }

    @Test
    fun `격차가 양수면 상대가 앞, 음수면 뒤, 0이면 나란히다`() {
        // when & then
        assertEquals(GapDirection.AHEAD, gapDirection(120))
        assertEquals(GapDirection.BEHIND, gapDirection(-20))
        assertEquals(GapDirection.LEVEL, gapDirection(0))
    }

    @Test
    fun `페이스는 분과 초로 쓰고, 없거나 60분을 넘으면 비운다`() {
        // when & then
        assertEquals("5'32\"", formatPace(332))
        assertEquals("10'05\"", formatPace(605))
        assertEquals("59'59\"", formatPace(3599))
        assertEquals(UNKNOWN_PACE, formatPace(3600))
        assertEquals(UNKNOWN_PACE, formatPace(null))
    }

    @Test
    fun `시간은 1시간 전엔 mm-ss, 넘으면 h-mm-ss다`() {
        // when & then
        assertEquals("00:05", formatElapsedTime(5))
        assertEquals("24:10", formatElapsedTime(1450))
        assertEquals("59:59", formatElapsedTime(3599))
        assertEquals("1:00:00", formatElapsedTime(3600))
        assertEquals("4:59:59", formatElapsedTime(17_999))
    }
}
