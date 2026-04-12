package com.secureguard.mdm.features.impl

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object BlockSetUserIconFeature : ProtectionFeature {
    override val id: String = "block_set_user_icon"
    override val titleRes: Int = R.string.feature_block_set_user_icon_title
    override val descriptionRes: Int = R.string.feature_block_set_user_icon_description
    override val iconRes: Int = R.drawable.ic_set_user_icon_off
    override val requiredSdkVersion: Int = Build.VERSION_CODES.N // API 24

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        if (enable) {
            dpm.addUserRestriction(admin, UserManager.DISALLOW_SET_USER_ICON)
        } else {
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_SET_USER_ICON)
        }
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        return dpm.getUserRestrictions(admin).getBoolean(UserManager.DISALLOW_SET_USER_ICON, false)
    }
}