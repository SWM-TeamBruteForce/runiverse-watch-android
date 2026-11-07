package com.runiverse.app.exercise

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.runiverse.app.MainActivity
import com.runiverse.app.R
import com.runiverse.app.RuniverseApp

/** 러닝 중 손목을 내려도 앱이 폰 메시지를 받고 심박을 재도록 앱을 살려 둔다. */
class ExerciseService : Service() {

    private var isForeground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isForeground) {
            isForeground = true
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), FOREGROUND_TYPE)
        }
        // 운동을 끄는 사이 새 시작 요청이 왔으면 그 요청의 서비스는 내리지 않는다
        (application as RuniverseApp).exerciseSession.start(onEnded = { stopSelf(startId) })
        // 화면이 안 보이면 심박을 쓰는 서비스를 시작할 수 없다 — 앱을 열 때 다시 시작한다
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        (application as RuniverseApp).exerciseSession.stop()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val title = getString(R.string.run_notification_title)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.run_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_running)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, builder)
            .setStaticIcon(R.drawable.ic_running)
            .setTouchIntent(openApp)
            .setStatus(Status.forPart(Status.TextPart(title)))
            .build()
            .apply(applicationContext)
        return builder.build()
    }

    companion object {
        private const val TAG = "ExerciseService"
        private const val CHANNEL_ID = "running"
        private const val NOTIFICATION_ID = 1
        private val FOREGROUND_TYPE =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            } else {
                0
            }

        /** 심박 없이 돌아도 앱은 살려 둔다. 알림 권한은 시계 화면 아이콘에 쓴다. */
        val PERMISSIONS: Array<String> = buildList {
            add(HEART_RATE_PERMISSION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, ExerciseService::class.java))
            } catch (e: IllegalStateException) {
                // 화면이 막 꺼지는 순간이면 거절된다 — 다음에 화면이 켜질 때 다시 시작한다
                Log.w(TAG, "운동 서비스 시작 거절", e)
            }
        }
    }
}
