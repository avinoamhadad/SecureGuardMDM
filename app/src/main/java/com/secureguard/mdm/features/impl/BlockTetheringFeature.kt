package com.secureguard.mdm.features.impl

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object BlockTetheringFeature : ProtectionFeature {
    override val id = "block_tethering"
    override val titleRes = R.string.feature_tethering_title
    override val descriptionRes = R.string.feature_tethering_description
    override val iconRes = R.drawable.ic_tethering_off

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        if (enable) dpm.addUserRestriction(admin, UserManager.DISALLOW_CONFIG_TETHERING)
        else dpm.clearUserRestriction(admin, UserManager.DISALLOW_CONFIG_TETHERING)
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return dpm.getUserRestrictions(admin).getBoolean(UserManager.DISALLOW_CONFIG_TETHERING)
        }
        return context.getSharedPreferences("secure_guard_prefs", Context.MODE_PRIVATE).getBoolean(id, false)
    }
}