package com.secureguard.mdm.features.impl

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import com.secureguard.mdm.utils.AppLogger
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object BlockPlayStoreFeature : ProtectionFeature {
    private const val PLAY_STORE_PACKAGE = "com.android.vending"

    override val id = "block_play_store"
    override val titleRes = R.string.feature_play_store_title
    override val descriptionRes = R.string.feature_play_store_description
    override val iconRes = R.drawable.ic_play_store_blocked

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        try {
            dpm.setApplicationHidden(admin, PLAY_STORE_PACKAGE, enable)
        } catch (e: Exception) {
            AppLogger.e("BlockPlayStore", "Failed to set application hidden state for Play Store", e)
        }
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        return try {
            dpm.isApplicationHidden(admin, PLAY_STORE_PACKAGE)
        } catch (e: Exception) {
            false
        }
    }
}