package com.runiverse.app

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.runiverse.app.exercise.ExerciseService
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val runiverseApp get() = application as RuniverseApp
    private val phoneLink get() = runiverseApp.phoneLink

    // Play 서비스가 옛 Fragment를 끌어오지만, 이 결함은 FragmentActivity 것이고 여기는 ComponentActivity다
    @SuppressLint("InvalidFragmentVersionForActivityResult")
    private val permissionRequest = registerForActivityResult(RequestMultiplePermissions()) {
        ExerciseService.start(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        phoneLink.start()
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> phoneLink.onScreenStarted()
                Lifecycle.Event.ON_STOP -> phoneLink.onScreenStopped()
                else -> Unit
            }
        })
        lifecycleScope.launch {
            // 심박은 사용 중 권한이라 화면이 보일 때만 서비스를 시작할 수 있다
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                phoneLink.state
                    .map { it.isRunActive && !it.isSyncing }
                    .distinctUntilChanged()
                    .filter { it }
                    .collect { startExerciseService() }
            }
        }
        lifecycleScope.launch {
            runiverseApp.exerciseSession.phoneLost.collect { finish() }
        }
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

    // 권한 창은 첫 러닝 때 한 번만 띄운다 — 거부해도 심박 없이 러닝 화면은 그대로 쓴다
    private fun startExerciseService() {
        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        if (preferences.getBoolean(KEY_PERMISSIONS_ASKED, false)) {
            ExerciseService.start(this)
            return
        }
        preferences.edit { putBoolean(KEY_PERMISSIONS_ASKED, true) }
        permissionRequest.launch(ExerciseService.PERMISSIONS)
    }

    private companion object {
        const val PREFERENCES_NAME = "exercise"
        const val KEY_PERMISSIONS_ASKED = "permissions_asked"
    }
}
