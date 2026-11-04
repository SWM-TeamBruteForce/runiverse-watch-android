package com.runiverse.app.exercise

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.health.connect.HealthPermissions
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.HealthServicesException
import androidx.health.services.client.clearUpdateCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseTrackedStatus
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.endExercise
import androidx.health.services.client.getCapabilities
import androidx.health.services.client.getCurrentExerciseInfo
import androidx.health.services.client.startExercise
import kotlin.math.roundToInt

enum class ExerciseOwner { NONE, ME, OTHER_APP }

/** 기기의 심박 측정 기능이다. 실패는 로그만 남기고 호출한 쪽에는 측정 못 함으로 알린다. */
interface HeartRateTracker {
    suspend fun canMeasure(): Boolean

    /** 알 수 없으면 null이다. */
    suspend fun currentOwner(): ExerciseOwner?

    /** [onHeartRate]는 측정할 수 없게 되면 null을 받는다. [onMeasuringStopped]는 운동이 끝났거나 듣기에 실패했을 때 불린다. */
    fun listen(onHeartRate: (Int?) -> Unit, onMeasuringStopped: () -> Unit)

    suspend fun startExercise(): Boolean

    /** 듣기를 멈추고, 내 운동이 남아 있으면 끝낸다. */
    suspend fun stop()
}

/** 심박 권한 이름은 API 36에서 바뀌었다. */
val HEART_RATE_PERMISSION: String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        HealthPermissions.READ_HEART_RATE
    } else {
        Manifest.permission.BODY_SENSORS
    }

class HealthServicesHeartRateTracker(private val context: Context) : HeartRateTracker {

    private val exerciseClient = HealthServices.getClient(context).exerciseClient
    private var callback: ExerciseUpdateCallback? = null

    override suspend fun canMeasure(): Boolean {
        val permission = ContextCompat.checkSelfPermission(context, HEART_RATE_PERMISSION)
        if (permission != PackageManager.PERMISSION_GRANTED) return false
        return healthCall {
            val capabilities = exerciseClient.getCapabilities()
            ExerciseType.RUNNING in capabilities.supportedExerciseTypes &&
                DataType.HEART_RATE_BPM in
                capabilities.getExerciseTypeCapabilities(ExerciseType.RUNNING).supportedDataTypes
        } ?: false
    }

    // 공식 안내가 가르라고 하는 상수인데 라이브러리 안쪽 표시가 붙어 lint가 막는다
    @SuppressLint("RestrictedApi", "WrongConstant")
    override suspend fun currentOwner(): ExerciseOwner? = healthCall {
        when (exerciseClient.getCurrentExerciseInfo().exerciseTrackedStatus) {
            ExerciseTrackedStatus.NO_EXERCISE_IN_PROGRESS -> ExerciseOwner.NONE
            ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS -> ExerciseOwner.ME
            ExerciseTrackedStatus.OTHER_APP_IN_PROGRESS -> ExerciseOwner.OTHER_APP
            else -> null
        }
    }

    override fun listen(onHeartRate: (Int?) -> Unit, onMeasuringStopped: () -> Unit) {
        val registered = object : ExerciseUpdateCallback {
            override fun onRegistered() = Unit

            override fun onRegistrationFailed(throwable: Throwable) {
                Log.w(TAG, "운동 콜백 등록 실패", throwable)
                onMeasuringStopped()
            }

            override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
                if (update.exerciseStateInfo.state.isEnded) {
                    onMeasuringStopped()
                    return
                }
                val latest = update.latestMetrics.getData(DataType.HEART_RATE_BPM).lastOrNull() ?: return
                onHeartRate(latest.value.roundToInt())
            }

            override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit

            override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {
                if (dataType == DataType.HEART_RATE_BPM && availability != DataTypeAvailability.AVAILABLE) {
                    onHeartRate(null)
                }
            }
        }
        callback = registered
        exerciseClient.setUpdateCallback(registered)
    }

    override suspend fun startExercise(): Boolean = healthCall {
        exerciseClient.startExercise(
            ExerciseConfig(
                exerciseType = ExerciseType.RUNNING,
                dataTypes = setOf(DataType.HEART_RATE_BPM),
                isAutoPauseAndResumeEnabled = false,
                isGpsEnabled = false,
            ),
        )
        true
    } ?: false

    override suspend fun stop() {
        if (currentOwner() == ExerciseOwner.ME) healthCall { exerciseClient.endExercise() }
        val registered = callback ?: return
        callback = null
        healthCall { exerciseClient.clearUpdateCallback(registered) }
    }

    // 심박은 부가 표시라 실패해도 러닝 화면을 막지 않는다
    private inline fun <T> healthCall(call: () -> T): T? = try {
        call()
    } catch (e: HealthServicesException) {
        Log.w(TAG, "Health Services 호출 실패", e)
        null
    } catch (e: SecurityException) {
        Log.w(TAG, "심박 권한 없음", e)
        null
    }

    private companion object {
        const val TAG = "HeartRateTracker"
    }
}
