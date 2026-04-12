@file:Suppress("DEPRECATION")

package com.secureguard.mdm.features.impl

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object BlockPasswordChangeFeature : ProtectionFeature {
    override val id: String = "block_password_change"
    override val titleRes: Int = R.string.feature_block_password_change_title
    override val descriptionRes: Int = R.string.feature_block_password_change_description
    override val iconRes: Int = R.drawable.ic_password_off
    override val requiredSdkVersion: Int = Build.VERSION_CODES.LOLLIPOP_MR1

    private const val STRONG_LENGTH = 6

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        // --- התיקון המרכזי כאן, בהשראת הרעיון שלך ---
        val isAlreadyActive = isPolicyActive(context, dpm, admin)

        // אם רוצים להפעיל והפיצ'ר כבר פעיל, אל תעשה כלום
        if (enable && isAlreadyActive) {
            Log.d("BlockPasswordChange", "Policy is already active. No changes needed.")
            return
        }
        // אם רוצים לכבות והפיצ'ר כבר כבוי, אל תעשה כלום
        if (!enable && !isAlreadyActive) {
            Log.d("BlockPasswordChange", "Policy is already inactive. No changes needed.")
            return
        }

        if (enable) {
            Log.d("BlockPasswordChange", "Applying STRONG password policy.")
            // מדיניות חזקה אך יציבה יותר
            // 1. קבע איכות
            dpm.setPasswordQuality(admin, DevicePolicyManager.PASSWORD_QUALITY_COMPLEX)
            // 2. קבע דרישות
            dpm.setPasswordMinimumLetters(admin, 1)
            dpm.setPasswordMinimumNumeric(admin, 1)
            // 3. קבע אורך כולל
            dpm.setPasswordMinimumLength(admin, STRONG_LENGTH)

        } else {
            Log.d("BlockPasswordChange", "Reverting password policy to default.")
            // Revert to default (no restrictions)
            dpm.setPasswordQuality(admin, DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED)
            dpm.setPasswordMinimumLength(admin, 0)
            dpm.setPasswordMinimumLetters(admin, 0)
            dpm.setPasswordMinimumNumeric(admin, 0)
            dpm.setPasswordMinimumSymbols(admin, 0)
            dpm.setPasswordMinimumUpperCase(admin, 0)
            dpm.setPasswordMinimumLowerCase(admin, 0)
        }
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        // הבדיקה הכי טובה היא לראות אם האורך המינימלי הוא הערך שהגדרנו
        return dpm.getPasswordMinimumLength(admin) >= STRONG_LENGTH &&
                dpm.getPasswordQuality(admin) == DevicePolicyManager.PASSWORD_QUALITY_COMPLEX
    }
}