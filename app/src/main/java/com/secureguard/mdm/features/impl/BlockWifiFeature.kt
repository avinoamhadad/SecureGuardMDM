package com.secureguard.mdm.features.impl

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager // <--- הוספת import חיוני
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature

object BlockWifiFeature : ProtectionFeature {

    override val id = "block_wifi"
    override val titleRes = R.string.feature_wifi_title
    override val descriptionRes = R.string.feature_wifi_description
    override val iconRes = R.drawable.ic_wifi_off

    // אנחנו כבר לא צריכים להכריז על גרסה מינימלית גבוהה,
    // מכיוון שהשיטה החדשה נתמכת בכל הגרסאות הרלוונטיות.
    // requiredSdkVersion יקבל את ערך ברירת המחדל.

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        if (enable) {
            dpm.addUserRestriction(admin, UserManager.DISALLOW_CONFIG_WIFI)
        } else {
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_CONFIG_WIFI)
        }
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        // השיטה הזו עובדת רק מ-API 24, אבל היא עדיין הדרך הנכונה לבדוק.
        // אנו נשמור על בדיקת הגרסה כאן כפי שעשינו עם החסימות האחרות.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return dpm.getUserRestrictions(admin).getBoolean(UserManager.DISALLOW_CONFIG_WIFI, false)
        }
        // במכשירים ישנים יותר, אין דרך פשוטה לבדוק, אך ההפעלה והכיבוי עדיין עובדים.
        // נחזיר את המצב השמור מההגדרות כהערכה הטובה ביותר.
        val prefs = context.getSharedPreferences("secure_guard_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean(id, false)
    }
}