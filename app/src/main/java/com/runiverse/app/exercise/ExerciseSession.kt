package com.runiverse.app.exercise

import android.os.SystemClock
import com.runiverse.app.phone.PhoneLink
import com.runiverse.app.phone.PhoneLinkState
import com.runiverse.app.phone.PhoneState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * 러닝 동안 심박을 재고, 러닝이 끝났거나 폰이 돌아오지 않으면 세션을 끝낸다.
 *
 * [scope]는 한 스레드에서 돌아야 한다 — 상태를 잠금 없이 고친다.
 */
class ExerciseSession(
    private val scope: CoroutineScope,
    private val phoneLink: PhoneLink,
    private val tracker: HeartRateTracker,
    private val nowMs: () -> Long = SystemClock::elapsedRealtime,
) {
    private val _heartRateBpm = MutableStateFlow<Int?>(null)
    val heartRateBpm: StateFlow<Int?> = _heartRateBpm.asStateFlow()

    // 받는 화면이 없으면 닫을 것도 없어 버린다
    private val _phoneLost = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val phoneLost: SharedFlow<Unit> = _phoneLost.asSharedFlow()

    // 끝내는 중에 시작하면 끝나 가는 운동에 붙으므로 차례로 부른다
    private val trackerCalls = Mutex()
    private var isMeasuring = false
    private var onEnded: (() -> Unit)? = null
    private var phoneWatchJob: Job? = null
    private var heardState: PhoneState? = null
    private var heardAtMs = 0L
    private var probedAtMs = 0L
    private var unansweredSinceMs: Long? = null

    private val isActive get() = phoneWatchJob?.isActive == true

    init {
        // 앱이 죽어도 운동은 남는다 — 시작한 적 없이 러닝이 없다고 들으면 남은 운동을 끈다
        scope.launch {
            phoneLink.state
                .map { it.phoneState != null && !it.isRunActive }
                .distinctUntilChanged()
                .filter { it }
                .collect {
                    if (phoneWatchJob != null) return@collect
                    trackerCalls.withLock { if (phoneWatchJob == null) stopTracker() }
                }
        }
    }

    /** 시작 요청마다 부른다 — 다른 앱에 밀렸던 심박을 화면이 켜질 때 다시 잰다. */
    fun start(onEnded: () -> Unit) {
        this.onEnded = onEnded
        if (!isActive) {
            heardState = phoneLink.state.value.phoneState
            heardAtMs = nowMs()
            probedAtMs = heardAtMs
            unansweredSinceMs = null
            phoneWatchJob = scope.launch {
                launch { phoneLink.state.collect(::onPhoneLinkChanged) }
                // 화면이 켜져 있으면 심박이 없어도(권한 거부) 폰을 판단한다
                while (true) {
                    delay(CHECK_INTERVAL)
                    checkPhone()
                }
            }
        }
        scope.launch { trackerCalls.withLock { startMeasuring() } }
    }

    /** 서비스가 사라질 때 부른다. */
    fun stop() = finish(onFinished = null)

    private fun finish(onFinished: (() -> Unit)?) {
        if (!isActive) return
        phoneWatchJob?.cancel()
        onEnded = null
        _heartRateBpm.value = null
        scope.launch {
            trackerCalls.withLock {
                isMeasuring = false
                stopTracker()
            }
            // 서비스를 먼저 내리면 화면이 꺼진 사이 앱이 얼어 운동 끄기가 안 나갈 수 있다
            onFinished?.invoke()
        }
    }

    // 서비스가 내려가면 다시 끌 길이 없어, 끄기가 실패하면 조금 뒤 다시 시도한다
    private suspend fun stopTracker() {
        for (attempt in 1..STOP_ATTEMPT_COUNT) {
            if (tracker.stop()) return
            if (attempt < STOP_ATTEMPT_COUNT) delay(STOP_RETRY_DELAY)
        }
    }

    private suspend fun startMeasuring() {
        if (!isActive || isMeasuring || !tracker.canMeasure()) return
        // 남의 운동을 끝내면서까지 부가 표시인 심박을 재지 않는다
        val owner = tracker.currentOwner()
        // 묻는 사이 러닝이 끝났으면 끝난 세션에 운동을 시작하지 않는다
        if (!isActive || owner == null || owner == ExerciseOwner.OTHER_APP) return
        tracker.listen(onHeartRate = ::onHeartRate, onMeasuringStopped = ::onMeasuringStopped)
        // 시작을 기다리는 사이 다른 앱에 밀릴 수 있어, 끝난 뒤에 표시하면 밀린 사실을 덮어쓴다
        isMeasuring = true
        if (owner == ExerciseOwner.NONE && !tracker.startExercise()) isMeasuring = false
    }

    private fun onHeartRate(heartRateBpm: Int?) {
        if (!isActive) return
        _heartRateBpm.value = heartRateBpm
        checkPhone()
    }

    private fun onMeasuringStopped() {
        isMeasuring = false
        _heartRateBpm.value = null
    }

    private fun onPhoneLinkChanged(link: PhoneLinkState) {
        if (!link.isRunActive) {
            end()
            return
        }
        if (link.phoneState !== heardState) {
            heardState = link.phoneState
            heardAtMs = nowMs()
            unansweredSinceMs = null
        }
    }

    // 화면이 꺼지면 delay가 밀린다 — 깨어날 때마다 잠든 시간까지 세는 시계로 잰다
    // 화면이 꺼진 동안 폰이 조용한 건 정상이라, 물어본 뒤에도 답이 없는 시간만 센다
    private fun checkPhone() {
        // 방금 받은 폰 답이 아직 수집기에 안 닿았을 수 있어 최신 상태부터 반영한다
        onPhoneLinkChanged(phoneLink.state.value)
        if (!isActive) return
        val currentMs = nowMs()
        if (currentMs - heardAtMs < PROBE_AFTER_MS) return
        val sinceMs = unansweredSinceMs ?: currentMs.also { unansweredSinceMs = it }
        if (currentMs - probedAtMs < PROBE_AFTER_MS) return
        // 오래 잠든 사이 폰이 돌아왔을 수 있어, 10분이 지난 뒤에 보낸 물음에도 답이 없어야 끝낸다
        if (probedAtMs - sinceMs >= GIVE_UP_AFTER_MS) {
            giveUp()
            return
        }
        probedAtMs = currentMs
        phoneLink.probeWhileScreenOff()
    }

    private fun giveUp() {
        phoneLink.forgetRun()
        end()
        _phoneLost.tryEmit(Unit)
    }

    private fun end() = finish(onFinished = onEnded)

    private companion object {
        val CHECK_INTERVAL = 1.minutes
        val PROBE_AFTER_MS = 1.minutes.inWholeMilliseconds
        val GIVE_UP_AFTER_MS = 10.minutes.inWholeMilliseconds
        const val STOP_ATTEMPT_COUNT = 3
        val STOP_RETRY_DELAY = 1.seconds
    }
}
