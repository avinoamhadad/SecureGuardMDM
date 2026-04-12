package com.secureguard.mdm.firewall

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.preference.PreferenceManager
import com.secureguard.mdm.utils.AppLogger

class AppFirewallVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    
    companion object {
        const val PREF_FIREWALL_BLOCKED_APPS = "firewall_blocked_apps"
        const val ACTION_STOP_FIREWALL = "com.secureguard.mdm.firewall.STOP"
        private const val NOTIFICATION_CHANNEL_ID = "app_firewall_channel"
        private const val NOTIFICATION_ID = 2002
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // We must call startForeground immediately to satisfy the contract of startForegroundService
        createNotificationChannel()
        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("App Firewall Active")
            .setContentText("Blocking internet for specific apps.")
            .setSmallIcon(android.R.drawable.ic_secure)
            .build()
            
        startForeground(NOTIFICATION_ID, notification)

        if (intent?.action == ACTION_STOP_FIREWALL) {
            stopVpn()
            return START_NOT_STICKY
        }
        
        setupVpnInterface()
        return START_STICKY
    }

    private fun startVpn() {
        // startForeground moved to onStartCommand for API contract safety
        setupVpnInterface()
    }

    private fun setupVpnInterface() {
        vpnInterface?.close()
        
        val builder = Builder()
        
        // This is a Black-Hole VPN. We set a dummy local address and don't route any real traffic out.
        // Apps routed here simply lose internet connectivity.
        builder.addAddress("10.10.10.1", 24)
        try {
            builder.addRoute("0.0.0.0", 0) // IPv4
            builder.addRoute("::", 0) // IPv6
        } catch (e: Exception) {
            AppLogger.e("AppFirewall", "Failed to add route", e)
        }
        
        // Android requires a session name
        builder.setSession("App Firewall")
        
        // Only apps inside PREF_FIREWALL_BLOCKED_APPS are routed into this black hole.
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val blockedApps = sharedPrefs.getStringSet(PREF_FIREWALL_BLOCKED_APPS, emptySet()) ?: emptySet()
        
        var appsAdded = false
        for (pkg in blockedApps) {
            try {
                builder.addAllowedApplication(pkg)
                appsAdded = true
            } catch (e: Exception) {
                AppLogger.e("AppFirewall", "Could not add allowed application: $pkg", e)
            }
        }
        
        // If no apps are blocked, we don't necessarily need to be running, but we maintain state.
        if (appsAdded) {
            vpnInterface = builder.establish()
        }
    }

    private fun stopVpn() {
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "App Firewall Status",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
