package com.runiverse.app

import com.runiverse.app.exercise.ExerciseSession
import com.runiverse.app.exercise.FakeHeartRateTracker
import com.runiverse.app.phone.PhoneLink
import com.runiverse.app.phone.WatchMessage
import kotlinx.coroutines.MainScope

// 수신 등록·진동을 돌리지 않고, 실제 PhoneLink·ExerciseSession으로 보낸 메시지와 측정 호출만 기록한다
// Android가 리플렉션으로 만들어 private로 둘 수 없다
class FakeRuniverseApp : RuniverseApp() {
    val testScope = MainScope()
    val sent = mutableListOf<WatchMessage>()
    internal val heartRateTracker = FakeHeartRateTracker()
    override val phoneLink = PhoneLink(testScope, send = { sent.add(it) })
    override val exerciseSession = ExerciseSession(testScope, phoneLink, heartRateTracker)

    override fun onCreate() = Unit
}
