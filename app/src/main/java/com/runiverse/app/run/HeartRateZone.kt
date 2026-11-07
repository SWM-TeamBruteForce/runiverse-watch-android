package com.runiverse.app.run

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException

// 경계는 화면에 안 보여 반올림하지 않고 그대로 비교한다
private val ZONE_LOWER_BOUND_FRACTIONS = listOf(0.5, 0.6, 0.7, 0.8, 0.9)
private const val MAX_HEART_RATE_BASE_BPM = 220

/** 심박 구간 막대에 채울 칸 수(0~5)다. 최대 심박은 220 − 만 나이로 잡는다. */
fun calculateHeartRateZone(heartRateBpm: Int, birthday: LocalDate, today: LocalDate): Int {
    val maxBpm = MAX_HEART_RATE_BASE_BPM - Period.between(birthday, today).years
    return ZONE_LOWER_BOUND_FRACTIONS.count { heartRateBpm >= maxBpm * it }
}

/** 생일이 없으면 null(막대 숨김), 심박이 아직 없으면 0칸(빈 막대)이다. */
fun heartRateZoneOrNull(heartRateBpm: Int?, birthday: LocalDate?, today: LocalDate): Int? {
    if (birthday == null) return null
    return heartRateBpm?.let { calculateHeartRateZone(it, birthday, today) } ?: 0
}

/** 폰이 보낸 생일(`yyyy-MM-dd`)이다. 없거나 못 읽으면 null이다. */
fun parseBirthday(value: String?): LocalDate? {
    if (value == null) return null
    return try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }
}
