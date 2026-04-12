package com.secureguard.mdm.boot.impl

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.secureguard.mdm.R
import com.secureguard.mdm.boot.api.BootTask
import com.secureguard.mdm.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ShowToastOnBootTask @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : BootTask {

    companion object {
        private const val BOOT_NOTIFICATION_CHANNEL_ID = "BootNotificationChannel"
        private const val BOOT_NOTIFICATION_ID = 2 // Use a different ID from the main service
    }

    override suspend fun onBootCompleted() {
        if (settingsRepository.isShowBootToastEnabled()) {
            showBootNotification()
            println(context.getString(R.string.boot_notification_message))
        }
    }

    private fun showBootNotification() {

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a notification channel for Android 8.0 (API 26) and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BOOT_NOTIFICATION_CHANNEL_ID,
                "Boot Notifications", // Channel name visible to the user in settings
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows a notification when the app starts on boot."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, BOOT_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_safe_boot_blocked) // Using an existing icon
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.boot_notification_message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // Notification will be dismissed when the user taps it
            .build()

        notificationManager.notify(BOOT_NOTIFICATION_ID, notification)
    }
}