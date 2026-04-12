package com.secureguard.mdm.features.impl

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object BlockAmbientDisplayFeature : ProtectionFeature {
    override val id: String = "block_ambient_display"
    override val titleRes: Int = R.string.feature_block_ambient_display_title
    override val descriptionRes: Int = R.string.feature_block_ambient_display_description
    override val iconRes: Int = R.drawable.ic_ambient_display_off
    override val requiredSdkVersion: Int = Build.VERSION_CODES.O // API 26

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (enable) {
            dpm.addUserRestriction(admin, UserManager.DISALLOW_AMBIENT_DISPLAY)
        } else {
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_AMBIENT_DISPLAY)
        }
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return dpm.getUserRestrictions(admin).getBoolean(UserManager.DISALLOW_AMBIENT_DISPLAY, false)
    }
}