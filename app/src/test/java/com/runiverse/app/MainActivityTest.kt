package com.runiverse.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Looper
import android.util.Base64
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.lifecycle.Lifecycle
import com.runiverse.app.exercise.ExerciseService
import com.runiverse.app.phone.ComboKind
import com.runiverse.app.phone.PhoneError
import com.runiverse.app.phone.PhoneMessage
import com.runiverse.app.phone.PhoneState
import com.runiverse.app.phone.PlayerPhoto
import com.runiverse.app.phone.RunState
import com.runiverse.app.phone.RunStatus
import com.runiverse.app.phone.WatchComboTriggered
import com.runiverse.app.phone.WatchPause
import com.runiverse.app.phone.WatchStateUpdated
import com.runiverse.app.phone.WatchSync
import com.runiverse.app.phone.WatchSynced
import com.runiverse.app.phone.WatchVisibilityUpdate
import com.runiverse.app.run.common.RunPreviewSamples
import com.runiverse.app.run.common.formatElapsedTime
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.time.Duration

// 닫기 애니메이션 200ms보다 넉넉히 돌린다
private const val SWIPE_SETTLE_MS = 1_000L
private const val SWIPE_SETTLE_FRAME_COUNT = 60
private const val REPLY_TIMEOUT_PASSED_MS = 3_500L
private const val RESULT_REPLY_TIMEOUT_PASSED_MS = 13_500L
private const val PHONE_LOST_NOT_YET_MS = 9 * 60_000L
private const val PHONE_LOST_REMAINING_MS = 4 * 60_000L
private const val BURST_PASSED_MS = 1_300L

