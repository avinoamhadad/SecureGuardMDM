package com.secureguard.mdm.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * הקלאס הראשי המייצג את מסד הנתונים של האפליקציה.
 * הוא יורש מ-RoomDatabase ומאגד את כל ה-Entities וה-DAOs.
 *
 * @property entities רשימת כל ה-Entities (טבלאות) במסד הנתונים.
 * @property version גרסת מסד הנתונים. יש להעלות מספר זה בכל פעם שמשנים את מבנה הטבלאות.
 */
@Database(entities = [BlockedAppCache::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    /**
     * פונקציה אבסטרקטית שמחזירה מופע של ה-DAO.
     * Room ייצור את המימוש שלה באופן אוטומטי.
     */
    abstract fun blockedAppCacheDao(): BlockedAppCacheDao
}