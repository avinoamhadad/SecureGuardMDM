package com.secureguard.mdm.vpn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.preference.PreferenceManager
import androidx.activity.result.ActivityResultLauncher
import com.emanuelef.remote_capture.CaptureService
import com.emanuelef.remote_capture.model.CaptureSettings
import com.emanuelef.remote_capture.model.Prefs

object VpnController {

    /**
     * Prepares the VPN and launches the permission request if need be.
     */
    fun prepareVpn(context: Context, launcher: ActivityResultLauncher<Intent>?) {
        val intent = VpnService.prepare(context)
        if (intent != null) {
            launcher?.launch(intent)
        } else {
            // Already prepared, start it directly
            startVpn(context)
        }
    }

    /**
     * Initializes PCAPdroid settings and starts CaptureService
     */
    fun startVpn(context: Context) {
        if (isVpnRunning()) return

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        
        // Ensure MDM app is always excluded to avoid loops/blocking
        val packageName = context.packageName
        val exceptions = sharedPrefs.getStringSet(Prefs.PREF_VPN_EXCEPTIONS, mutableSetOf<String>())?.toMutableSet() ?: mutableSetOf()
        if (!exceptions.contains(packageName)) {
            exceptions.add(packageName)
            sharedPrefs.edit().putStringSet(Prefs.PREF_VPN_EXCEPTIONS, exceptions).apply()
        }

        val settings = CaptureSettings(context, sharedPrefs)

        // Configure minimal required settings
        settings.dump_mode = Prefs.DumpMode.NONE
        settings.app_filter.clear() // Apply to all apps by default

        // Start the native capture service
        val intent = Intent(context, CaptureService::class.java)
        intent.putExtra("settings", settings)
        context.startForegroundService(intent)
    }

    /**
     * Restarts the VPN service
     */
    fun restartVpn(context: Context) {
        stopVpn()
        // Wait a small bit for service to clean up
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            startVpn(context)
        }, 500)
    }

    /**
     * Stops the CaptureService
     */
    fun stopVpn() {
        CaptureService.stopService()
    }

    /**
     * Checks if the CaptureService is currently active
     */
    fun isVpnRunning(): Boolean {
        return CaptureService.isServiceActive()
    }

    /**
     * Checks if an app is in the VPN exceptions list
     */
    fun isAppExcluded(context: Context, packageName: String): Boolean {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val exceptions = sharedPrefs.getStringSet(Prefs.PREF_VPN_EXCEPTIONS, null)
        return exceptions?.contains(packageName) == true
    }
}