@RunWith(RobolectricTestRunner::class)
@Config(application = FakeRuniverseApp::class)
class MainActivityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val app get() = composeRule.activity.application as FakeRuniverseApp
    private var nextStateSequence = 1L

    // 오프라인 솔로는 방 번호가 없어 SYNC 없이 바로 받는다
    private val soloRun = RunPreviewSamples.solo.copy(runningRoomId = null)
    private val partyRun = RunPreviewSamples.party
    // 대기 화면 아이콘은 폰 연결 여부와 상관없이 있어, 폰이 끊긴 대기 화면도 이것으로 찾는다
    private val idleMark get() = composeRule.activity.getString(R.string.app_name)

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
        composeRule.onNodeWithContentDescription(idleMark).assertExists()
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
        composeRule.onNodeWithContentDescription(idleMark).assertExists()
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
    fun `대기 화면을 쓸어 닫을 때 제자리로 돌아와 다시 보이지 않는다`() {
        // given
        answerSync(run = null)
        composeRule.mainClock.autoAdvance = false

        // when
        composeRule.onRoot().performTouchInput { swipeRight() }
        val lefts = recordIdleMarkLefts()

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

        // when
        receiveState(soloRun.copy(status = RunStatus.PAUSED), lastRequestId = pause.requestId)
        composeRule.waitForIdle()

        // then
        composeRule.onNodeWithText(text(R.string.controls_resume)).assertExists()
    }

    @Test
    fun `앱을 열면 폰 답 전엔 문구 없이 그림만 보이고 답이 오면 러닝 시작 안내를 보인다`() {
        // when
        composeRule.waitForIdle()

        // then -> 폰 앱이 꺼져 있으면 답이 오지 않아, 답 전엔 러닝 시작을 안내하지 않는다
        composeRule.onNodeWithContentDescription(idleMark).assertExists()
        composeRule.onNodeWithText(text(R.string.idle_start_on_phone)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.run_phone_checking)).assertDoesNotExist()

        // when
        answerSync(run = null)

        // then
        composeRule.onNodeWithText(text(R.string.idle_start_on_phone)).assertExists()
    }

    @Test
    fun `화면이 멈췄다 다시 보이면 폰 답 전엔 문구 없이 그림만 보인다`() {
        // given
        answerSync(run = null)
        composeRule.onNodeWithText(text(R.string.idle_start_on_phone)).assertExists()

        // when -> 나가 있는 사이 폰 앱이 꺼졌을 수 있다
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()

        // then
        composeRule.onNodeWithContentDescription(idleMark).assertExists()
        composeRule.onNodeWithText(text(R.string.idle_start_on_phone)).assertDoesNotExist()

        // when
        receiveState(run = null)

        // then
        composeRule.onNodeWithText(text(R.string.idle_start_on_phone)).assertExists()
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
    fun `폰에서 결과를 닫으면 어느 프레임에도 대기 화면을 보이지 않고 앱을 닫는다`() {
        // given
        showFinishedRun()
        composeRule.mainClock.autoAdvance = false

        // when
        receiveState(run = null)
        val lefts = recordIdleMarkLefts()

        // then
        assertTrue(composeRule.activity.isFinishing)
        assertTrue(lefts.all { it == null })
        composeRule.onNodeWithText(text(R.string.run_summary_completed)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.run_phone_checking)).assertDoesNotExist()
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
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(PHONE_LOST_REMAINING_MS))
        val lefts = recordIdleMarkLefts()

        // then
        assertTrue(composeRule.activity.isFinishing)
        assertTrue(lefts.all { it == null })
        composeRule.onNodeWithText(text(R.string.run_phone_checking)).assertDoesNotExist()
    }

    @Test
    fun `폰이 업데이트를 요구하면 업데이트 화면을 보인다`() {
        // when
        rejectSync()

        // then
        composeRule.onNodeWithText(text(R.string.update_required)).assertExists()
        composeRule.onNodeWithContentDescription(idleMark).assertDoesNotExist()
    }

    @Test
    fun `러닝 중 업데이트를 요구받으면 러닝 화면 대신 업데이트 화면을 보인다`() {
        // given
        answerSync(soloRun)

        // when
        rejectSync()

        // then
        composeRule.onNodeWithText(text(R.string.update_required)).assertExists()
        composeRule.onNodeWithText(formatElapsedTime(soloRun.me.elapsedSeconds)).assertDoesNotExist()
    }

    @Test
    fun `업데이트 화면을 쓸면 앱을 닫는다`() {
        // given
        rejectSync()

        // when
        composeRule.onRoot().performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        // then
        assertTrue(composeRule.activity.isFinishing)
    }

    @Test
    fun `업데이트를 누르면 워치 스토어의 앱 상세를 연다`() {
        // given
        rejectSync()

        // when
        composeRule.onNodeWithText(text(R.string.update_open_store)).performClick()

        // then
        val opened: Intent = shadowOf(composeRule.activity).nextStartedActivity
        assertEquals("market://details?id=${composeRule.activity.packageName}", opened.dataString)
    }

    @Test
    fun `스토어 앱이 없으면 업데이트를 눌러도 스토어 열기 실패를 보인다`() {
        // given
        shadowOf(composeRule.activity.application).checkActivities(true)
        rejectSync()

        // when
        composeRule.onNodeWithText(text(R.string.update_open_store)).performClick()

        // then
        composeRule.onNodeWithText(text(R.string.update_open_store_failed)).assertExists()
    }

    @Test
    fun `앱을 열면 폰에 SYNC와 화면 켜짐을 보낸다`() {
        // when
        composeRule.waitForIdle()

        // then
        assertTrue(app.sent.any { it is WatchSync })
        assertEquals(WatchVisibilityUpdate(isVisible = true), app.sent.last { it is WatchVisibilityUpdate })
    }

    @Test
    fun `화면이 안 보이면 폰에 화면 꺼짐을 보낸다`() {
        // given
        composeRule.waitForIdle()

        // when
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.waitForIdle()

        // then
        assertEquals(WatchVisibilityUpdate(isVisible = false), app.sent.last { it is WatchVisibilityUpdate })
    }

    @Test
    fun `폰이 보낸 파티원 사진을 러닝 첫 화면에 보인다`() {
        // when
        answerSync(partyRun, photos = listOf(PlayerPhoto(userId = "u1", jpegBase64 = jpegBase64())))

        // then
        composeRule.onNodeWithContentDescription("완두콩").assertExists()
    }

    @Test
    fun `심박이 들어오면 러닝 화면에 보인다`() {
        // given
        answerSync(soloRun)
        startExerciseSession()

        // when
        composeRule.runOnUiThread { app.heartRateTracker.deliver(150) }
        composeRule.waitForIdle()

        // then
        composeRule.onNodeWithText("150").assertExists()
    }

    @Test
    fun `심박을 못 재면 러닝 화면에 모름 표시를 보인다`() {
        // given
        answerSync(soloRun)
        startExerciseSession()

        // when
        composeRule.runOnUiThread { app.heartRateTracker.deliver(null) }
        composeRule.waitForIdle()

        // then
        composeRule.onNodeWithText(text(R.string.run_value_unknown)).assertExists()
    }

    @Test
    fun `러닝이 없다고 들으면 남은 운동을 끈다`() {
        // when
        answerSync(run = null)

        // then
        assertEquals(listOf("stop"), app.heartRateTracker.calls)
    }

    @Test
    fun `러닝이 끝나면 운동을 끄고 결과 화면을 보인다`() {
        // given
        answerSync(soloRun)
        startExerciseSession()

        // when
        receiveState(soloRun.copy(status = RunStatus.FINISHED, result = RunPreviewSamples.result))
        composeRule.waitForIdle()

        // then
        assertEquals("stop", app.heartRateTracker.calls.last())
        composeRule.onNodeWithText(text(R.string.run_summary_completed)).assertExists()
    }

    @Test
    fun `대기 화면이 보이면 권한을 묻는다`() {
        // when
        composeRule.waitForIdle()

        // then
        assertNotNull(shadowOf(composeRule.activity).lastRequestedPermission)
    }

    @Test
    fun `대기 화면에서 권한 창이 닫히면 운동 서비스를 시작하지 않는다`() {
        // given
        answerSync(run = null)

        // when
        closePermissionRequest()

        // then
        assertNull(shadowOf(app).nextStartedService)
    }

    @Test
    fun `첫 러닝엔 권한부터 묻고 서비스를 아직 시작하지 않는다`() {
        // when
        answerSync(soloRun)

        // then
        assertNotNull(shadowOf(composeRule.activity).lastRequestedPermission)
        assertNull(shadowOf(app).nextStartedService)
    }

    @Test
    fun `권한을 한 번 물으면 물은 것을 기억한다`() {
        // when
        answerSync(soloRun)

        // then
        val preferences = composeRule.activity.getSharedPreferences("exercise", Context.MODE_PRIVATE)
        assertTrue(preferences.getBoolean("permissions_asked", false))
    }

    @Test
    fun `권한을 물은 적 있으면 러닝이 보일 때 운동 서비스를 시작한다`() {
        // given
        closePermissionRequest()

        // when
        answerSync(soloRun)

        // then
        assertExerciseServiceStarted()
    }

    @Test
    fun `러닝이 없으면 운동 서비스를 시작하지 않는다`() {
        // given
        closePermissionRequest()

        // when
        answerSync(run = null)

        // then
        assertNull(shadowOf(app).nextStartedService)
    }

    @Test
    fun `화면이 다시 보이면 운동 서비스를 다시 시작한다`() {
        // given
        closePermissionRequest()
        answerSync(soloRun)
        assertExerciseServiceStarted()

        // when
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()

        // then
        assertExerciseServiceStarted()
    }

    @Test
    fun `권한 창이 닫히면 거부해도 운동 서비스를 시작한다`() {
        // given
        answerSync(soloRun)
        assertNull(shadowOf(app).nextStartedService)

        // when
        closePermissionRequest()

        // then
        assertExerciseServiceStarted()
    }

    @Test
    fun `폰 답이 끊겼다가 돌아오면 운동 서비스를 다시 시작한다`() {
        // given
        closePermissionRequest()
        answerSync(soloRun)
        assertExerciseServiceStarted()
        idleFor(REPLY_TIMEOUT_PASSED_MS)

        // when
        answerSync(soloRun)

        // then
        assertExerciseServiceStarted()
    }

    @Test
    fun `조작 장에서 콤보 효과를 끄면 설정에 저장한다`() {
        // given
        answerSync(soloRun)
        composeRule.onRoot().performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        // when
        composeRule.onNodeWithText(text(R.string.controls_combo_effect)).performClick()
        composeRule.waitForIdle()

        // then
        assertFalse(app.comboEffectSetting.isOn.value)
    }

    @Test
    fun `콤보 눈금이 오면 큰 순간을 띄우고 다 그린 뒤 걷는다`() {
        // given
        answerSync(partyRun)
        composeRule.mainClock.autoAdvance = false

        // when
        receiveMilestone("e1", "u2", 10)

        // then
        composeRule.onNodeWithContentDescription("10 COMBO 콩나물").assertExists()
        composeRule.mainClock.advanceTimeBy(BURST_PASSED_MS)
        composeRule.onNodeWithContentDescription("10 COMBO 콩나물").assertDoesNotExist()
    }

    @Test
    fun `콤보 효과를 끄면 눈금이 와도 큰 순간을 띄우지 않는다`() {
        // given
        answerSync(partyRun)
        composeRule.runOnUiThread { app.comboEffectSetting.set(false) }
        composeRule.mainClock.autoAdvance = false

        // when
        receiveMilestone("e1", "u2", 10)

        // then
        composeRule.onNodeWithContentDescription("10 COMBO 콩나물").assertDoesNotExist()
    }

    @Test
    fun `명단에 없는 사람의 눈금은 큰 순간을 띄우지 않는다`() {
        // given
        answerSync(partyRun)
        composeRule.mainClock.autoAdvance = false

        // when
        receiveMilestone("e1", "u9", 10)

        // then
        composeRule.onNodeWithContentDescription("10 COMBO", substring = true).assertDoesNotExist()
    }

    @Test
    fun `큰 순간 중에 온 다음 눈금은 띄우지 않는다`() {
        // given
        answerSync(partyRun)
        composeRule.mainClock.autoAdvance = false
        receiveMilestone("e1", "u2", 10)

        // when
        receiveMilestone("e2", "u1", 20)

        // then
        composeRule.onNodeWithContentDescription("10 COMBO 콩나물").assertExists()
        composeRule.onNodeWithContentDescription("20 COMBO 완두콩").assertDoesNotExist()
    }

    @Test
    fun `화면이 꺼진 사이 온 눈금은 다시 켜져도 띄우지 않는다`() {
        // given
        answerSync(partyRun)
        composeRule.mainClock.autoAdvance = false
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)

        // when
        receiveMilestone("e1", "u2", 10)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.mainClock.advanceTimeByFrame()

        // then
        composeRule.onNodeWithContentDescription("10 COMBO 콩나물").assertDoesNotExist()
    }

    @Test
    fun `큰 순간이 덮은 동안 쓸어도 장이 넘어가지 않는다`() {
        // given
        answerSync(partyRun)
        composeRule.mainClock.autoAdvance = false
        receiveMilestone("e1", "u2", 10)

        // when
        composeRule.onRoot().performTouchInput { swipeRight() }
        composeRule.mainClock.advanceTimeBy(BURST_PASSED_MS)

        // then
        composeRule.onNodeWithText(text(R.string.controls_pause)).assertIsNotDisplayed()
    }

    @Test
    fun `조작 장에서는 눈금이 와도 큰 순간을 띄우지 않는다`() {
        // given
        answerSync(partyRun)
        composeRule.onRoot().performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false

        // when
        receiveMilestone("e1", "u2", 10)

        // then
        composeRule.onNodeWithContentDescription("10 COMBO 콩나물").assertDoesNotExist()
    }

    @Test
    fun `조작 장에서 돌아오면 눈금이 올 때 큰 순간을 띄운다`() {
        // given
        answerSync(partyRun)
        composeRule.onRoot().performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false

        // when
        receiveMilestone("e1", "u2", 10)

        // then
        composeRule.onNodeWithContentDescription("10 COMBO 콩나물").assertExists()
    }

    @Test
    fun `명단에 없는 사람의 눈금 뒤에 온 눈금은 띄운다`() {
        // given
        answerSync(partyRun)
        composeRule.mainClock.autoAdvance = false
        receiveMilestone("e1", "u9", 10)

        // when
        receiveMilestone("e2", "u2", 20)

        // then
        composeRule.onNodeWithContentDescription("20 COMBO 콩나물").assertExists()
    }

    @Test
    fun `큰 순간 중에 화면이 꺼지면 다시 켜져도 이어 그리지 않는다`() {
        // given
        answerSync(partyRun)
        composeRule.mainClock.autoAdvance = false
        receiveMilestone("e1", "u2", 10)
        composeRule.onNodeWithContentDescription("10 COMBO 콩나물").assertExists()

        // when
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.mainClock.advanceTimeByFrame()

        // then
        composeRule.onNodeWithContentDescription("10 COMBO 콩나물").assertDoesNotExist()
    }

    // 받은 콤보는 다음 차례에 상태로 들어가 그 다음 프레임에 그려진다
    private fun receiveMilestone(eventId: String, userId: String, comboCount: Int) {
        receive(WatchComboTriggered(eventId, userId, comboCount, ComboKind.MILESTONE))
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun startExerciseSession() {
        composeRule.runOnUiThread { app.exerciseSession.start(onEnded = {}) }
        composeRule.waitForIdle()
    }

    private fun closePermissionRequest() {
        composeRule.waitForIdle()
        val request = shadowOf(composeRule.activity).lastRequestedPermission
        val denied = IntArray(request.requestedPermissions.size) { PackageManager.PERMISSION_DENIED }
        val result = Intent()
            .putExtra(RequestMultiplePermissions.EXTRA_PERMISSIONS, request.requestedPermissions)
            .putExtra(RequestMultiplePermissions.EXTRA_PERMISSION_GRANT_RESULTS, denied)
        composeRule.runOnUiThread {
            composeRule.activity.activityResultRegistry.dispatchResult(request.requestCode, Activity.RESULT_OK, result)
        }
        composeRule.waitForIdle()
    }

    private fun assertExerciseServiceStarted() {
        val started: Intent = shadowOf(app).nextStartedService
        assertEquals(ExerciseService::class.java.name, started.component?.className)
    }

    private fun showFinishedRun() {
        answerSync(soloRun.copy(status = RunStatus.FINISHED, result = RunPreviewSamples.result))
        composeRule.onNodeWithText(text(R.string.run_summary_completed)).assertExists()
    }

    private fun assertClosedWithoutIdleScreen() {
        assertTrue(composeRule.activity.isFinishing)
        assertNull(app.phoneLink.state.value.finishedRun)
        composeRule.onNodeWithContentDescription(idleMark).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.run_summary_completed)).assertDoesNotExist()
    }

    private fun text(id: Int) = composeRule.activity.getString(id)

    private fun recordIdleMarkLefts(): List<Float?> = List(SWIPE_SETTLE_FRAME_COUNT) {
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onAllNodesWithContentDescription(idleMark).fetchSemanticsNodes().firstOrNull()?.boundsInRoot?.left
    }

    private fun idleFor(ms: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
        composeRule.waitForIdle()
    }

    private fun answerSync(run: RunState?, photos: List<PlayerPhoto> = emptyList()) {
        composeRule.waitForIdle()
        val syncId = app.sent.filterIsInstance<WatchSync>().last().syncId
        receive(WatchSynced(syncId, phoneState(run), birthday = null, photos = photos))
        composeRule.waitForIdle()
    }

    private fun rejectSync() {
        composeRule.waitForIdle()
        val syncId = app.sent.filterIsInstance<WatchSync>().last().syncId
        receive(PhoneError("WATCH_UPDATE_REQUIRED", "워치 앱 업데이트 필요", "WATCH_SYNC", syncId))
        composeRule.waitForIdle()
    }

    private fun jpegBase64(): String {
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val jpeg = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
        return Base64.encodeToString(jpeg, Base64.NO_WRAP)
    }

    // SYNC 중엔 상태가 조용히 버려진다
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

