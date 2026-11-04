package com.runiverse.app.phone

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

data class PhoneLinkState(
    /** 마지막으로 받은 폰 상태다. 연결이 끊겨도 지우지 않는다 — 화면은 이 값에서 멈춘다. */
    val phoneState: PhoneState? = null,
    val birthday: String? = null,
    val photos: List<PlayerPhoto> = emptyList(),
    /** false면 `폰 연결 확인 중`을 띄운다. */
    val isPhoneReachable: Boolean = true,
    /** SYNC를 보내고 SYNCED를 기다리는 중이다. */
    val isSyncing: Boolean = false,
    /** 보내고 아직 결과를 못 받은 명령이다. */
    val pendingRequestId: String? = null,
    /** 명령을 보낸 지 3초가 넘었다 — 종료면 `종료 확인 중`을 띄운다. */
    val isCommandOverdue: Boolean = false,
) {
    val isLocked: Boolean get() = isSyncing || pendingRequestId != null

    /** 마지막으로 받은 상태에 아직 끝나지 않은 러닝이 있다. */
    val isRunActive: Boolean get() = phoneState?.run?.let { it.status != RunStatus.FINISHED } ?: false
}

/**
 * 폰에서 받은 메시지로 연결 상태를 판정하고, 명령·SYNC·화면 켜짐 신호를 보낸다.
 *
 * [scope]는 한 스레드에서 돌아야 한다 — 상태를 잠금 없이 고친다.
 */
