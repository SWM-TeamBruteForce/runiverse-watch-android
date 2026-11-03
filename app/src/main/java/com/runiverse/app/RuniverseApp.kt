package com.runiverse.app

import android.app.Application
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.runiverse.app.phone.PhoneChannel
import com.runiverse.app.phone.PhoneLink
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

// 화면이 닫혀도 연결을 이어 가려고 폰 연결은 화면이 아니라 앱에 하나 둔다
class RuniverseApp : Application() {

    private val scope = MainScope()
    private val phoneChannel by lazy { PhoneChannel(this) }
    val phoneLink by lazy { PhoneLink(scope, phoneChannel::send) }

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            phoneChannel.messages
                .retryWhen { cause, _ ->
                    if (cause !is ApiException) return@retryWhen false
                    Log.w(TAG, "폰 메시지 받기 등록 실패 — 3초 뒤 다시 등록", cause)
                    delay(RETRY_DELAY)
                    true
                }
                .collect(phoneLink::onReceived)
        }
    }

    private companion object {
        const val TAG = "RuniverseApp"
        val RETRY_DELAY = 3.seconds
    }
}
