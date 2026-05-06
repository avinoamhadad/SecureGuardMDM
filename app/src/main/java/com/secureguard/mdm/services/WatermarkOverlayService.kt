package com.secureguard.mdm.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.secureguard.mdm.R
import com.secureguard.mdm.data.local.PreferencesManager
import com.secureguard.mdm.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that draws a small watermark in the top-right corner of the screen
 * while the device launcher (home screen / app drawer) is in the foreground.
 *
 * Uses TYPE_APPLICATION_OVERLAY, so SYSTEM_ALERT_WINDOW must be granted before
 * the service is started.
 */
@AndroidEntryPoint
class WatermarkOverlayService : Service() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())

    private var windowManager: WindowManager? = null
    private var overlayView: ImageView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var launcherPackages: Set<String> = emptySet()
    private var isOverlayAttached = false

    private val pollRunnable = object : Runnable {
        override fun run() {
            try {
                updateOverlayVisibility()
            } catch (t: Throwable) {
                Log.w(TAG, "Poll tick failed", t)
            } finally {
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        launcherPackages = resolveLauncherPackages()
        Log.d(TAG, "Service created. Launcher packages: $launcherPackages")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_REFRESH -> applySettings()
            else -> applySettings()
        }
        handler.removeCallbacks(pollRunnable)
        handler.post(pollRunnable)
        return START_STICKY
    }

    private fun applySettings() {
        scope.launch {
            val enabled = settingsRepository.isWatermarkEnabled()
            if (!enabled) {
                stopSelf()
                return@launch
            }
            val alphaPercent = settingsRepository.getWatermarkAlphaPercent()
            val variant = settingsRepository.getWatermarkVariant()
            ensureOverlayCreated(alphaPercent, variant)
        }
    }

    private fun drawableForVariant(variant: String): Int = when (variant) {
        PreferencesManager.WATERMARK_VARIANT_BLOCKED -> R.drawable.ic_watermark_blocked
        else -> R.drawable.ic_watermark_filtered
    }

    private fun ensureOverlayCreated(alphaPercent: Int, variant: String) {
        if (overlayView != null) {
            overlayView?.alpha = alphaPercent / 100f
            overlayView?.setImageResource(drawableForVariant(variant))
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "SYSTEM_ALERT_WINDOW not granted, stopping service.")
            stopSelf()
            return
        }
        val sizePx = (resources.displayMetrics.density * SIZE_DP).toInt()
        val marginPx = (resources.displayMetrics.density * MARGIN_DP).toInt()

        overlayView = ImageView(this).apply {
            setImageResource(drawableForVariant(variant))
            alpha = alphaPercent / 100f
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = marginPx
            y = marginPx
        }
    }

    private fun updateOverlayVisibility() {
        val view = overlayView ?: return
        val params = layoutParams ?: return
        val wm = windowManager ?: return

        val shouldShow = isLauncherForeground()
        if (shouldShow && !isOverlayAttached) {
            try {
                wm.addView(view, params)
                isOverlayAttached = true
            } catch (e: Exception) {
                Log.w(TAG, "addView failed", e)
            }
        } else if (!shouldShow && isOverlayAttached) {
            try {
                wm.removeView(view)
                isOverlayAttached = false
            } catch (e: Exception) {
                Log.w(TAG, "removeView failed", e)
            }
        }
    }

    private fun isLauncherForeground(): Boolean {
        if (launcherPackages.isEmpty()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) return false
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return false
        val now = System.currentTimeMillis()
        val events = try {
            usm.queryEvents(now - LOOKBACK_MS, now)
        } catch (e: Exception) {
            Log.w(TAG, "queryEvents failed", e)
            return false
        }
        var lastFgPackage: String? = null
        val tmp = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(tmp)
            val type = tmp.eventType
            val isMoveToFg = type == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    type == UsageEvents.Event.ACTIVITY_RESUMED)
            if (isMoveToFg) {
                lastFgPackage = tmp.packageName
            }
        }
        return lastFgPackage != null && launcherPackages.contains(lastFgPackage)
    }

    private fun resolveLauncherPackages(): Set<String> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfos: List<ResolveInfo> =
            pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfos.mapNotNull { it.activityInfo?.packageName }
            .filterNot { it == packageName }
            .toSet()
    }

    private fun buildNotification(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.watermark_notification_channel_name),
                NotificationManager.IMPORTANCE_MIN
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.watermark_notification_text))
            .setSmallIcon(R.drawable.ic_watermark_filtered)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollRunnable)
        if (isOverlayAttached) {
            try {
                windowManager?.removeView(overlayView)
            } catch (_: Exception) { }
            isOverlayAttached = false
        }
        overlayView = null
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "WatermarkOverlay"
        private const val NOTIFICATION_ID = 4242
        private const val CHANNEL_ID = "WatermarkOverlayChannel"
        private const val POLL_INTERVAL_MS = 700L
        private const val LOOKBACK_MS = 5_000L
        private const val SIZE_DP = 28
        private const val MARGIN_DP = 6

        const val ACTION_REFRESH = "com.secureguard.mdm.action.REFRESH_WATERMARK"

        fun start(context: Context) {
            val intent = Intent(context, WatermarkOverlayService::class.java).apply {
                action = ACTION_REFRESH
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WatermarkOverlayService::class.java))
        }

        fun hasOverlayPermission(context: Context): Boolean {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                Settings.canDrawOverlays(context)
        }

        fun hasUsageStatsPermission(context: Context): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE)
                as? android.app.AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            return mode == android.app.AppOpsManager.MODE_ALLOWED
        }
    }
}
