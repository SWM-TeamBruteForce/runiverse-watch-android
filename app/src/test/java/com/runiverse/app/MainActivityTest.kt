package com.runiverse.app

import android.os.Looper
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import com.runiverse.app.phone.PhoneLink
import com.runiverse.app.phone.PhoneMessage
import com.runiverse.app.phone.PhoneState
import com.runiverse.app.phone.RunState
import com.runiverse.app.phone.RunStatus
import com.runiverse.app.phone.WatchMessage
import com.runiverse.app.phone.WatchPause
import com.runiverse.app.phone.WatchStateUpdated
import com.runiverse.app.phone.WatchSync
import com.runiverse.app.phone.WatchSynced
import com.runiverse.app.run.RunPreviewSamples
import com.runiverse.app.run.formatElapsedTime
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

// 닫기 애니메이션 200ms보다 넉넉히 돌린다
private const val SWIPE_SETTLE_MS = 1_000L
private const val SWIPE_SETTLE_FRAME_COUNT = 60
private const val REPLY_TIMEOUT_PASSED_MS = 3_500L
private const val RESULT_REPLY_TIMEOUT_PASSED_MS = 13_500L
private const val PHONE_LOST_NOT_YET_MS = 9 * 60_000L
private const val PHONE_LOST_PASSED_MS = 4 * 60_000L

