package com.runiverse.app.exercise

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.runiverse.app.FakeRuniverseApp
import com.runiverse.app.phone.PhoneState
import com.runiverse.app.phone.RunState
import com.runiverse.app.phone.RunStatus
import com.runiverse.app.phone.WatchStateUpdated
import com.runiverse.app.phone.WatchSync
import com.runiverse.app.phone.WatchSynced
import com.runiverse.app.run.common.RunPreviewSamples
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = FakeRuniverseApp::class)
class ExerciseServiceTest {

    private val app get() = ApplicationProvider.getApplicationContext<FakeRuniverseApp>()
    private val soloRun = RunPreviewSamples.solo.copy(runningRoomId = null)

    @After
    fun cancelTestScope() {
        app.testScope.cancel()
    }

    @Test
    fun `시작하면 포그라운드 알림을 띄우고 심박 측정을 시작한다`() {
        // given
        answerSync(soloRun)

        // when
        val service = startService().get()
        runLooper()

        // then
        assertTrue(shadowOf(service).lastForegroundNotificationId != 0)
        assertTrue("start" in app.heartRateTracker.calls)
    }

    @Test
    fun `러닝이 끝나면 운동을 끈 뒤 서비스를 내린다`() {
        // given
        answerSync(soloRun)
        val service = startService().get()
        runLooper()
        assertFalse(shadowOf(service).isStoppedBySelf)

        // when
        val finished = soloRun.copy(status = RunStatus.FINISHED)
        app.phoneLink.onReceived(WatchStateUpdated(phoneState(2, finished)))
        runLooper()

        // then
        assertTrue(shadowOf(service).isStoppedBySelf)
        assertEquals("stop", app.heartRateTracker.calls.last())
    }

    @Test
    fun `서비스가 사라지면 운동을 끈다`() {
        // given
        answerSync(soloRun)
        val controller = startService()
        runLooper()

        // when
        controller.destroy()
        runLooper()

        // then
        assertEquals("stop", app.heartRateTracker.calls.last())
    }

    private fun startService() = Robolectric.buildService(ExerciseService::class.java).create().startCommand(0, 1)

    private fun runLooper() = shadowOf(Looper.getMainLooper()).idle()

    private fun answerSync(run: RunState) {
        app.phoneLink.start()
        runLooper()
        val syncId = app.sent.filterIsInstance<WatchSync>().last().syncId
        app.phoneLink.onReceived(WatchSynced(syncId, phoneState(1, run), birthday = null, photos = emptyList()))
        runLooper()
    }

    private fun phoneState(stateSequence: Long, run: RunState) =
        PhoneState(phoneInstanceId = "phone", stateSequence = stateSequence, statusVersion = 1, run = run)
}
