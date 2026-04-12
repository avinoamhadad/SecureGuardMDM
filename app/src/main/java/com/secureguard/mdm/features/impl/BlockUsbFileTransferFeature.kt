
package com.secureguard.mdm.features.impl

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object BlockUsbFileTransferFeature : ProtectionFeature {
    override val id = "block_usb_transfer"
    override val titleRes = R.string.feature_usb_transfer_title
    override val descriptionRes = R.string.feature_usb_transfer_description
    override val iconRes = R.drawable.ic_usb_off
    // FIX: Correctly state required SDK version
    override val requiredSdkVersion = Build.VERSION_CODES.M

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        // FIX: Add version guard to prevent crash on API < 23
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        if (enable) {
            dpm.addUserRestriction(admin, UserManager.DISALLOW_USB_FILE_TRANSFER)
        } else {
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_USB_FILE_TRANSFER)
        }
        // FIX: Persist state for the fallback check on APIs < 24
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            context.getSharedPreferences("secure_guard_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean(id, enable).apply()
        }
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        // FIX: Add version guard to prevent crash on API < 23
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return dpm.getUserRestrictions(admin).getBoolean(UserManager.DISALLOW_USB_FILE_TRANSFER)
        }
        // FIX: Read from saved state for API 23
        return context.getSharedPreferences("secure_guard_prefs", Context.MODE_PRIVATE).getBoolean(id, false)
    }
}