package com.secureguard.mdm.features.impl

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object BlockSystemErrorDialogsFeature : ProtectionFeature {
    override val id: String = "block_system_error_dialogs"
    override val titleRes: Int = R.string.feature_block_system_error_dialogs_title
    override val descriptionRes: Int = R.string.feature_block_system_error_dialogs_description
    override val iconRes: Int = R.drawable.ic_system_error_off
    override val requiredSdkVersion: Int = Build.VERSION_CODES.P // API 28

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        if (enable) {
            dpm.addUserRestriction(admin, UserManager.DISALLOW_SYSTEM_ERROR_DIALOGS)
        } else {
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_SYSTEM_ERROR_DIALOGS)
        }
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return dpm.getUserRestrictions(admin).getBoolean(UserManager.DISALLOW_SYSTEM_ERROR_DIALOGS, false)
    }
}