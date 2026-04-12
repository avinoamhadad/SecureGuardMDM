package com.secureguard.mdm.features.impl

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object BlockUninstallAppsFeature : ProtectionFeature {
    override val id: String = "block_uninstall_apps"
    override val titleRes: Int = R.string.feature_block_uninstall_apps_title
    override val descriptionRes: Int = R.string.feature_block_uninstall_apps_description
    override val iconRes: Int = R.drawable.ic_uninstall_off

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        if (enable) {
            dpm.addUserRestriction(admin, UserManager.DISALLOW_UNINSTALL_APPS)
        } else {
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_UNINSTALL_APPS)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            context.getSharedPreferences("secure_guard_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean(id, enable).apply()
        }
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return dpm.getUserRestrictions(admin).getBoolean(UserManager.DISALLOW_UNINSTALL_APPS, false)
        }
        return context.getSharedPreferences("secure_guard_prefs", Context.MODE_PRIVATE)
            .getBoolean(id, false)
    }
}