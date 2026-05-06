package com.secureguard.mdm.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Toggles the visibility of the app's icon in the device app drawer by
 * enabling/disabling the .LauncherAlias activity-alias declared in AndroidManifest.
 *
 * MainActivity itself is left enabled so the app remains reachable via:
 *  - Kiosk password dialog
 *  - Settings > App Info > Open
 *  - Secret dial code (*#*#1818#*#*) handled by SecretDialReceiver
 *  - ADB: `am start -n com.secureguard.mdm/.MainActivity`
 */
object LauncherIconHelper {
    private const val TAG = "LauncherIconHelper"
    private const val ALIAS_CLASS = "LauncherAlias"

    fun setHidden(context: Context, hidden: Boolean) {
        try {
            val pm = context.packageManager
            val alias = ComponentName(context.packageName, "${context.packageName}.$ALIAS_CLASS")
            val state = if (hidden) {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }
            pm.setComponentEnabledSetting(alias, state, PackageManager.DONT_KILL_APP)
        } catch (t: Throwable) {
            AppLogger.w(TAG, "Failed to toggle launcher alias", t)
        }
    }

    /** Returns true if the alias is currently hidden (DISABLED). */
    fun isHidden(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            val alias = ComponentName(context.packageName, "${context.packageName}.$ALIAS_CLASS")
            pm.getComponentEnabledSetting(alias) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } catch (t: Throwable) {
            false
        }
    }
}
