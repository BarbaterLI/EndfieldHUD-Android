package com.endfieldhud.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HudService : android.app.Service() {

    companion object {
        const val ACTION_START = "com.endfieldhud.android.action.START"
        const val ACTION_TOGGLE = "com.endfieldhud.android.action.TOGGLE"
        const val ACTION_STOP = "com.endfieldhud.android.action.STOP"
        const val ACTION_TIMER_START = "com.endfieldhud.android.action.TIMER_START"
        const val ACTION_TIMER_CANCEL = "com.endfieldhud.android.action.TIMER_CANCEL"
        const val ACTION_STOPWATCH_TOGGLE = "com.endfieldhud.android.action.STOPWATCH_TOGGLE"
        const val ACTION_STOPWATCH_RESET = "com.endfieldhud.android.action.STOPWATCH_RESET"
        const val ACTION_STOPWATCH_LAP = "com.endfieldhud.android.action.STOPWATCH_LAP"
        const val EXTRA_MS = "ms"

        private const val CHANNEL_SERVICE = "hud_service"
        private const val CHANNEL_ALERT = "hud_alert"
        private const val NOTIF_ID = 101
        private const val ALERT_NOTIF_ID = 102

        fun start(context: Context) {
            val intent = Intent(context, HudService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, HudService::class.java).setAction(ACTION_STOP))
        }

        fun toggle(context: Context) {
            if (HudState.serviceRunning.value) {
                context.startService(Intent(context, HudService::class.java).setAction(ACTION_TOGGLE))
            } else {
                start(context)
            }
        }

        fun send(context: Context, action: String, extraMs: Long = 0L) {
            val intent = Intent(context, HudService::class.java).setAction(action)
            if (extraMs > 0) intent.putExtra(EXTRA_MS, extraMs)
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var overlay: HudOverlay? = null
    private var clipboardMgr: ClipboardManager? = null
    private var vibrator: Vibrator? = null

    private var timerJob: Job? = null
    private var stopwatchJob: Job? = null
    private var stopwatchStartAt = 0L
    private var stopwatchAccum = 0L

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        val cm = clipboardMgr ?: return@OnPrimaryClipChangedListener
        val clip = cm.primaryClip ?: return@OnPrimaryClipChangedListener
        if (clip.itemCount > 0) {
            val text = clip.getItemAt(0).coerceToText(this)?.toString() ?: return@OnPrimaryClipChangedListener
            if (text.isNotBlank() && text != HudState.lastCapturedClip) {
                HudState.lastCapturedClip = text
                HudState.captureClip(text)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        HudState.init(this)
        createChannels()
        vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val notif = buildServiceNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }

        clipboardMgr = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardMgr?.addPrimaryClipChangedListener(clipListener)

        val ov = HudOverlay(this, serviceScope)
        overlay = ov
        ov.attach()

        HudState.serviceRunning.value = true
        HudState.collapsed.value = false
        refreshTile()
        HudState.logEvent("HUD 服务上线", LogEvent.OK)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> {
                val collapsed = !HudState.collapsed.value
                HudState.collapsed.value = collapsed
                refreshTile()
                updateNotification()
                HudState.logEvent(if (collapsed) "HUD 收起为气泡" else "HUD 展开")
            }
            ACTION_STOP -> {
                HudState.logEvent("HUD 服务下线", LogEvent.WARN)
                stopSelf()
            }
            ACTION_TIMER_START -> {
                val ms = intent.getLongExtra(EXTRA_MS, 60_000L)
                startTimer(ms)
            }
            ACTION_TIMER_CANCEL -> cancelTimer()
            ACTION_STOPWATCH_TOGGLE -> toggleStopwatch()
            ACTION_STOPWATCH_RESET -> resetStopwatch()
            ACTION_STOPWATCH_LAP -> lapStopwatch()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null

    override fun onDestroy() {
        // mirrors the desktop app: clipboard history is wiped when HUD exits
        HudState.clearClips()
        overlay?.destroy()
        overlay = null
        clipboardMgr?.removePrimaryClipChangedListener(clipListener)
        timerJob?.cancel()
        stopwatchJob?.cancel()
        serviceScope.cancel()
        HudState.serviceRunning.value = false
        refreshTile()
        super.onDestroy()
    }

    // ---------- timer ----------

    private fun startTimer(totalMs: Long) {
        timerJob?.cancel()
        val endAt = System.currentTimeMillis() + totalMs
        HudState.timerTotalMs.value = totalMs
        HudState.timerRemainingMs.value = totalMs
        HudState.timerRunning.value = true
        timerJob = serviceScope.launch {
            while (isActive) {
                val rem = endAt - System.currentTimeMillis()
                if (rem <= 0) break
                HudState.timerRemainingMs.value = rem
                delay(200)
            }
            if (HudState.timerRunning.value) {
                HudState.timerRemainingMs.value = 0
                HudState.timerRunning.value = false
                onTimerFinished()
            }
        }
        HudState.logEvent("计时器启动 ${totalMs / 60000} 分钟")
    }

    private fun cancelTimer() {
        timerJob?.cancel()
        HudState.timerRemainingMs.value = 0
        HudState.timerRunning.value = false
        HudState.logEvent("计时器已取消", LogEvent.WARN)
    }

    private fun onTimerFinished() {
        HudState.logEvent("计时结束", LogEvent.OK)
        if (HudState.settings.value.vibrate) {
            vibrate()
        }
        val notif = NotificationCompat.Builder(this, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_stat_hud)
            .setContentTitle("EndfieldHUD")
            .setContentText("计时结束 — 管理员，任务时间到")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(ALERT_NOTIF_ID, notif)
    }

    private fun vibrate() {
        try {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
        } catch (_: Exception) {
        }
    }

    // ---------- stopwatch ----------

    private fun toggleStopwatch() {
        if (HudState.stopwatchRunning.value) {
            // pause
            stopwatchJob?.cancel()
            stopwatchAccum += System.currentTimeMillis() - stopwatchStartAt
            HudState.stopwatchMs.value = stopwatchAccum
            HudState.stopwatchRunning.value = false
        } else {
            stopwatchStartAt = System.currentTimeMillis()
            HudState.stopwatchRunning.value = true
            stopwatchJob = serviceScope.launch {
                while (isActive) {
                    HudState.stopwatchMs.value = stopwatchAccum + (System.currentTimeMillis() - stopwatchStartAt)
                    delay(100)
                }
            }
        }
    }

    private fun resetStopwatch() {
        stopwatchJob?.cancel()
        stopwatchAccum = 0
        stopwatchStartAt = 0
        HudState.stopwatchMs.value = 0
        HudState.stopwatchRunning.value = false
        HudState.stopwatchLaps.value = emptyList()
    }

    private fun lapStopwatch() {
        HudState.stopwatchLaps.value = (HudState.stopwatchLaps.value + HudState.stopwatchMs.value).takeLast(20)
    }

    // ---------- clipboard helper used by the HUD UI ----------

    fun copyClipBack(text: String) {
        val cm = clipboardMgr ?: return
        HudState.lastCapturedClip = text
        cm.setPrimaryClip(ClipData.newPlainText("endfield", text))
        HudState.logEvent("已回拷 ${text.length} 字", LogEvent.OK)
    }

    // ---------- notifications ----------

    private fun createChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SERVICE, getString(R.string.notif_channel_hud), NotificationManager.IMPORTANCE_LOW).apply {
                description = getString(R.string.notif_channel_hud_desc)
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERT, getString(R.string.notif_channel_alert), NotificationManager.IMPORTANCE_HIGH).apply {
                description = getString(R.string.notif_channel_alert_desc)
            }
        )
    }

    private fun buildServiceNotification(): Notification {
        val openPi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val togglePi = PendingIntent.getService(
            this, 1, Intent(this, HudService::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopPi = PendingIntent.getService(
            this, 2, Intent(this, HudService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_hud)
            .setContentTitle("EndfieldHUD")
            .setContentText("HUD 服务运行中 · 点击打开主界面")
            .setOngoing(true)
            .setContentIntent(openPi)
            .addAction(0, "切换 HUD", togglePi)
            .addAction(0, "退出", stopPi)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildServiceNotification())
    }

    private fun refreshTile() {
        TileService.requestListeningState(
            this,
            ComponentName(this, HudTileService::class.java)
        )
    }
}
