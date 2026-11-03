package com.runiverse.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.google.android.gms.common.api.ApiException
import com.runiverse.app.phone.PhoneChannel
import com.runiverse.app.phone.PhoneLink
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {

    private val phoneChannel by lazy { PhoneChannel(this) }
    private val phoneLink by lazy { PhoneLink(lifecycleScope, phoneChannel::send) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            phoneChannel.messages
                .retryWhen { cause, _ ->
                    if (cause !is ApiException) return@retryWhen false
                    Log.w(TAG, "폰 메시지 받기 등록 실패 — 3초 뒤 다시 등록", cause)
                    delay(RETRY_DELAY)
                    true
                }
                .collect(phoneLink::onReceived)
        }
        phoneLink.start()
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> phoneLink.onScreenVisibilityChanged(true)
                Lifecycle.Event.ON_STOP -> phoneLink.onScreenVisibilityChanged(false)
                else -> Unit
            }
        })
        setContent {
            MaterialTheme {
                AppScaffold {
                    ScreenScaffold {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = stringResource(R.string.app_name))
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val TAG = "MainActivity"
        val RETRY_DELAY = 3.seconds
    }
}
