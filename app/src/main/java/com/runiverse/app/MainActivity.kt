package com.runiverse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.SwipeToDismissBox
import com.runiverse.app.exercise.ExerciseService
import com.runiverse.app.run.RunScreen
import com.runiverse.app.ui.theme.RuniverseTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val runiverseApp get() = application as RuniverseApp
    private val phoneLink get() = runiverseApp.phoneLink
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
            val link by phoneLink.state.collectAsState()
            val heartRateBpm by runiverseApp.exerciseSession.heartRateBpm.collectAsState()
            val run = link.phoneState?.run?.takeIf { link.isRunActive }
            RuniverseTheme {
                AppScaffold {
                    // 시스템 닫기는 테마에서 껐다 — 러닝 중에만 막고 나머지 화면은 여기서 쓸어 닫는다
                    SwipeToDismissBox(onDismissed = ::finish, userSwipeEnabled = run == null) { isBackground ->
                        if (isBackground) return@SwipeToDismissBox
                        if (run == null) {
                            IdleScreen()
                        } else {
                            RunScreen(run, link.isPhoneReachable, heartRateBpm, link.birthday)
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
