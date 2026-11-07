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
        // 폰은 화면이 꺼져 있어도 종료 단계를 바로 보낸다 — 화면 코드에 두면 꺼진 동안엔 안 울린다
        scope.launch {
            phoneLink.state.goalPendingStarts().collect { vibrateOnce() }
        }
    }

    // 용도를 안 정하면 터치 반응으로 분류돼 터치 진동을 끈 사람에겐 안 울리고, 알림 용도는 워치8에서 앱이 뒤에 있을 때 막힌다
    private fun vibrateOnce() {
        val effect = VibrationEffect.createOneShot(GOAL_PENDING_VIBRATION_MILLIS, VibrationEffect.DEFAULT_AMPLITUDE)
        val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
        getSystemService(Vibrator::class.java).vibrate(effect, attributes)
    }

    private companion object {
        const val TAG = "RuniverseApp"
        const val GOAL_PENDING_VIBRATION_MILLIS = 400L
        val RETRY_DELAY = 3.seconds
        val MAX_RETRY_DELAY = 1.minutes
    }
}
