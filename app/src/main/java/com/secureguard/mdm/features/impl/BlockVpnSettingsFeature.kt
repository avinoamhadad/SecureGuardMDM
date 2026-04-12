package com.secureguard.mdm.features.impl

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object BlockVpnSettingsFeature : ProtectionFeature {
    override val id: String = "block_vpn_settings"
    override val titleRes: Int = R.string.feature_block_vpn_settings_title
    override val descriptionRes: Int = R.string.feature_block_vpn_settings_description
    override val iconRes: Int = R.drawable.ic_vpn_lock
    override val requiredSdkVersion: Int = Build.VERSION_CODES.N // API 24

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        if (enable) {
            dpm.addUserRestriction(admin, UserManager.DISALLOW_CONFIG_VPN)
        } else {
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_CONFIG_VPN)
        }
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        return dpm.getUserRestrictions(admin).getBoolean(UserManager.DISALLOW_CONFIG_VPN, false)
    }
}