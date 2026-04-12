package com.secureguard.mdm.astore.domain

/**
 * Service interface for app store operations.
 * Implementations can use different backends (Google Play, APKPure, etc.)
 */
interface AppStoreService {
    /**
     * Get details for a specific app by package name
     */
    suspend fun getAppDetails(packageName: String): AppMetadata?
    
    /**
     * Get app details with source availability information
     */
    suspend fun getAppDetailsWithSources(packageName: String): AppMetadata?
    
    /**
     * Get details for a custom package (not installed, from settings)
     */
    suspend fun getCustomPackageDetails(packageName: String): AppMetadata?
    
    /**
     * Search for apps by query
     */
    suspend fun searchApps(query: String): List<AppMetadata>
    
    /**
     * Get download link for an app
     */
    suspend fun getDownloadLink(packageName: String): String?
    
    /**
     * Get download link for an app from a specific source
     */
    suspend fun getDownloadLink(packageName: String, source: DownloadSource): String?
    
    /**
     * Check for updates for a list of installed apps.
     * Each triple is (packageName, installedVersionCode, installedVersionName).
     */
    suspend fun checkForUpdates(installedApps: List<Triple<String, Long, String>>): List<AppMetadata>
    
    /**
     * Check availability of an app on different sources
     */
    suspend fun checkSourceAvailability(packageName: String): List<SourceAvailability>
}
