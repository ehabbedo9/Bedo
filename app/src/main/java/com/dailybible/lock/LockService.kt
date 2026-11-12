package com.dailybible.lock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Runs in the background, watches which app is in front, and covers a locked app
 * with a full-screen overlay until today's reading is complete.
 */
class LockService : Service() {

    companion object {
        private const val CHANNEL_ID = "lock_status"
        private const val NOTIFICATION_ID = 1
        private const val INTERVAL_MS = 500L
        private const val IDLE_INTERVAL_MS = 2000L

        // Diagnostics shown at the bottom of the home screen.
        @Volatile var lastTick = 0L
        @Volatile var lastOtherPackage: String? = null
        @Volatile var lastError: String? = null

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, LockService::class.java))
            } catch (e: Exception) {
                // The system refused a background start; the service starts next time the app opens.
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var windowManager: WindowManager
    private lateinit var usageStats: UsageStatsManager
    private lateinit var powerManager: PowerManager
    private var overlay: View? = null
    private var foregroundPackage: String? = null
    private var lastQuery = 0L

    private val ticker = object : Runnable {
        override fun run() {
            var delay = INTERVAL_MS
            lastTick = System.currentTimeMillis()
            try {
                if (powerManager.isInteractive) check() else delay = IDLE_INTERVAL_MS
            } catch (e: Exception) {
                // Keep the loop alive whatever happens in one pass.
                lastError = e.toString()
            }
            handler.postDelayed(this, delay)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        usageStats = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        lastQuery = System.currentTimeMillis() - 60L * 60L * 1000L
        startAsForeground()
        handler.post(ticker)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        hideOverlay()
        super.onDestroy()
    }

    private fun startAsForeground() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, "حالة القفل", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)

        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_lock)
            .setContentTitle("قفل التطبيقات يعمل")
            .setContentText("تُفتح التطبيقات بعد إكمال قراءة اليوم")
            .setOngoing(true)
            .setContentIntent(open)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            lastError = e.toString()
            stopSelf()
        }
    }

    /** One pass: find the app in front and show or hide the overlay. */
    private fun check() {
        if (!Prefs.isSetupDone(this) || !Permissions.hasUsageAccess(this)) {
            hideOverlay()
            return
        }
        updateForegroundPackage()
        val pkg = foregroundPackage
        val app = if (pkg == null) null else LockedApps.find(this, pkg)
        if (app != null && !Prefs.isComplete(this) && Permissions.hasOverlay(this)) {
            showOverlay(app)
        } else {
            hideOverlay()
        }
    }

    private fun updateForegroundPackage() {
        val now = System.currentTimeMillis()
        val events = usageStats.queryEvents(lastQuery, now) ?: return
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            // ACTIVITY_RESUMED has the same value (1) as the older MOVE_TO_FOREGROUND.
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                foregroundPackage = event.packageName
                if (event.packageName != packageName) lastOtherPackage = event.packageName
            }
        }
        lastQuery = now
    }

    private fun showOverlay(app: LockedApps.App) {
        var view = overlay
        if (view == null) {
            val themed = ContextThemeWrapper(this, R.style.Theme_DailyBible)
            view = LayoutInflater.from(themed).inflate(R.layout.overlay_lock, null)
            view.findViewById<View>(R.id.lockContinue).setOnClickListener { openReader() }
            view.findViewById<View>(R.id.lockExit).setOnClickListener { goHome() }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.OPAQUE
            )
            params.gravity = Gravity.TOP or Gravity.START
            windowManager.addView(view, params)
            overlay = view
        }
        val target = Prefs.target(this)
        val done = Prefs.doneToday(this).coerceAtMost(target)
        view!!.findViewById<TextView>(R.id.lockedAppName).text = app.label + " مقفول"
        view.findViewById<TextView>(R.id.lockProgressText).text = Ar.num(done) + " من " + Ar.num(target)
        val bar = view.findViewById<ProgressBar>(R.id.lockProgressBar)
        bar.max = target
        bar.progress = done
        view.findViewById<TextView>(R.id.lockNext).text = "التالي: " + Bible.title(Prefs.pointer(this))
    }

    private fun hideOverlay() {
        val view = overlay ?: return
        try {
            windowManager.removeView(view)
        } catch (e: Exception) {
            // Already removed.
        }
        overlay = null
    }

    private fun openReader() {
        foregroundPackage = packageName
        hideOverlay()
        val intent = Intent(this, ReaderActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    private fun goHome() {
        foregroundPackage = null
        hideOverlay()
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