@RunWith(RobolectricTestRunner::class)
@Config(application = FakeRuniverseApp::class, qualifiers = "w226dp-h226dp-round-watch-xhdpi")
class MainActivityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val app get() = composeRule.activity.application as FakeRuniverseApp
    private var nextStateSequence = 1L

    // 오프라인 솔로는 방 번호가 없어 SYNC 없이 바로 받는다
    private val soloRun = RunPreviewSamples.solo.copy(runningRoomId = null)
    private val appName get() = composeRule.activity.getString(R.string.app_name)

    @After
    fun cancelTestScope() {
        app.testScope.cancel()
    }

    @Test
    fun `러닝이 없으면 대기 화면을 보인다`() {
        // when
        answerSync(run = null)
        idleFor(REPLY_TIMEOUT_PASSED_MS)

        // then -> SYNCED를 못 받았으면 3초 뒤 폰 연결 확인 중으로 바뀐다
        composeRule.onNodeWithText(appName).assertExists()
    }

    @Test
    fun `러닝이 시작되면 러닝 화면을 보인다`() {
        // given
        answerSync(run = null)

        // when
        receiveState(soloRun)
        composeRule.waitForIdle()

        // then
        composeRule.onNodeWithText(formatElapsedTime(soloRun.me.elapsedSeconds)).assertExists()
    }

    @Test
    fun `러닝이 끝나면 결과 화면을 보인다`() {
        // given
        answerSync(run = null)

        // when
        receiveState(soloRun.copy(status = RunStatus.FINISHED, result = RunPreviewSamples.result))
        composeRule.waitForIdle()

        // then
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.run_summary_completed)).assertExists()
    }

    @Test
    fun `대기 화면을 끝까지 쓸면 앱을 닫는다`() {
        // given
        answerSync(run = null)

        // when
        composeRule.onRoot().performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        // then
        assertTrue(composeRule.activity.isFinishing)
    }

    @Test
    fun `쓸어 닫는 도중 러닝이 시작되면 앱을 닫지 않고 러닝 화면을 보인다`() {
        // given
        answerSync(run = null)
        composeRule.onNodeWithText(appName).assertExists()
        composeRule.mainClock.autoAdvance = false
        composeRule.onRoot().performTouchInput { swipeRight() }

        // when -> 손을 뗀 직후라 닫기 애니메이션(200ms)이 아직 안 끝났다
        receiveState(soloRun)
        composeRule.mainClock.advanceTimeBy(SWIPE_SETTLE_MS)
        composeRule.waitForIdle()

        // then
        assertFalse(composeRule.activity.isFinishing)
        composeRule.onNodeWithText(formatElapsedTime(soloRun.me.elapsedSeconds)).assertExists()
    }

    @Test
    fun `폰에서 결과를 닫으면 대기 화면을 보이지 않고 앱을 닫는다`() {
        // given
        val completed = composeRule.activity.getString(R.string.run_summary_completed)
        val phoneChecking = composeRule.activity.getString(R.string.run_phone_checking)
        answerSync(run = null)
        receiveState(soloRun.copy(status = RunStatus.FINISHED, result = RunPreviewSamples.result))
        composeRule.waitForIdle()
        composeRule.onNodeWithText(completed).assertExists()

        // when
        receiveState(run = null)
        composeRule.waitForIdle()

        // then
        assertTrue(composeRule.activity.isFinishing)
        composeRule.onRoot().assertExists()
        composeRule.onNodeWithText(completed).assertDoesNotExist()
        composeRule.onNodeWithText(appName).assertDoesNotExist()
        composeRule.onNodeWithText(phoneChecking).assertDoesNotExist()
    }

    @Test
    fun `대기 화면을 쓸어 닫을 때 제자리로 돌아와 다시 보이지 않는다`() {
        // given
        answerSync(run = null)
        composeRule.mainClock.autoAdvance = false

        // when
        composeRule.onRoot().performTouchInput { swipeRight() }
        val lefts = List(SWIPE_SETTLE_FRAME_COUNT) {
            composeRule.mainClock.advanceTimeByFrame()
            appNameLeftOrNull()
        }

        // then
        val shownLefts = lefts.takeWhile { it != null }.filterNotNull()
        assertTrue(shownLefts.isNotEmpty())
        assertEquals(shownLefts.sorted(), shownLefts)
        assertTrue(lefts.drop(shownLefts.size).all { it == null })
        assertTrue(composeRule.activity.isFinishing)
    }

    @Test
    fun `결과에서 닫기를 누르면 대기 화면 없이 닫고 결과를 기억한다`() {
        // given
        showFinishedRun()

        // when
        composeRule.onNodeWithText(text(R.string.run_summary_close)).performClick()
        composeRule.waitForIdle()

        // then
        assertClosedWithoutIdleScreen()
    }

    @Test
    fun `결과에서 뒤로가면 대기 화면 없이 닫고 결과를 기억한다`() {
        // given
        showFinishedRun()

        // when
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        // then
        assertClosedWithoutIdleScreen()
    }

    @Test
    fun `결과를 쓸어 닫으면 대기 화면 없이 닫고 결과를 기억한다`() {
        // given
        showFinishedRun()

        // when
        composeRule.onRoot().performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        // then
        assertClosedWithoutIdleScreen()
    }

    @Test
    fun `결과에서 새 러닝이 시작되면 앱을 닫지 않고 러닝 화면을 보인다`() {
        // given
        showFinishedRun()

        // when
        receiveState(soloRun)
        composeRule.waitForIdle()

        // then
        assertFalse(composeRule.activity.isFinishing)
        composeRule.onNodeWithText(formatElapsedTime(soloRun.me.elapsedSeconds)).assertExists()
    }

    @Test
    fun `조작 장에서 일시정지를 누르면 폰에 보내고 폰 답을 받아야 화면이 바뀐다`() {
        // given
        answerSync(soloRun)
        composeRule.onRoot().performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        // when
        composeRule.onNodeWithText(text(R.string.controls_pause)).performClick()
        composeRule.waitForIdle()

        // then
        val pause = app.sent.filterIsInstance<WatchPause>().single()
        composeRule.onNodeWithText(text(R.string.controls_pause)).assertExists()

        // when -> 폰이 멈춘 상태로 답한다
        receiveState(soloRun.copy(status = RunStatus.PAUSED), lastRequestId = pause.requestId)
        composeRule.waitForIdle()

        // then
        composeRule.onNodeWithText(text(R.string.controls_resume)).assertExists()
    }

    @Test
    fun `폰 답이 3초 없으면 대기 화면에 폰 연결 확인 중을 보인다`() {
        // given
        composeRule.waitForIdle()

        // when
        idleFor(REPLY_TIMEOUT_PASSED_MS)

        // then
        composeRule.onNodeWithText(text(R.string.run_phone_checking)).assertExists()
    }

    @Test
    fun `결과를 받는 중 13초 소식이 없으면 폰 연결 확인 중을 보인다`() {
        // given
        answerSync(soloRun.copy(status = RunStatus.FINISHED, result = null))

        // when -> 끝난 뒤엔 3초 조용해도 정상이다
        idleFor(REPLY_TIMEOUT_PASSED_MS)

        // then
        composeRule.onNodeWithText(text(R.string.run_phone_checking)).assertDoesNotExist()

        // when
        idleFor(RESULT_REPLY_TIMEOUT_PASSED_MS - REPLY_TIMEOUT_PASSED_MS)

        // then
        composeRule.onNodeWithText(text(R.string.run_phone_checking)).assertExists()
    }

    @Test
    fun `폰에서 결과를 닫는 동안 어느 프레임에도 대기 화면이 보이지 않는다`() {
        // given
        showFinishedRun()
        composeRule.mainClock.autoAdvance = false

        // when
        receiveState(run = null)
        val lefts = List(SWIPE_SETTLE_FRAME_COUNT) {
            composeRule.mainClock.advanceTimeByFrame()
            appNameLeftOrNull()
        }

        // then
        assertTrue(lefts.all { it == null })
    }

    @Test
    fun `러닝 중 폰이 10분 넘게 돌아오지 않으면 대기 화면 없이 닫는다`() {
        // given
        answerSync(soloRun)
        composeRule.runOnUiThread { app.exerciseSession.start(onEnded = {}) }

        // when
        idleFor(PHONE_LOST_NOT_YET_MS)

        // then
        assertFalse(composeRule.activity.isFinishing)

        // when
        composeRule.mainClock.autoAdvance = false
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(PHONE_LOST_PASSED_MS))
        val lefts = List(SWIPE_SETTLE_FRAME_COUNT) {
            composeRule.mainClock.advanceTimeByFrame()
            appNameLeftOrNull()
        }

        // then
        assertTrue(composeRule.activity.isFinishing)
        assertTrue(lefts.all { it == null })
        composeRule.onNodeWithText(text(R.string.run_phone_checking)).assertDoesNotExist()
    }

    private fun idleFor(ms: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
        composeRule.waitForIdle()
    }

    private fun showFinishedRun() {
        answerSync(soloRun.copy(status = RunStatus.FINISHED, result = RunPreviewSamples.result))
        composeRule.onNodeWithText(text(R.string.run_summary_completed)).assertExists()
    }

    private fun assertClosedWithoutIdleScreen() {
        assertTrue(composeRule.activity.isFinishing)
        assertNull(app.phoneLink.state.value.finishedRun)
        composeRule.onNodeWithText(appName).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.run_summary_completed)).assertDoesNotExist()
    }

    private fun text(id: Int) = composeRule.activity.getString(id)

    private fun appNameLeftOrNull(): Float? =
        composeRule.onAllNodesWithText(appName).fetchSemanticsNodes().firstOrNull()?.boundsInRoot?.left

    private fun answerSync(run: RunState?) {
        composeRule.waitForIdle()
        val syncId = app.sent.filterIsInstance<WatchSync>().last().syncId
        receive(WatchSynced(syncId, phoneState(run), birthday = null, photos = emptyList()))
        composeRule.waitForIdle()
    }

    // SYNC 중에 받은 상태는 조용히 버려져, 시험이 엉뚱한 이유로 통과하거나 실패하지 않게 막는다
    private fun receiveState(run: RunState?, lastRequestId: String? = null) {
        check(!app.phoneLink.state.value.isSyncing) { "SYNC 중이라 상태를 버린다" }
        receive(WatchStateUpdated(phoneState(run, lastRequestId)))
    }

    private fun receive(message: PhoneMessage) {
        composeRule.runOnUiThread { app.phoneLink.onReceived(message) }
    }

    private fun phoneState(run: RunState?, lastRequestId: String? = null) = PhoneState(
        phoneInstanceId = "phone",
        stateSequence = nextStateSequence++,
        statusVersion = 1,
        lastRequestId = lastRequestId,
        run = run,
    )
}

// 수신 등록·진동을 돌리지 않고, 실제 PhoneLink로 보낸 메시지만 기록한다
// Android가 리플렉션으로 만들어 private로 둘 수 없다
class FakeRuniverseApp : RuniverseApp() {
    val testScope = MainScope()
    val sent = mutableListOf<WatchMessage>()
    override val phoneLink = PhoneLink(testScope, send = { sent.add(it) })

    override fun onCreate() = Unit
}
