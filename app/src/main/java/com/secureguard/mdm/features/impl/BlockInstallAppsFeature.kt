package com.secureguard.mdm.features.impl

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object BlockInstallAppsFeature : ProtectionFeature {
    override val id = "block_install_apps"
    override val titleRes = R.string.feature_install_apps_disabled_title
    override val descriptionRes = R.string.feature_install_apps_disabled_description
    override val iconRes = R.drawable.ic_install_mobile_disabled

    // This restriction is available from API 23 (Android 6.0)
    override val requiredSdkVersion = Build.VERSION_CODES.M

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        val restriction = UserManager.DISALLOW_INSTALL_APPS
        if (enable) dpm.addUserRestriction(admin, restriction) else dpm.clearUserRestriction(admin, restriction)
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return dpm.getUserRestrictions(admin).getBoolean(UserManager.DISALLOW_INSTALL_APPS, false)
        }
        return context.getSharedPreferences("secure_guard_prefs", Context.MODE_PRIVATE).getBoolean(id, false)
    }
}