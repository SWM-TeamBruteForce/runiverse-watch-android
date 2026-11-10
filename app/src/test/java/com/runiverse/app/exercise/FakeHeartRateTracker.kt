package com.runiverse.app.exercise

import kotlinx.coroutines.delay

internal class FakeHeartRateTracker : HeartRateTracker {
    val calls = mutableListOf<String>()
    var isMeasurable = true
    var owner: ExerciseOwner? = ExerciseOwner.NONE
    var isStartSuccessful = true
    var ownerDelayMs = 0L
    var startDelayMs = 0L
    var stopDelayMs = 0L
    var stopFailures = 0
    private var onHeartRate: ((Int?) -> Unit)? = null
    private var onMeasuringStopped: (() -> Unit)? = null

    override suspend fun canMeasure() = isMeasurable

    override suspend fun currentOwner(): ExerciseOwner? {
        delay(ownerDelayMs)
        return owner
    }

    override fun listen(onHeartRate: (Int?) -> Unit, onMeasuringStopped: () -> Unit) {
        calls += "listen"
        this.onHeartRate = onHeartRate
        this.onMeasuringStopped = onMeasuringStopped
    }

    override suspend fun startExercise(): Boolean {
        calls += "start"
        delay(startDelayMs)
        if (!isStartSuccessful) return false
        owner = ExerciseOwner.ME
        return true
    }

    override suspend fun stop(): Boolean {
        calls += "stop"
        delay(stopDelayMs)
        if (stopFailures > 0) {
            stopFailures--
            return false
        }
        owner = ExerciseOwner.NONE
        return true
    }

    fun deliver(heartRateBpm: Int?) {
        onHeartRate?.invoke(heartRateBpm)
    }

    fun endByOtherApp() {
        onMeasuringStopped?.invoke()
    }
}
