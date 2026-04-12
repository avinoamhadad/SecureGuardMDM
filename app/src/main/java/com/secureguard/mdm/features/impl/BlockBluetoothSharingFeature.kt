package com.secureguard.mdm.features.impl

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object BlockBluetoothSharingFeature : ProtectionFeature {
    override val id = "block_bluetooth_sharing"
    override val titleRes = R.string.feature_bluetooth_sharing_title
    override val descriptionRes = R.string.feature_bluetooth_sharing_description
    override val iconRes = R.drawable.ic_bluetooth_sharing_off

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        if (enable) dpm.addUserRestriction(admin, UserManager.DISALLOW_BLUETOOTH_SHARING)
        else dpm.clearUserRestriction(admin, UserManager.DISALLOW_BLUETOOTH_SHARING)
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return dpm.getUserRestrictions(admin).getBoolean(UserManager.DISALLOW_BLUETOOTH_SHARING)
        }
        return context.getSharedPreferences("secure_guard_prefs", Context.MODE_PRIVATE).getBoolean(id, false)
    }
}