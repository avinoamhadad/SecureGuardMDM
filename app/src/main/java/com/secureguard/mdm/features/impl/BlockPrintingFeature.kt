package com.secureguard.mdm.features.impl

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object BlockPrintingFeature : ProtectionFeature {
    override val id: String = "block_printing"
    override val titleRes: Int = R.string.feature_block_printing_title
    override val descriptionRes: Int = R.string.feature_block_printing_description
    override val iconRes: Int = R.drawable.ic_print_off
    override val requiredSdkVersion: Int = Build.VERSION_CODES.O_MR1 // API 27

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return
        if (enable) {
            dpm.addUserRestriction(admin, UserManager.DISALLOW_PRINTING)
        } else {
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_PRINTING)
        }
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return false
        return dpm.getUserRestrictions(admin).getBoolean(UserManager.DISALLOW_PRINTING, false)
    }
}