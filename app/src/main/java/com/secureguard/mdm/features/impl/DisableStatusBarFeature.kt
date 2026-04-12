package com.secureguard.mdm.features.impl

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object DisableStatusBarFeature : ProtectionFeature {
    override val id: String = "disable_status_bar"
    override val titleRes: Int = R.string.feature_disable_status_bar_title
    override val descriptionRes: Int = R.string.feature_disable_status_bar_description
    override val iconRes: Int = R.drawable.ic_status_bar_off
    override val requiredSdkVersion: Int = Build.VERSION_CODES.M // API 23

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        dpm.setStatusBarDisabled(admin, enable)
        // There is no getter for this policy, so we must persist the state ourselves.
        context.getSharedPreferences("secure_guard_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean(id, enable).apply()
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        // Rely on our stored preference as no system getter is available.
        return context.getSharedPreferences("secure_guard_prefs", Context.MODE_PRIVATE)
            .getBoolean(id, false)
    }
}