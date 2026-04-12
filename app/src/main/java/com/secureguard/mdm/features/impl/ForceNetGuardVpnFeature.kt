package com.secureguard.mdm.features.impl

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.net.VpnService
import android.os.Build
import com.secureguard.mdm.utils.AppLogger
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object ForceNetGuardVpnFeature : ProtectionFeature {
    override val id: String = "force_netguard_vpn"
    override val titleRes: Int = R.string.feature_force_netguard_vpn_title
    override val descriptionRes: Int = R.string.feature_force_netguard_vpn_description
    override val iconRes: Int = R.drawable.ic_netguard_shield
    override val requiredSdkVersion: Int = Build.VERSION_CODES.N // API 24

    private const val NETGUARD_PACKAGE_NAME = "eu.faircode.netguard"
    private const val TAG = "ForceNetGuardFeature"

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        if (VpnService.prepare(context) != null) {
            AppLogger.w(TAG, "VPN permission not granted by user. Cannot apply Always-On VPN policy.")
            return
        }

        try {
            if (enable) {
                // Set NetGuard as the always-on VPN and enable lockdown mode.
                dpm.setAlwaysOnVpnPackage(admin, NETGUARD_PACKAGE_NAME, true)
            } else {
                // Remove our app as the always-on VPN.
                dpm.setAlwaysOnVpnPackage(admin, null, false)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to set Always-On VPN policy for NetGuard", e)
        }
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        return NETGUARD_PACKAGE_NAME == dpm.getAlwaysOnVpnPackage(admin)
    }
}