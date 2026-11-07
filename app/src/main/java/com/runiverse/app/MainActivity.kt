package com.runiverse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.compose.foundation.SwipeToDismissValue
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.SwipeToDismissBox
import com.runiverse.app.exercise.ExerciseService
import com.runiverse.app.phone.finishedRunRemovals
import com.runiverse.app.run.RunCommandState
import com.runiverse.app.run.RunCommands
import com.runiverse.app.run.RunScreen
import com.runiverse.app.run.RunSummaryScreen
import com.runiverse.app.ui.theme.RuniverseTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val runiverseApp get() = application as RuniverseApp
    private val phoneLink get() = runiverseApp.phoneLink

    // 결과가 지워진 채 닫히면 그 사이 대기 화면이 보여 내용을 숨긴다
    private var isClosing by mutableStateOf(false)
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
        lifecycleScope.launch {
            phoneLink.state.finishedRunRemovals().collect { finishHidden() }
        }
        setContent {
            val linkState by phoneLink.state.collectAsState()
            val heartRateBpm by runiverseApp.exerciseSession.heartRateBpm.collectAsState()
            val isComboEffectOn by runiverseApp.comboEffectSetting.isOn.collectAsState()
            val run = linkState.phoneState?.run?.takeIf { linkState.isRunActive }
            val finishedRun = linkState.finishedRun
            val commands = remember {
                RunCommands(
                    pause = { phoneLink.pause() },
                    resume = { phoneLink.resume() },
                    finish = { forced -> phoneLink.finish(forced) },
                )
            }
            RuniverseTheme {
                AppScaffold {
                    if (run == null) {
                        val dismissState = rememberSwipeToDismissBoxState()
                        var isDismissed by remember { mutableStateOf(false) }
                        // 라이브러리가 닫기 함수를 부르기 전에 화면을 되돌려, 앱이 한 번 더 보이지 않게 숨긴다
                        if (dismissState.currentValue == SwipeToDismissValue.Dismissed) isDismissed = true
                        // 시스템 닫기는 테마에서 껐다 — 러닝이 아닌 화면만 여기서 쓸어 닫는다
                        SwipeToDismissBox(
                            onDismissed = { if (!phoneLink.state.value.isRunActive) closeScreen() },
                            state = dismissState,
                        ) { isBackground ->
                            if (!isBackground && !isDismissed && !isClosing) {
                                if (finishedRun != null) {
                                    RunSummaryScreen(finishedRun, linkState.isPhoneReachable, ::closeScreen)
                                } else {
                                    IdleScreen(linkState.isPhoneReachable)
                                }
                            }
                        }
                    } else {
                        RunScreen(
                            run,
                            linkState.isPhoneReachable,
                            heartRateBpm,
                            linkState.birthday,
                            RunCommandState(linkState.isLocked, linkState.isFinishPending, linkState.isCommandOverdue),
                            commands,
                            isComboEffectOn,
                            runiverseApp.comboEffectSetting::set,
                        )
                    }
                }
            }
        }
    }

    // 어떻게 닫아도 결과가 다시 뜨지 않게 한 길로 닫는다
    private fun closeScreen() {
        phoneLink.dismissResult()
        finishHidden()
    }

    private fun finishHidden() {
        isClosing = true
        finish()
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
