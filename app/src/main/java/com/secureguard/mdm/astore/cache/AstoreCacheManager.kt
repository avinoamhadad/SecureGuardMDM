package com.secureguard.mdm.astore.cache

import android.content.Context
import android.content.SharedPreferences
import com.secureguard.mdm.utils.AppLogger
import com.secureguard.mdm.astore.domain.AppMetadata
import com.secureguard.mdm.astore.domain.DownloadSource
import com.secureguard.mdm.astore.domain.SourceAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AstoreCacheManager"
private const val CACHE_PREFS_NAME = "astore_cache"
private const val KEY_APPS_CACHE = "apps_cache"
private const val KEY_LAST_UPDATE = "last_update"
private const val CACHE_VALIDITY_MS = 30 * 60 * 1000L // 30 minutes

/**
 * Cache manager for Astore to reduce network calls and improve loading time.
 * Uses SharedPreferences for persistence.
 */
@Singleton
class AstoreCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(CACHE_PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Check if cache is still valid
     */
    fun isCacheValid(): Boolean {
        val lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0)
        return System.currentTimeMillis() - lastUpdate < CACHE_VALIDITY_MS
    }

    /**
     * Get cached apps list
     */
    fun getCachedApps(): List<AppMetadata> {
        return try {
            val json = prefs.getString(KEY_APPS_CACHE, null) ?: return emptyList()
            parseAppsFromJson(json)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to parse cached apps", e)
            emptyList()
        }
    }

    /**
     * Save apps to cache
     */
    fun saveAppsToCache(apps: List<AppMetadata>) {
        try {
            val json = appsToJson(apps)
            prefs.edit()
                .putString(KEY_APPS_CACHE, json)
                .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                .apply()
            AppLogger.d(TAG, "Saved ${apps.size} apps to cache")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to save apps to cache", e)
        }
    }

    /**
     * Get a single cached app by package name
     */
    fun getCachedApp(packageName: String): AppMetadata? {
        return getCachedApps().find { it.packageName == packageName }
    }

    /**
     * Update a single app in cache
     */
    fun updateCachedApp(app: AppMetadata) {
        val apps = getCachedApps().toMutableList()
        val index = apps.indexOfFirst { it.packageName == app.packageName }
        if (index >= 0) {
            apps[index] = app
        } else {
            apps.add(app)
        }
        saveAppsToCache(apps)
    }

    /**
     * Clear the cache
     */
    fun clearCache() {
        prefs.edit().clear().apply()
        AppLogger.d(TAG, "Cache cleared")
    }

    /**
     * Get cache age in milliseconds
     */
    fun getCacheAge(): Long {
        val lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0)
        return if (lastUpdate == 0L) Long.MAX_VALUE else System.currentTimeMillis() - lastUpdate
    }

    // JSON parsing helpers
    private fun appsToJson(apps: List<AppMetadata>): String {
        val jsonArray = JSONArray()
        apps.forEach { app ->
            jsonArray.put(appToJson(app))
        }
        return jsonArray.toString()
    }

    private fun appToJson(app: AppMetadata): JSONObject {
        return JSONObject().apply {
            put("packageName", app.packageName)
            put("title", app.title)
            put("versionName", app.versionName)
            put("versionCode", app.versionCode)
            put("iconUrl", app.iconUrl)
            put("description", app.description)
            put("isInstalled", app.isInstalled)
            put("updateAvailable", app.updateAvailable)
            put("size", app.size)
            put("selectedSource", app.selectedSource.name)
            put("lastUpdated", app.lastUpdated)
            put("isCustomPackage", app.isCustomPackage)
            
            // Save available sources
            val sourcesArray = JSONArray()
            app.availableSources.forEach { source ->
                sourcesArray.put(JSONObject().apply {
                    put("source", source.source.name)
                    put("isAvailable", source.isAvailable)
                    put("downloadUrl", source.downloadUrl)
                    put("versionName", source.versionName)
                    put("size", source.size)
                    put("originalSource", source.originalSource)
                })
            }
            put("availableSources", sourcesArray)
        }
    }

    private fun parseAppsFromJson(json: String): List<AppMetadata> {
        val apps = mutableListOf<AppMetadata>()
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            try {
                val jsonObj = jsonArray.getJSONObject(i)
                apps.add(parseAppFromJson(jsonObj))
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to parse app at index $i", e)
            }
        }
        return apps
    }

    private fun parseAppFromJson(json: JSONObject): AppMetadata {
        val sourcesList = mutableListOf<SourceAvailability>()
        val sourcesArray = json.optJSONArray("availableSources")
        if (sourcesArray != null) {
            for (i in 0 until sourcesArray.length()) {
                val sourceJson = sourcesArray.getJSONObject(i)
                sourcesList.add(SourceAvailability(
                    source = DownloadSource.valueOf(sourceJson.getString("source")),
                    isAvailable = sourceJson.getBoolean("isAvailable"),
                    downloadUrl = sourceJson.optString("downloadUrl").takeIf { it.isNotEmpty() },
                    versionName = sourceJson.optString("versionName").takeIf { it.isNotEmpty() },
                    size = sourceJson.optLong("size", 0),
                    originalSource = sourceJson.optString("originalSource").takeIf { it.isNotEmpty() }
                ))
            }
        }

        return AppMetadata(
            packageName = json.getString("packageName"),
            title = json.getString("title"),
            versionName = json.getString("versionName"),
            versionCode = json.getLong("versionCode"),
            iconUrl = json.optString("iconUrl").takeIf { it.isNotEmpty() },
            description = json.optString("description").takeIf { it.isNotEmpty() },
            isInstalled = json.getBoolean("isInstalled"),
            updateAvailable = json.getBoolean("updateAvailable"),
            size = json.getLong("size"),
            selectedSource = try {
                DownloadSource.valueOf(json.getString("selectedSource"))
            } catch (e: Exception) {
                DownloadSource.STORE_KIT
            },
            availableSources = sourcesList,
            lastUpdated = json.optLong("lastUpdated", System.currentTimeMillis()),
            isCustomPackage = json.optBoolean("isCustomPackage", false)
        )
    }
}
