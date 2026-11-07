package com.runiverse.app.run

import com.runiverse.app.phone.MyRecord
import com.runiverse.app.phone.Player
import com.runiverse.app.phone.RoomType
import com.runiverse.app.phone.RunResult
import com.runiverse.app.phone.RunState
import com.runiverse.app.phone.RunStatus

internal object RunPreviewSamples {
    private val me = MyRecord(
        distanceMeters = 3421,
        elapsedSeconds = 1450,
        currentPaceSecondsPerKm = 332,
        caloriesKcal = 210,
        cadenceSpm = 172,
    )

    private val players = listOf(
        player("u1", "완두콩", 1, gapMeters = 120, status = RunStatus.RUNNING),
        player("u2", "콩나물", 2, gapMeters = -20, status = RunStatus.RUNNING, comboCount = 12),
        player("u3", "강낭콩", 3, gapMeters = -600, status = RunStatus.PAUSED),
    )

    val party = RunState(
        runningRoomId = 812,
        type = RoomType.MATCH,
        status = RunStatus.RUNNING,
        targetDistanceMeters = 5000,
        isServerConnected = true,
        isPenaltyApplicable = true,
        me = me,
        players = players,
    )

    val solo = party.copy(
        type = RoomType.SOLO,
        targetDistanceMeters = null,
        isPenaltyApplicable = false,
        players = emptyList(),
    )

    val longValues = party.copy(
        targetDistanceMeters = 42_195,
        me = me.copy(distanceMeters = 42_100, elapsedSeconds = 17_999, currentPaceSecondsPerKm = 605),
        players = players.map { it.copy(gapMeters = 9_999) },
    )

    val result = RunResult(
        isServerConfirmed = true,
        totalDistanceMeters = 5_020,
        averagePaceSecondsPerKm = 343,
        elapsedSeconds = 1_721,
    )

    val longResult = RunResult(
        isServerConfirmed = true,
        totalDistanceMeters = 42_100,
        averagePaceSecondsPerKm = 426,
        elapsedSeconds = 17_999,
    )

    val finishedPlayer = player("u1", "콩나물", 2, gapMeters = 1_579, status = RunStatus.FINISHED)
        .copy(distanceMeters = 5000)

    val notNotifiedPlayer = player("u1", "함께 달리는 사람", 1, gapMeters = null, status = RunStatus.RUNNING)
        .copy(distanceMeters = 0, currentPaceSecondsPerKm = null, maxComboCount = 0)

    val longestPlayer = player("u1", "가나다라마바사아자차카", 1, gapMeters = 42_195, status = RunStatus.RUNNING)
        .copy(distanceMeters = 42_100, currentPaceSecondsPerKm = 605, maxComboCount = 888)

    private fun player(
        userId: String,
        nickname: String,
        colorSlot: Int,
        gapMeters: Int?,
        status: RunStatus,
        comboCount: Int = 0,
    ) = Player(
        userId = userId,
        nickname = nickname,
        colorSlot = colorSlot,
        distanceMeters = 3421 + (gapMeters ?: 0),
        currentPaceSecondsPerKm = 318,
        status = status,
        gapMeters = gapMeters,
        comboCount = comboCount,
        maxComboCount = 20,
        isStale = false,
    )
}
