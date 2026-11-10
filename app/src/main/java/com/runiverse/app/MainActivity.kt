package com.runiverse.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.compose.foundation.SwipeToDismissValue
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.SwipeToDismissBox
import com.runiverse.app.exercise.ExerciseService
import com.runiverse.app.phone.WatchComboTriggered
import com.runiverse.app.phone.finishedRunRemovals
import com.runiverse.app.run.ComboBurstScreen
import com.runiverse.app.run.RunScreen
import com.runiverse.app.run.RunSummaryScreen
import com.runiverse.app.run.common.ComboMomentSize
import com.runiverse.app.run.common.RunCommandState
import com.runiverse.app.run.common.RunCommands
import com.runiverse.app.run.common.SMALL_COMBO_MOMENT_DURATION
import com.runiverse.app.run.common.comboMomentSizeOrNull
import com.runiverse.app.run.common.decodePlayerPhotos
import com.runiverse.app.ui.theme.RuniverseTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val runiverseApp get() = application as RuniverseApp
    private val phoneLink get() = runiverseApp.phoneLink

    // 결과가 지워진 채 닫히면 그 사이 대기 화면이 보여 내용을 숨긴다
    private var isClosing by mutableStateOf(false)

    // 사람마다 따로 둔다 — 한 통지에 여러 사람이 함께 오른다
    private var smallComboMoments by mutableStateOf(emptyMap<String, String>())

    // 다 그릴 때까지 다음 큰 순간은 받지 않는다 — 폰도 임팩트 중엔 새로 터뜨리지 않는다
    private var bigComboMoment by mutableStateOf<WatchComboTriggered?>(null)

    // 조작 장에선 덮개가 버튼을 가려 큰 순간을 띄우지 않는다 — 진동만 한다
    private var isControlsShown = false

    // 답하기 전에 시작하면 서비스가 알림 권한 없이 알림을 한 번 올리고 다시 올리지 않는다
    private var isPermissionRequestPending = false

    private val permissionRequest = registerForActivityResult(RequestMultiplePermissions()) {
        isPermissionRequestPending = false
        val linkState = phoneLink.state.value
        if (linkState.isRunActive && !linkState.isSyncing) ExerciseService.start(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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
            // 화면이 꺼진 사이 온 콤보는 진동만 한다 — 켜졌을 때 효과를 뒤늦게 틀지 않는다
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    phoneLink.combos.collect { showComboMoment(it) }
                } finally {
                    smallComboMoments = emptyMap()
                    bigComboMoment = null
                }
            }
        }
        lifecycleScope.launch {
            runiverseApp.exerciseSession.phoneLost.collect { finishHidden() }
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
            val playerPhotos = remember(linkState.photos) { decodePlayerPhotos(linkState.photos) }
            val commands = remember {
                RunCommands(
                    pause = { phoneLink.pause() },
                    resume = { phoneLink.resume() },
                    finish = { forced -> phoneLink.finish(forced) },
                )
            }
            RuniverseTheme {
                Box(Modifier.fillMaxSize()) {
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
                                    when {
                                        linkState.isUpdateRequired -> UpdateRequiredScreen(::openStore)
                                        finishedRun != null ->
                                            RunSummaryScreen(finishedRun, linkState.isPhoneReachable, ::closeScreen)

                                        else -> {
                                            // 러닝 화면에서 물으면 권한 창이 러닝을 가려, 대기 화면에서 먼저 묻는다
                                            LaunchedEffect(Unit) { requestPermissionsOnce() }
                                            IdleScreen(linkState.isPhoneReachable)
                                        }
                                    }
                                }
                            }
                        } else {
                            RunScreen(
                                run,
                                linkState.isPhoneReachable,
                                heartRateBpm,
                                linkState.birthday,
                                RunCommandState(
                                    linkState.isLocked,
                                    linkState.isFinishPending,
                                    linkState.isCommandOverdue,
                                ),
                                commands,
                                isComboEffectOn,
                                runiverseApp.comboEffectSetting::set,
                                smallComboMoments,
                                playerPhotos,
                                onControlsShownChange = { isControlsShown = it },
                            )
                        }
                    }
                    // 시계까지 덮으려고 AppScaffold 바깥에 얹는다 — 시계는 AppScaffold가 내용 위에 그린다
                    bigComboMoment?.let { combo ->
                        val player = run?.players?.find { it.userId == combo.userId }
                        if (player == null) {
                            // 명단에 없거나 러닝이 끝났으면 진동만 하고 접는다
                            LaunchedEffect(combo.eventId) { bigComboMoment = null }
                        } else {
                            key(combo.eventId) {
                                ComboBurstScreen(
                                    combo.comboCount,
                                    player,
                                    playerPhotos[player.userId],
                                    onFinished = { bigComboMoment = null },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 넘겨 둔 장이 돌아와 늦게 그려져도 지난 순간을 틀지 않게 시간이 지나면 지운다
    private fun CoroutineScope.showComboMoment(combo: WatchComboTriggered) {
        val isEffectOn = runiverseApp.comboEffectSetting.isOn.value
        when (comboMomentSizeOrNull(combo.kind, isEffectOn)) {
            ComboMomentSize.SMALL -> {
                smallComboMoments = smallComboMoments + (combo.userId to combo.eventId)
                launch {
                    delay(SMALL_COMBO_MOMENT_DURATION)
                    if (smallComboMoments[combo.userId] == combo.eventId) {
                        smallComboMoments = smallComboMoments - combo.userId
                    }
                }
            }

            ComboMomentSize.BIG -> if (bigComboMoment == null && !isControlsShown) bigComboMoment = combo
            null -> Unit
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

    // 워치8에서 이 주소가 워치 Play 스토어의 앱 상세를 바로 연다
    private fun openStore(): Boolean = try {
        startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    // 대기 화면을 거치지 않고 러닝이 먼저 보이면 여기서 묻는다
    private fun startExerciseService() {
        requestPermissionsOnce()
        if (!isPermissionRequestPending) ExerciseService.start(this)
    }

    // 권한 창은 한 번만 띄운다 — 거부해도 심박 없이 러닝 화면은 그대로 쓴다
    private fun requestPermissionsOnce() {
        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        if (preferences.getBoolean(KEY_PERMISSIONS_ASKED, false)) return
        preferences.edit { putBoolean(KEY_PERMISSIONS_ASKED, true) }
        isPermissionRequestPending = true
        permissionRequest.launch(ExerciseService.PERMISSIONS)
    }

    private companion object {
        const val PREFERENCES_NAME = "exercise"
        const val KEY_PERMISSIONS_ASKED = "permissions_asked"
    }
}
