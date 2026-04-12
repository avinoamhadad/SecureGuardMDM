package com.secureguard.mdm.uninstall_blocker

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.SharedPreferences
import com.secureguard.mdm.SecureGuardDeviceAdminReceiver

object UninstallBlockerManager {
    private const val PREFS_NAME = "uninstall_blocker_prefs"
    private const val KEY_BLOCKED_APPS = "blocked_apps"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getBlockedApps(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
    }

    fun setUninstallBlocked(context: Context, packageName: String, blocked: Boolean) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = SecureGuardDeviceAdminReceiver.getComponentName(context)

        try {
            dpm.setUninstallBlocked(admin, packageName, blocked)
            
            val currentBlocked = getBlockedApps(context).toMutableSet()
            if (blocked) {
                currentBlocked.add(packageName)
            } else {
                currentBlocked.remove(packageName)
            }
            getPrefs(context).edit().putStringSet(KEY_BLOCKED_APPS, currentBlocked).apply()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun isUninstallBlocked(context: Context, packageName: String): Boolean {
        return getBlockedApps(context).contains(packageName)
    }
}
