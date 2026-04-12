package com.secureguard.mdm.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity המייצג את מבנה טבלת המטמון עבור אפליקציות חסומות.
 * כל מופע של קלאס זה מייצג שורה בטבלה.
 *
 * @param packageName שם החבילה של האפליקציה, משמש כמפתח ראשי.
 * @param appName שם האפליקציה כפי שיוצג למשתמש.
 * @param iconPath הנתיב לקובץ האייקון השמור באחסון הפנימי של האפליקציה.
 */
@Entity(tableName = "blocked_apps_cache")
data class BlockedAppCache(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "app_name")
    val appName: String,

    @ColumnInfo(name = "icon_path")
    val iconPath: String
)