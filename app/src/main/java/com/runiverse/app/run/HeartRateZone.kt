package com.runiverse.app.run

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException

// 경계는 화면에 안 보여 반올림하지 않고 그대로 비교한다
private val ZONE_LOWER_BOUNDS = listOf(0.5, 0.6, 0.7, 0.8, 0.9)

/** 심박 구간 막대에 채울 칸 수(0~5)다. 최대 심박은 220 − 만 나이로 잡는다. */
fun calculateHeartRateZone(bpm: Int, birthday: LocalDate, today: LocalDate): Int {
    val maxBpm = 220 - Period.between(birthday, today).years
    return ZONE_LOWER_BOUNDS.count { bpm >= maxBpm * it }
}

/** 폰이 보낸 생일(`yyyy-MM-dd`)이다. 없거나 읽을 수 없으면 null — 구간 막대를 숨긴다. */
fun parseBirthday(value: String?): LocalDate? {
    if (value == null) return null
    return try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }
}
