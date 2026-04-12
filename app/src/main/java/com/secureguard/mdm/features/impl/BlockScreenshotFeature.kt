package com.secureguard.mdm.features.impl

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object BlockScreenshotFeature : ProtectionFeature {
    override val id = "block_screenshot"
    override val titleRes = R.string.feature_screenshot_title
    override val descriptionRes = R.string.feature_screenshot_description
    override val iconRes = R.drawable.ic_screenshot_disabled
    override val requiredSdkVersion = Build.VERSION_CODES.P // API is from Android 9

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            dpm.setScreenCaptureDisabled(admin, enable)
        }
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return dpm.getScreenCaptureDisabled(admin)
        }
        return false
    }
}