package com.secureguard.mdm.features.impl

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object DisableKeyguardFeature : ProtectionFeature {
    override val id: String = "disable_keyguard"
    override val titleRes: Int = R.string.feature_disable_keyguard_title
    override val descriptionRes: Int = R.string.feature_disable_keyguard_description
    override val iconRes: Int = R.drawable.ic_lock_open

    // FIX: Set the minimum required SDK to Android 6.0 (API 23)
    // This will automatically disable the feature on older, unsupported devices.
    override val requiredSdkVersion: Int = Build.VERSION_CODES.M

    @SuppressLint("NewApi")
    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        // Since we've set requiredSdkVersion, we can safely assume the API is available.
        // This is a safety net in case the policy is triggered from an unexpected place.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        // Use the modern, recommended API for Android 6.0+
        val flags = if (enable) {
            DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_ALL
        } else {
            DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_NONE
        }
        dpm.setKeyguardDisabledFeatures(admin, flags)
    }

    @SuppressLint("NewApi")
    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        // If the device is older than Android 6.0, the feature is not active by definition.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false

        // On supported devices, we can directly ask the system for the current state.
        // This is more reliable than saving the state in SharedPreferences.
        return dpm.getKeyguardDisabledFeatures(admin) != DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_NONE
    }
}