package com.secureguard.mdm.astore.impl

import com.secureguard.mdm.utils.AppLogger
import com.secureguard.mdm.astore.domain.AppMetadata
import com.secureguard.mdm.astore.domain.AppStoreService
import com.secureguard.mdm.astore.domain.DownloadSource
import com.secureguard.mdm.astore.domain.SourceAvailability
import io.github.kdroidfilter.storekit.apklinkresolver.core.service.ApkLinkResolverService
import io.github.kdroidfilter.storekit.gplay.scrapper.services.getGooglePlayApplicationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import io.github.kdroidfilter.storekit.aptoide.api.services.AptoideService
import io.github.kdroidfilter.storekit.fdroid.api.services.FDroidService
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "StoreKitAppStoreService"

@Singleton
class StoreKitAppStoreService @Inject constructor() : AppStoreService {

    private val apkLinkResolver = ApkLinkResolverService()

    override suspend fun getAppDetails(packageName: String): AppMetadata? = withContext(Dispatchers.IO) {
        try {
            val appInfo = getGooglePlayApplicationInfo(packageName)
            var versionName = appInfo.version ?: ""
            var iconUrl = appInfo.icon

            // Fallback for "Varies with device"
            if (versionName.equals("Varies with device", ignoreCase = true) || versionName.isEmpty()) {
                try {
                    val downloadInfo = apkLinkResolver.getApkDownloadLink(packageName)
                    versionName = downloadInfo.version ?: versionName
                } catch (e: Exception) {
                    AppLogger.d(TAG, "APKLinkResolver fallback failed for $packageName: ${e.message}")
                }

                if (versionName.equals("Varies with device", ignoreCase = true) || versionName.isEmpty()) {
                    try {
                        val apkPureApp = io.github.kdroidfilter.storekit.apkpure.scraper.services.getApkPureApplicationInfo(packageName)
                        versionName = apkPureApp.version ?: versionName
                    } catch (e: Exception) {
                        AppLogger.d(TAG, "APKPure fallback failed for $packageName: ${e.message}")
                    }
                }
            }

            versionName = versionName.filter { it.isDigit() || it == '.' }.trim('.')

            AppMetadata(
                packageName = packageName,
                title = appInfo.title,
                versionName = versionName,
                versionCode = 0,
                iconUrl = iconUrl,
                description = appInfo.summary,
                size = 0
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to get app details for $packageName", e)
            null
        }
    }
    
    /**
     * Get app details for a custom package (not installed, from settings)
     */
    override suspend fun getCustomPackageDetails(packageName: String): AppMetadata? = withContext(Dispatchers.IO) {
        try {
            // Try Google Play first for icon and title
            var iconUrl: String? = null
            var title = packageName
            try {
                val playInfo = getGooglePlayApplicationInfo(packageName)
                iconUrl = playInfo.icon
                if (playInfo.title.isNotEmpty()) title = playInfo.title.removePrefix("Download ").trim()
            } catch (e: Exception) {
                AppLogger.d(TAG, "Google Play icon lookup failed for $packageName: ${e.message}")
            }
            
            // Get download info from StoreKit
            val downloadInfo = apkLinkResolver.getApkDownloadLink(packageName)
            if (downloadInfo.title.isNotEmpty()) title = downloadInfo.title.removePrefix("Download ").trim()
            
            AppMetadata(
                packageName = packageName,
                title = title,
                versionName = downloadInfo.version ?: "",
                versionCode = downloadInfo.versionCode.toLongOrNull() ?: 0,
                iconUrl = iconUrl,
                description = null,
                size = downloadInfo.fileSize,
                isInstalled = false,
                isCustomPackage = true
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to get custom package details for $packageName", e)
            // Still try to get icon from Google Play even if StoreKit failed
            var iconUrl: String? = null
            var title = packageName
            try {
                val playInfo = getGooglePlayApplicationInfo(packageName)
                iconUrl = playInfo.icon
                if (playInfo.title.isNotEmpty()) title = playInfo.title
            } catch (_: Exception) {}
            
            AppMetadata(
                packageName = packageName,
                title = title,
                versionName = "",
                versionCode = 0,
                iconUrl = iconUrl,
                isInstalled = false,
                isCustomPackage = true
            )
        }
    }

    override suspend fun getAppDetailsWithSources(packageName: String): AppMetadata? = withContext(Dispatchers.IO) {
        val basicInfo = getAppDetails(packageName) ?: return@withContext null
        
        // Check availability on all sources in parallel
        val sources = checkSourceAvailability(packageName)
        
        basicInfo.copy(availableSources = sources)
    }

    override suspend fun searchApps(query: String): List<AppMetadata> {
        return emptyList() // Not used in update-only mode
    }

    override suspend fun getDownloadLink(packageName: String): String? = withContext(Dispatchers.IO) {
        try {
            val downloadInfo = apkLinkResolver.getApkDownloadLink(packageName)
            downloadInfo.downloadLink
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to get download link for $packageName", e)
            null
        }
    }

    override suspend fun getDownloadLink(packageName: String, source: DownloadSource): String? = withContext(Dispatchers.IO) {
        when (source) {
            DownloadSource.STORE_KIT, DownloadSource.PLAY_STORE -> {
                try {
                    val downloadInfo = apkLinkResolver.getApkDownloadLink(packageName)
                    downloadInfo.downloadLink
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to get StoreKit download link for $packageName", e)
                    null
                }
            }
            DownloadSource.APKPURE -> {
                try {
                    // Try APKPure-specific resolver if available
                    val downloadInfo = apkLinkResolver.getApkDownloadLink(packageName)
                    downloadInfo.downloadLink
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to get APKPure download link for $packageName", e)
                    null
                }
            }
            DownloadSource.APK_MIRROR -> {
                // APKMirror not currently supported by storekit
                null
            }
            else -> {
                // For other sources, use the default resolver
                try {
                    val downloadInfo = apkLinkResolver.getApkDownloadLink(packageName)
                    downloadInfo.downloadLink
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to get download link for $packageName from $source", e)
                    null
                }
            }
        }
    }

    override suspend fun checkForUpdates(installedApps: List<Triple<String, Long, String>>): List<AppMetadata> = withContext(Dispatchers.IO) {
        val updates = mutableListOf<AppMetadata>()
        val shuffledApps = installedApps.shuffled() // Randomize order to avoid detection patterns
        
        // Process in chunks of 5 to avoid overwhelming sources while still gaining huge performance
        val chunkSize = 5
        
        for (chunk in shuffledApps.chunked(chunkSize)) {
            val deferredUpdates = chunk.map { (packageName, currentVersionCode, installedVersionName) ->
                async {
                    try {
                        // Add a small random delay between requests to look more human
                        kotlinx.coroutines.delay((100L..500L).random())
                        
                        val appInfo = getGooglePlayApplicationInfo(packageName)
                        var storeVersionName = appInfo.version ?: ""
                        storeVersionName = storeVersionName.filter { it.isDigit() || it == '.' }.trim('.')
                        
                        if (storeVersionName.isNotEmpty()) {
                            // Compare versions — only mark as update if store version is newer
                            if (isNewerVersion(storeVersionName, installedVersionName)) {
                                return@async AppMetadata(
                                    packageName = packageName,
                                    title = appInfo.title.removePrefix("Download ").trim(),
                                    versionName = storeVersionName,
                                    versionCode = 0,
                                    iconUrl = appInfo.icon,
                                    updateAvailable = true 
                                )
                            } else {
                                AppLogger.d(TAG, "$packageName is up-to-date (installed=$installedVersionName, store=$storeVersionName)")
                                return@async null
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.d(TAG, "Google Play lookup failed for $packageName: ${e.message}")
                    }
                    
                    // Fallback: try ApkLinkResolver if Google Play failed
                    try {
                        kotlinx.coroutines.delay((100L..300L).random())
                        
                        val downloadInfo = apkLinkResolver.getApkDownloadLink(packageName)
                        var resolvedVersion = downloadInfo.version ?: ""
                        resolvedVersion = resolvedVersion.filter { it.isDigit() || it == '.' }.trim('.')
                        
                        if (resolvedVersion.isNotEmpty()) {
                            if (isNewerVersion(resolvedVersion, installedVersionName)) {
                                AppLogger.d(TAG, "Resolved $packageName via ApkLinkResolver (${downloadInfo.source})")
                                return@async AppMetadata(
                                    packageName = packageName,
                                    title = packageName,
                                    versionName = resolvedVersion,
                                    versionCode = 0,
                                    updateAvailable = true
                                )
                            } else {
                                AppLogger.d(TAG, "$packageName is up-to-date (installed=$installedVersionName, resolved=$resolvedVersion)")
                            }
                        } else {
                            AppLogger.d(TAG, "No version found for $packageName from any source")
                        }
                    } catch (e2: Exception) {
                        AppLogger.d(TAG, "All sources failed for $packageName: ${e2.message}")
                    }
                    null
                }
            }
            updates.addAll(deferredUpdates.awaitAll().filterNotNull())
        }
        updates
    }
    
    /**
     * Compare two version strings to determine if storeVersion is newer.
     * Handles semantic versioning (e.g., "1.2.3" vs "1.2.4").
     */
    private fun isNewerVersion(storeVersion: String, installedVersion: String): Boolean {
        if (installedVersion.isEmpty()) return true // No installed version = always update
        if (storeVersion == installedVersion) return false
        
        val storeParts = storeVersion.split(".").mapNotNull { it.toIntOrNull() }
        val installedParts = installedVersion.filter { it.isDigit() || it == '.' }.trim('.').split(".").mapNotNull { it.toIntOrNull() }

        if (storeParts.isEmpty() || installedParts.isEmpty()) {
            return storeVersion != installedVersion
        }
        
        val maxLen = maxOf(storeParts.size, installedParts.size)
        for (i in 0 until maxLen) {
            val s = storeParts.getOrElse(i) { 0 }
            val inst = installedParts.getOrElse(i) { 0 }
            if (s > inst) return true
            if (s < inst) return false
        }
        return false // Versions are equal
    }

    override suspend fun checkSourceAvailability(packageName: String): List<SourceAvailability> = withContext(Dispatchers.IO) {
        val sources = mutableListOf<SourceAvailability>()
        
        // Use parallel async calls for efficiency
        val deferredResults = listOf(
            // StoreKit / ApkLinkResolver (Checks multiple sources based on internal priority)
            async {
                try {
                    val downloadInfo = apkLinkResolver.getApkDownloadLink(packageName)
                    val mappedSource = mapStoreKitSource(downloadInfo.source)
                    SourceAvailability(
                        source = mappedSource,
                        isAvailable = true,
                        downloadUrl = downloadInfo.downloadLink,
                        versionName = downloadInfo.version,
                        size = downloadInfo.fileSize,
                        originalSource = downloadInfo.source
                    )
                } catch (e: Exception) {
                    AppLogger.d(TAG, "StoreKit (General) not available for $packageName: ${e.message}")
                    SourceAvailability(source = DownloadSource.STORE_KIT, isAvailable = false)
                }
            },
            // APKPure Specific
            async {
                try {
                    val apkPureInfo = io.github.kdroidfilter.storekit.apkpure.scraper.services.getApkPureApplicationInfo(packageName)
                    SourceAvailability(
                        source = DownloadSource.APKPURE,
                        isAvailable = true,
                        versionName = apkPureInfo.version,
                        originalSource = "APKPURE"
                    )
                } catch (e: Exception) {
                    AppLogger.d(TAG, "APKPure not available for $packageName: ${e.message}")
                    SourceAvailability(source = DownloadSource.APKPURE, isAvailable = false)
                }
            },
            // Aptoide Specific
            async {
                try {
                   val aptoideService = AptoideService()
                   val appInfo = aptoideService.getAppMetaByPackageName(packageName)
                   SourceAvailability(
                       source = DownloadSource.APTOIDE,
                       isAvailable = true,
                       versionName = appInfo.file.vername,
                       size = appInfo.file.filesize,
                       originalSource = "APTOIDE"
                   )
                } catch (e: Exception) {
                    AppLogger.d(TAG, "Aptoide not available for $packageName: ${e.message}")
                    SourceAvailability(source = DownloadSource.APTOIDE, isAvailable = false)
                }
            },
            // F-Droid Specific
            async {
                try {
                    val fdroidService = FDroidService()
                    val packageInfo = fdroidService.getPackageInfo(packageName)
                    SourceAvailability(
                        source = DownloadSource.FDROID,
                        isAvailable = true,
                        versionName = packageInfo.packages.firstOrNull()?.versionName ?: "",
                        originalSource = "FDROID"
                    )
                } catch (e: Exception) {
                    AppLogger.d(TAG, "F-Droid not available for $packageName: ${e.message}")
                    SourceAvailability(source = DownloadSource.FDROID, isAvailable = false)
                }
            },
            // APKCombo Specific
            async {
                try {
                    val info = io.github.kdroidfilter.storekit.apkcombo.scraper.services.getApkComboApplicationInfo(packageName)
                    SourceAvailability(
                        source = DownloadSource.APKCOMBO,
                        isAvailable = true,
                        versionName = info.version,
                        originalSource = "APKCOMBO"
                    )
                } catch (e: Exception) {
                    AppLogger.d(TAG, "APKCombo not available for $packageName: ${e.message}")
                    SourceAvailability(source = DownloadSource.APKCOMBO, isAvailable = false)
                }
            }
        )
        
        deferredResults.awaitAll().filter { it.isAvailable || it.source == DownloadSource.STORE_KIT }.forEach { sources.add(it) }
        
        // APKMirror placeholder (not implemented)
        if (sources.none { it.source == DownloadSource.APK_MIRROR }) {
            sources.add(SourceAvailability(source = DownloadSource.APK_MIRROR, isAvailable = false))
        }
        
        sources
    }
    
    /**
     * Map StoreKit source names to our DownloadSource enum
     */
    private fun mapStoreKitSource(source: String): DownloadSource {
        return when (source.uppercase()) {
            "APKCOMBO" -> DownloadSource.APKCOMBO
            "APKPURE" -> DownloadSource.APKPURE
            "APTOIDE" -> DownloadSource.APTOIDE
            "FDROID" -> DownloadSource.FDROID
            "APK_FOLLOW", "APKFOLLOW" -> DownloadSource.APK_FOLLOW
            "UPTODOWN" -> DownloadSource.UPTODOWN
            "EVOZI" -> DownloadSource.EVOZI
            else -> DownloadSource.STORE_KIT
        }
    }
}
