package com.secureguard.mdm.features.api

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Interface (חוזה) המגדיר את המבנה של "חסימת הגנה".
 * כל חסימה חדשה שניצור תהיה חייבת לממש את הממשק הזה.
 */
interface ProtectionFeature {
    /** מזהה ייחודי המשמש כמפתח לשמירת מצב החסימה. */
    val id: String
    /** מזהה המשאב (string resource) עבור שם החסימה שיוצג למשתמש. */
    @get:StringRes val titleRes: Int
    /** מזהה המשאב (string resource) עבור תיאור החסימה. */
    @get:StringRes val descriptionRes: Int
    /** מזהה המשאב (drawable resource) עבור האייקון שייצג את החסימה. */
    @get:DrawableRes val iconRes: Int

    /**
     * מגדיר את גרסת ה-SDK המינימלית הנדרשת כדי שהחסימה תעבוד.
     * כברירת מחדל, תומך מהגרסה המינימלית של האפליקציה (API 22).
     */
    val requiredSdkVersion: Int
        get() = Build.VERSION_CODES.LOLLIPOP_MR1

    /**
     * מחיל או מסיר את מדיניות האבטחה על המכשיר.
     * @param context הקשר של האפליקציה.
     * @param dpm מופע של DevicePolicyManager.
     * @param admin ה-ComponentName של ה-DeviceAdminReceiver.
     * @param enable True כדי להפעיל את המדיניות, false כדי לבטל אותה.
     */
    fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean)

    /**
     * בודק אם מדיניות האבטחה פעילה כרגע במכשיר.
     * @param context הקשר של האפליקציה.
     * @param dpm מופע של DevicePolicyManager.
     * @param admin ה-ComponentName של ה-DeviceAdminReceiver.
     * @return True אם המדיניות נאכפת כרגע, false אחרת.
     */
    fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean
}