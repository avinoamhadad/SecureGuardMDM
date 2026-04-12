package com.secureguard.mdm.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO (Data Access Object) עבור טבלת המטמון של אפליקציות חסומות.
 * הוא מכיל את כל הפעולות הנדרשות על מסד הנתונים עבור טבלה זו.
 */
@Dao
interface BlockedAppCacheDao {

    /**
     * מכניס רשומה חדשה או מחליף רשומה קיימת אם שם החבילה זהה.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(appCache: BlockedAppCache)

    /**
     * מושך את כל הרשומות מהטבלה.
     */
    @Query("SELECT * FROM blocked_apps_cache")
    suspend fun getAll(): List<BlockedAppCache>

    /**
     * מושך רשומה ספציפית לפי שם החבילה שלה.
     */
    @Query("SELECT * FROM blocked_apps_cache WHERE package_name = :packageName")
    suspend fun getByPackageName(packageName: String): BlockedAppCache?

    /**
     * מוחק רשימה של רשומות לפי שמות החבילה שלהן.
     */
    @Query("DELETE FROM blocked_apps_cache WHERE package_name IN (:packageNames)")
    suspend fun deleteByPackageNames(packageNames: List<String>)
}