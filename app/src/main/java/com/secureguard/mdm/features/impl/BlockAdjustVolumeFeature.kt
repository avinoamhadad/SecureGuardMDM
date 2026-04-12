package com.secureguard.mdm.features.impl

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object BlockAdjustVolumeFeature : ProtectionFeature {
    override val id: String = "block_adjust_volume"
    override val titleRes: Int = R.string.feature_block_adjust_volume_title
    override val descriptionRes: Int = R.string.feature_block_adjust_volume_description
    override val iconRes: Int = R.drawable.ic_volume_off

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        if (enable) {
            dpm.addUserRestriction(admin, UserManager.DISALLOW_ADJUST_VOLUME)
        } else {
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_ADJUST_VOLUME)
        }
        // Persist state for the fallback check on APIs < 24
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            context.getSharedPreferences("secure_guard_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean(id, enable).apply()
        }
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return dpm.getUserRestrictions(admin).getBoolean(UserManager.DISALLOW_ADJUST_VOLUME, false)
        }
        // Fallback for APIs < 24, where no getter exists.
        return context.getSharedPreferences("secure_guard_prefs", Context.MODE_PRIVATE)
            .getBoolean(id, false)
    }
}