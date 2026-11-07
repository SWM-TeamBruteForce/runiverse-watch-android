package com.runiverse.app

import android.app.Application
import android.media.AudioAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.runiverse.app.exercise.ExerciseSession
import com.runiverse.app.exercise.HealthServicesHeartRateTracker
import com.runiverse.app.phone.PhoneChannel
import com.runiverse.app.phone.PhoneLink
import com.runiverse.app.phone.goalPendingStarts
import com.runiverse.app.run.ComboEffectSetting
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// 화면이 닫혀도 연결을 이어 가려고 폰 연결은 화면이 아니라 앱에 하나 둔다
class RuniverseApp : Application() {

    private val scope = MainScope()
    private val phoneChannel by lazy { PhoneChannel(this) }
    val phoneLink by lazy { PhoneLink(scope, phoneChannel::send) }
    val exerciseSession by lazy { ExerciseSession(scope, phoneLink, HealthServicesHeartRateTracker(this)) }
    val comboEffectSetting by lazy { ComboEffectSetting(getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)) }

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            phoneChannel.messages
                .retryWhen { cause, attempt ->
                    if (cause !is ApiException) return@retryWhen false
                    // 구글 서비스가 계속 안 되면 배터리를 덜 쓰게 3초에서 1분까지 간격을 늘린다
                    val wait = (RETRY_DELAY * (1 shl attempt.coerceAtMost(5).toInt())).coerceAtMost(MAX_RETRY_DELAY)
                    Log.w(TAG, "폰 메시지 받기 등록 실패 — $wait 뒤 다시 등록", cause)
                    delay(wait)
                    true
                }
                .collect(phoneLink::onReceived)
        }
        // 화면이 꺼져 있어도 울리게 화면 코드 밖에서 받는다
        scope.launch {
            phoneLink.state.goalPendingStarts().collect { vibrateOnce() }
        }
    }

    // 기본 용도는 터치 진동 설정에 막히고, 알림 용도는 앱이 뒤에 있으면 막힌다
    private fun vibrateOnce() {
        val effect = VibrationEffect.createOneShot(GOAL_PENDING_VIBRATION_MS, GOAL_PENDING_VIBRATION_AMPLITUDE)
        val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
        getSystemService(Vibrator::class.java).vibrate(effect, attributes)
    }

    private companion object {
        const val TAG = "RuniverseApp"
        const val PREFERENCES_NAME = "settings"
        // 콤보의 짧은 여러 번과 구분되게 한 번만 세게 울린다
        const val GOAL_PENDING_VIBRATION_MS = 300L
        const val GOAL_PENDING_VIBRATION_AMPLITUDE = 255
        val RETRY_DELAY = 3.seconds
        val MAX_RETRY_DELAY = 1.minutes
    }
}
