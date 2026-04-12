package com.secureguard.mdm.features.impl

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object BlockContentCaptureFeature : ProtectionFeature {
    override val id: String = "block_content_capture"
    override val titleRes: Int = R.string.feature_block_content_capture_title
    override val descriptionRes: Int = R.string.feature_block_content_capture_description
    override val iconRes: Int = R.drawable.ic_content_capture_off
    override val requiredSdkVersion: Int = Build.VERSION_CODES.Q // API 29

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        if (enable) {
            dpm.addUserRestriction(admin, UserManager.DISALLOW_CONTENT_CAPTURE)
        } else {
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_CONTENT_CAPTURE)
        }
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return dpm.getUserRestrictions(admin).getBoolean(UserManager.DISALLOW_CONTENT_CAPTURE, false)
    }
}