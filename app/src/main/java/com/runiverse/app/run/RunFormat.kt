package com.runiverse.app.run

import com.runiverse.app.phone.Player
import com.runiverse.app.phone.RunState
import com.runiverse.app.phone.RunStatus
import kotlin.math.abs

/** 표기를 마친 거리다. 숫자와 단위를 따로 그린다 — 단위는 작고 흐리게 쓴다. */
data class FormattedDistance(val value: String, val unit: DistanceUnitLabel?)

/** `km`와 `k`는 다른 단위가 아니라 같은 단위의 다른 표기다. */
enum class DistanceUnitLabel { METER, KILOMETER, SHORT_KILOMETER }

enum class GapDirection { AHEAD, BEHIND, LEVEL }

const val UNKNOWN_PACE = "--'--\""

private const val METERS_PER_KM = 1000
private const val METERS_PER_TENTH_KM = 100
private const val EXTRA_DECIMAL_BELOW_METERS = 10_000
private const val SECONDS_PER_HOUR = 3600

/** 완주 전에 목표 숫자가 먼저 뜨지 않게 버린다. 10km부터는 위 줄에 들어가게 한 자리만 쓴다. */
fun formatRunDistanceKm(meters: Int): String {
    val km = meters / METERS_PER_KM
    val remainderMeters = meters % METERS_PER_KM
    return if (meters < EXTRA_DECIMAL_BELOW_METERS) {
        "$km.${(remainderMeters / 10).padTwoDigits()}"
    } else {
        "$km.${remainderMeters / METERS_PER_TENTH_KM}"
    }
}

/**
 * 파티원과의 격차다. 1000m 미만은 m, 10km 미만은 소수 한 자리 km, 그 위는 정수 km로 버린다.
 * [abbreviateUnits]면 m는 숫자만, km는 `k`로 줄이고, [dropKmDecimal]이면 km 소수도 뺀다.
 */
fun formatGapDistance(gapMeters: Int, abbreviateUnits: Boolean, dropKmDecimal: Boolean = false): FormattedDistance {
    val meters = abs(gapMeters)
    val kilometerLabel = if (abbreviateUnits) DistanceUnitLabel.SHORT_KILOMETER else DistanceUnitLabel.KILOMETER
    return when {
        meters < METERS_PER_KM -> FormattedDistance("$meters", if (abbreviateUnits) null else DistanceUnitLabel.METER)
        dropKmDecimal -> FormattedDistance("${meters / METERS_PER_KM}", kilometerLabel)
        meters < EXTRA_DECIMAL_BELOW_METERS -> {
            val tenthsOfKm = meters % METERS_PER_KM / METERS_PER_TENTH_KM
            FormattedDistance("${meters / METERS_PER_KM}.$tenthsOfKm", kilometerLabel)
        }

        else -> FormattedDistance("${meters / METERS_PER_KM}", kilometerLabel)
    }
}

/** 남은 거리를 적게 말하지 않게 폰처럼 올린다. */
fun formatRemainingDistance(meters: Int): FormattedDistance {
    if (meters < METERS_PER_KM) return FormattedDistance("$meters", DistanceUnitLabel.METER)
    val tenthsOfKm = (meters + METERS_PER_TENTH_KM - 1) / METERS_PER_TENTH_KM
    return FormattedDistance("${tenthsOfKm / 10}.${tenthsOfKm % 10}", DistanceUnitLabel.KILOMETER)
}

/** `gapMeters`는 상대 − 나라서 양수면 상대가 앞이다. */
fun gapDirection(gapMeters: Int): GapDirection = when {
    gapMeters > 0 -> GapDirection.AHEAD
    gapMeters < 0 -> GapDirection.BEHIND
    else -> GapDirection.LEVEL
}

/** 위치를 못 잡았거나 60분을 넘으면 낼 페이스가 없다고 본다. */
fun formatPace(secondsPerKm: Int?): String {
    if (secondsPerKm == null || secondsPerKm >= SECONDS_PER_HOUR) return UNKNOWN_PACE
    return "${secondsPerKm / 60}'${(secondsPerKm % 60).padTwoDigits()}\""
}

/** 1시간 전엔 폰처럼 `mm:ss`, 넘으면 `h:mm:ss`다. */
fun formatElapsedTime(seconds: Int): String {
    val minutes = seconds / 60 % 60
    val rest = (seconds % 60).padTwoDigits()
    if (seconds < SECONDS_PER_HOUR) return "${(seconds / 60).padTwoDigits()}:$rest"
    return "${seconds / SECONDS_PER_HOUR}:${minutes.padTwoDigits()}:$rest"
}

/** 목표까지 진행률이다. 목표가 없으면(솔로) null — 링을 그리지 않는다. */
fun distanceProgress(distanceMeters: Int, targetDistanceMeters: Int?): Float? =
    targetDistanceMeters?.takeIf { it > 0 }?.let { distanceMeters.toFloat() / it }

fun Player.hasStaleValues(run: RunState, isPhoneReachable: Boolean): Boolean =
    !isPhoneReachable || !run.isServerConnected || isStale

/** 멈췄거나 끊긴 사람은 지금 페이스가 없다 — 폰은 마지막 값을 그대로 보낸다. */
fun Player.shownPaceSecondsPerKm(): Int? = when (status) {
    RunStatus.PAUSED, RunStatus.DISCONNECTED -> null
    RunStatus.RUNNING, RunStatus.FINISHED -> currentPaceSecondsPerKm
}

/** 내가 버튼으로 멈춰도 상대처럼 지금 페이스가 없다. */
fun RunState.shownMyPaceSecondsPerKm(): Int? = if (status == RunStatus.PAUSED) null else me.currentPaceSecondsPerKm

private fun Int.padTwoDigits(): String = toString().padStart(2, '0')
