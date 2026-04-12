package com.secureguard.mdm.astore.domain

/**
 * Enum representing available download sources for APK files
 */
enum class DownloadSource(val displayName: String) {
    APKPURE("APKPure"),
    APK_MIRROR("APKMirror"),
    PLAY_STORE("Google Play"),
    STORE_KIT("StoreKit"),
    APKCOMBO("APKCombo"),
    APTOIDE("Aptoide"),
    FDROID("F-Droid"),
    APK_FOLLOW("APKFollow"),
    UPTODOWN("Uptodown"),
    EVOZI("Evozi")
}

/**
 * Represents the availability of an app on different download sources
 */
data class SourceAvailability(
    val source: DownloadSource,
    val isAvailable: Boolean,
    val downloadUrl: String? = null,
    val versionName: String? = null,
    val size: Long = 0,
    val originalSource: String? = null // The actual source StoreKit used (e.g., "APKCOMBO", "APKPURE", "APTOIDE", "FDROID")
)

/**
 * Metadata for an application in the store
 */
data class AppMetadata(
    val packageName: String,
    val title: String,
    val versionName: String,
    val versionCode: Long,
    val iconUrl: String? = null,
    val description: String? = null,
    val downloadUrl: String? = null,
    val isInstalled: Boolean = false,
    val updateAvailable: Boolean = false,
    val size: Long = 0,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val selectedSource: DownloadSource = DownloadSource.STORE_KIT,
    val availableSources: List<SourceAvailability> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isCustomPackage: Boolean = false // Flag for custom packages from settings
) {
    /**
     * Returns the download URL for the selected source.
     * Falls back to the first available source if the selected one isn't found.
     */
    fun getDownloadUrlForSelectedSource(): String? {
        return availableSources.find { it.source == selectedSource }?.downloadUrl
            ?: availableSources.firstOrNull { it.isAvailable && it.downloadUrl != null }?.downloadUrl
            ?: downloadUrl
    }
    
    /**
     * Returns true if the selected source is available.
     * Falls back to checking if any source is available when the selected one isn't found.
     */
    fun isSourceAvailable(): Boolean {
        return availableSources.find { it.source == selectedSource }?.isAvailable 
            ?: availableSources.any { it.isAvailable }
    }
    
    /**
     * Returns the original source name for display (where StoreKit actually got the APK from)
     */
    fun getOriginalSourceName(): String? {
        return availableSources.find { it.source == selectedSource }?.originalSource
    }
}
