package com.runiverse.app

import android.app.Application
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.runiverse.app.exercise.ExerciseSession
import com.runiverse.app.exercise.HealthServicesHeartRateTracker
import com.runiverse.app.phone.PhoneChannel
import com.runiverse.app.phone.PhoneLink
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
    }

    private companion object {
        const val TAG = "RuniverseApp"
        val RETRY_DELAY = 3.seconds
        val MAX_RETRY_DELAY = 1.minutes
    }
}
