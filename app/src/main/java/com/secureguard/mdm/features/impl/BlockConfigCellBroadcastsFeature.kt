package com.secureguard.mdm.features.impl

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object BlockConfigCellBroadcastsFeature : ProtectionFeature {
    override val id: String = "block_config_cell_broadcasts"
    override val titleRes: Int = R.string.feature_block_config_cell_broadcasts_title
    override val descriptionRes: Int = R.string.feature_block_config_cell_broadcasts_description
    override val iconRes: Int = R.drawable.ic_cell_broadcast_off

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        if (enable) {
            dpm.addUserRestriction(admin, UserManager.DISALLOW_CONFIG_CELL_BROADCASTS)
        } else {
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_CONFIG_CELL_BROADCASTS)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            context.getSharedPreferences("secure_guard_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean(id, enable).apply()
        }
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return dpm.getUserRestrictions(admin).getBoolean(UserManager.DISALLOW_CONFIG_CELL_BROADCASTS, false)
        }
        return context.getSharedPreferences("secure_guard_prefs", Context.MODE_PRIVATE)
            .getBoolean(id, false)
    }
}