class PhoneLink(
    private val scope: CoroutineScope,
    private val send: suspend (WatchMessage) -> Unit,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    private val _state = MutableStateFlow(PhoneLinkState())
    val state: StateFlow<PhoneLinkState> = _state.asStateFlow()

    // TODO: 콤보 진동 — 콤보 효과를 만들 때 앱에서 구독한다. 받는 쪽이 없으면 버린다
    private val _combos = MutableSharedFlow<WatchComboTriggered>(extraBufferCapacity = COMBO_BUFFER)
    val combos: SharedFlow<WatchComboTriggered> = _combos.asSharedFlow()

    private val awaitedSyncIds = ArrayDeque<String>()
    private val seenComboEventIds = ArrayDeque<String>()
    private var startedScreens = 0
    private val isScreenVisible get() = startedScreens > 0
    private var syncJob: Job? = null
    private var silenceJob: Job? = null
    private var commandJob: Job? = null
    private var visibilityJob: Job? = null

    // 꺼짐·켜짐이 앞뒤로 바뀌어 닿지 않게 하나씩 차례로 보낸다. 밀린 신호는 마지막 것만 남긴다
    private val visibilitySends = Channel<Boolean>(Channel.CONFLATED)

    init {
        scope.launch {
            for (isVisible in visibilitySends) sendWithLimit(WatchVisibilityUpdate(isVisible = isVisible))
        }
    }

    /** 화면을 새로 만들 때 부른다 — 닫혀 있던 동안의 상태를 SYNC로 새로 맞춘다. */
    fun start() = requestSync()

    /** 화면이 꺼진 동안 폰이 아직 있는지 묻는다. 화면이 켜져 있으면 3초 감시가 이미 묻고 있다. */
    fun probeWhileScreenOff() {
        if (!isScreenVisible) requestSync()
    }

    /** 폰이 돌아오지 않아 러닝을 접는다. 다음 SYNCED 전까지 상태를 받지 않는다. */
    fun forgetRun() {
        syncJob?.cancel()
        silenceJob?.cancel()
        commandJob?.cancel()
        _state.value = PhoneLinkState()
    }

    // 화면 둘이 잠깐 겹쳐도 먼저 멈춘 쪽이 꺼짐을 보내지 않게 켜진 화면 수를 센다
    fun onScreenStarted() {
        if (startedScreens++ == 0) onScreenVisibilityChanged(true)
    }

    fun onScreenStopped() {
        if (startedScreens == 0) return
        if (--startedScreens == 0) onScreenVisibilityChanged(false)
    }

    private fun onScreenVisibilityChanged(isVisible: Boolean) {
        visibilityJob?.cancel()
        if (isVisible) {
            visibilityJob = scope.launch {
                while (true) {
                    visibilitySends.trySend(true)
                    delay(VISIBILITY_INTERVAL)
                }
            }
            if (_state.value.isSyncing) requestSync() else awaitVisibleReply()
        } else {
            visibilitySends.trySend(false)
            silenceJob?.cancel()
        }
    }

    fun onReceived(message: PhoneMessage) {
        when (message) {
            is WatchSynced -> onSynced(message)
            is WatchStateUpdated -> onStateUpdated(message.state)
            is WatchComboTriggered -> onComboTriggered(message)
            is PhoneError -> onError(message)
        }
    }

    /** 잠겨 있거나 러닝이 없으면 보내지 않고 false를 돌려준다. 아래 셋 모두 같다. */
    fun pause(): Boolean = sendCommand(::WatchPause)

    fun resume(): Boolean = sendCommand(::WatchResume)

    fun finish(forced: Boolean): Boolean = sendCommand { requestId, phoneInstanceId, runningRoomId, statusVersion ->
        WatchFinish(requestId, phoneInstanceId, runningRoomId, statusVersion, forced)
    }

    private fun sendCommand(build: (String, String, Long?, Int) -> WatchMessage): Boolean {
        val current = _state.value
        val phoneState = current.phoneState ?: return false
        val run = phoneState.run ?: return false
        if (current.isLocked) return false

        val requestId = newId()
        _state.update { it.copy(pendingRequestId = requestId, isCommandOverdue = false) }
        scope.launch {
            sendWithLimit(build(requestId, phoneState.phoneInstanceId, run.runningRoomId, phoneState.statusVersion))
        }
        commandJob?.cancel()
        // 명령은 다시 보내지 않는다 — 폰이 이미 처리했을 수 있어, 최신 상태부터 받아 본다
        commandJob = scope.launch {
            delay(REPLY_TIMEOUT)
            _state.update { it.copy(isCommandOverdue = true) }
            requestSync()
        }
        return true
    }

    private fun requestSync() {
        _state.update { it.copy(isSyncing = true) }
        silenceJob?.cancel()
        syncJob?.cancel()
        // 화면이 꺼져 있으면 한 번만 보내고, 켜질 때 다시 시작한다
        // 보내기가 늦어져도 3초는 그대로 센다 — 보내기를 기다리지 않는다
        syncJob = scope.launch {
            do {
                val syncId = newId()
                awaitedSyncIds.addCapped(syncId, SYNC_ID_MEMORY)
                scope.launch { sendWithLimit(WatchSync(syncId)) }
                delay(REPLY_TIMEOUT)
                _state.update { it.copy(isPhoneReachable = false) }
            } while (isScreenVisible)
        }
    }

    private fun onSynced(message: WatchSynced) {
        if (message.syncId !in awaitedSyncIds) return
        awaitedSyncIds.clear()
        syncJob?.cancel()
        commandJob?.cancel()
        _state.value = PhoneLinkState(
            phoneState = message.state,
            birthday = message.birthday,
            photos = message.photos,
        )
        awaitNextState()
    }

    private fun onStateUpdated(incoming: PhoneState) {
        val current = _state.value
        // SYNC 중엔 사진·번호를 새로 받기 전이라 이어 붙이지 않는다
        if (current.isSyncing) return
        val known = current.phoneState ?: return
        if (incoming.phoneInstanceId != known.phoneInstanceId) {
            requestSync()
            return
        }
        if (incoming.stateSequence <= known.stateSequence) return
        val incomingRoomId = incoming.run?.runningRoomId
        if (incomingRoomId != null && incomingRoomId != known.run?.runningRoomId) {
            requestSync()
            return
        }

        val isCommandDone = current.pendingRequestId != null && incoming.lastRequestId == current.pendingRequestId
        if (isCommandDone) commandJob?.cancel()
        _state.update {
            if (isCommandDone) {
                it.copy(phoneState = incoming, pendingRequestId = null, isCommandOverdue = false)
            } else {
                it.copy(phoneState = incoming)
            }
        }
        awaitNextState()
    }

    private fun onComboTriggered(combo: WatchComboTriggered) {
        if (combo.eventId in seenComboEventIds) return
        seenComboEventIds.addCapped(combo.eventId, COMBO_ID_MEMORY)
        _combos.tryEmit(combo)
    }

    private fun onError(error: PhoneError) {
        if (error.requestId == null || error.requestId != _state.value.pendingRequestId) return
        commandJob?.cancel()
        _state.update { it.copy(pendingRequestId = null, isCommandOverdue = false) }
    }

    // 폰은 켜짐 신호를 받으면 러닝이 없어도 지금 상태를 바로 보낸다 — 그 답이 없으면 끊긴 것이다
    private fun awaitVisibleReply() {
        silenceJob?.cancel()
        silenceJob = scope.launch {
            delay(REPLY_TIMEOUT)
            _state.update { it.copy(isPhoneReachable = false) }
            requestSync()
        }
    }

    // 폰은 화면이 켜져 있고 러닝이 끝나기 전일 때만 1초마다 보낸다 — 그 밖엔 조용한 게 정상이다
    private fun awaitNextState() {
        silenceJob?.cancel()
        if (!isScreenVisible || !_state.value.isRunActive) return
        silenceJob = scope.launch {
            delay(REPLY_TIMEOUT)
            _state.update { it.copy(isPhoneReachable = false) }
            requestSync()
        }
    }

    // 구글 서비스가 답을 안 줘도 보내기가 쌓이거나 다음 보내기를 막지 않게 3초에서 끊는다
    private suspend fun sendWithLimit(message: WatchMessage) {
        withTimeoutOrNull(REPLY_TIMEOUT) { send(message) }
    }

    private fun ArrayDeque<String>.addCapped(id: String, limit: Int) {
        addLast(id)
        if (size > limit) removeFirst()
    }

    private companion object {
        val REPLY_TIMEOUT = 3.seconds
        val VISIBILITY_INTERVAL = 10.seconds
        // 늦게 온 SYNCED도 받되, 화면을 켜 둔 채 폰이 사라져도 끝없이 쌓이지 않게 한다
        const val SYNC_ID_MEMORY = 5
        const val COMBO_ID_MEMORY = 64
        const val COMBO_BUFFER = 8
    }
}
