package com.secureguard.mdm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.secureguard.mdm.MainActivity
import com.secureguard.mdm.utils.AppLogger

/**
 * Receives the system "SECRET_CODE" broadcast when the user dials *#*#1818#*#*
 * (data URI: android_secret_code://1818). This provides a way to open the admin
 * UI even after the launcher icon has been hidden via Settings.
 *
 * Wired in AndroidManifest.xml on both the legacy (pre-O) and new (O+) action names.
 */
class SecretDialReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AppLogger.d(TAG, "Secret dial code received: ${intent.data}")
        val launch = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        try {
            context.startActivity(launch)
        } catch (t: Throwable) {
            AppLogger.w(TAG, "Failed to start MainActivity from secret dial", t)
        }
    }

    companion object {
        private const val TAG = "SecretDialReceiver"
    }
}
