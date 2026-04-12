package com.secureguard.mdm.features.impl

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object BlockConfigMobileNetworksFeature : ProtectionFeature {
    override val id: String = "block_config_mobile_networks"
    override val titleRes: Int = R.string.feature_block_config_mobile_networks_title
    override val descriptionRes: Int = R.string.feature_block_config_mobile_networks_description
    override val iconRes: Int = R.drawable.ic_config_mobile_networks_off

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        if (enable) {
            dpm.addUserRestriction(admin, UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)
        } else {
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)
        }
        context.getSharedPreferences("secure_guard_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean(id, enable).apply()
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        // --- התיקון כאן ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return dpm.getUserRestrictions(admin).getBoolean(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS, false)
        }
        return context.getSharedPreferences("secure_guard_prefs", Context.MODE_PRIVATE)
            .getBoolean(id, false)
    }
}