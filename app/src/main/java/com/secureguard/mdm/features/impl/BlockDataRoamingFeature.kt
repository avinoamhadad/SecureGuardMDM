package com.secureguard.mdm.features.impl

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object BlockDataRoamingFeature : ProtectionFeature {
    override val id: String = "block_data_roaming"
    override val titleRes: Int = R.string.feature_block_data_roaming_title
    override val descriptionRes: Int = R.string.feature_block_data_roaming_description
    override val iconRes: Int = R.drawable.ic_data_roaming_off
    override val requiredSdkVersion: Int = Build.VERSION_CODES.M // API 23

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        // FIX: Add version guard to prevent crash on API < 23
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        if (enable) {
            dpm.addUserRestriction(admin, UserManager.DISALLOW_DATA_ROAMING)
        } else {
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_DATA_ROAMING)
        }
        // FIX: Persist state for the fallback check on APIs < 24
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            context.getSharedPreferences("secure_guard_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean(id, enable).apply()
        }
    }

    @SuppressLint("NewApi")
    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        // FIX: Add version guard to prevent crash on API < 23
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false

        // FIX: Check for API 24+ before using the new API, otherwise fallback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return dpm.getUserRestrictions(admin).getBoolean(UserManager.DISALLOW_DATA_ROAMING, false)
        }
        return context.getSharedPreferences("secure_guard_prefs", Context.MODE_PRIVATE)
            .getBoolean(id, false)
    }
}