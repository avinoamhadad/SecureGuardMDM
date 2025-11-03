package com.secureguard.mdm.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.secureguard.mdm.R
import com.secureguard.mdm.utils.FileLogger
import java.io.IOException

class BlockerVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val tag = "NetfreeVpnService"
    private lateinit var connectivityManager: ConnectivityManager

    companion object {
        const val ACTION_CONNECT = "com.secureguard.mdm.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.secureguard.mdm.ACTION_DISCONNECT"
        const val ACTION_UPDATE_POLICY = "ACTION_UPDATE_POLICY"
        const val EXTRA_PREFERRED_NETWORK = "EXTRA_PREFERRED_NETWORK"

        // --- מיקום נכון של המשתנים ---
        private const val VPN_NOTIFICATION_CHANNEL_ID = "BlockerVpnChannel"
        private const val VPN_NOTIFICATION_ID = 1002 // מספר שונה מהשירות השני
    }

    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // --- קריאה להפעלת השירות במצב חזית ---
        startForeground(VPN_NOTIFICATION_ID, createNotification())

        when (intent?.action) {
            ACTION_CONNECT -> {
                FileLogger.log(tag, "Received ACTION_CONNECT. Starting VPN in simple block mode.")
                stopVpn()
                startVpn(preferredNetwork = null)
            }
            ACTION_DISCONNECT -> {
                FileLogger.log(tag, "Received ACTION_DISCONNECT. Stopping VPN.")
                stopVpn()
                stopSelf()
            }
            ACTION_UPDATE_POLICY -> {
                val preferredNetwork: Network? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_PREFERRED_NETWORK, Network::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_PREFERRED_NETWORK)
                }
                FileLogger.log(tag, "Received ACTION_UPDATE_POLICY. Preferred network: $preferredNetwork")
                stopVpn()
                startVpn(preferredNetwork)
            }
        }
        return START_STICKY
    }

    private fun startVpn(preferredNetwork: Network?) {
        try {
            val builder = Builder()
                .addAddress("10.8.0.1", 24)
                .addRoute("0.0.0.0", 0)
                .addAllowedApplication(packageName)

            if (preferredNetwork != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    FileLogger.log(tag, "Routing all traffic through network (API >= 29): $preferredNetwork")
                    setUnderlyingNetworks(arrayOf(preferredNetwork))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    FileLogger.log(tag, "Binding process to network (API < 29): $preferredNetwork")
                    if (!connectivityManager.bindProcessToNetwork(preferredNetwork)) {
                        FileLogger.log(tag, "Failed to bind process to network $preferredNetwork.")
                    }
                }
            } else {
                FileLogger.log(tag, "No preferred network. Blocking all traffic.")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setUnderlyingNetworks(null)
                }
            }

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                FileLogger.log(tag, "VPN interface is NULL, policy will not be applied.")
            }

        } catch (e: Exception) {
            FileLogger.log(tag, "Error establishing VPN: ${e.message}")
        }
    }

    private fun stopVpn() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                connectivityManager.bindProcessToNetwork(null)
            }
            vpnInterface?.close()
        } catch (e: IOException) {
            FileLogger.log(tag, "Error closing VPN interface: ${e.message}")
        } finally {
            vpnInterface = null
        }
    }

    // --- הפונקציה ליצירת התראה ---
    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                VPN_NOTIFICATION_CHANNEL_ID,
                "A Bloq VPN Service",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "התראה קבועה המציינת שה-VPN של החסימה פעיל."
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, VPN_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("A Bloq")
            .setContentText("שירות חומת האש פעיל.")
            .setSmallIcon(R.drawable.ic_netguard_shield)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        FileLogger.log(tag, "VpnService is being destroyed.")
        stopVpn()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}