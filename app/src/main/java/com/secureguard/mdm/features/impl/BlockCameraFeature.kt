package com.secureguard.mdm.features.impl

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object BlockCameraFeature : ProtectionFeature {
    override val id = "block_camera"
    override val titleRes = R.string.feature_camera_title
    override val descriptionRes = R.string.feature_camera_description
    override val iconRes = R.drawable.ic_camera_off

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        dpm.setCameraDisabled(admin, enable) // Uses a specific API call, not a user restriction
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        return dpm.getCameraDisabled(admin)
    }
}