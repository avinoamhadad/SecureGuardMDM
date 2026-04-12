package com.secureguard.mdm.astore.data

import com.secureguard.mdm.data.local.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AstoreRepository @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    suspend fun isFirstTimeSetup(): Boolean = withContext(Dispatchers.IO) {
        preferencesManager.loadBoolean(PreferencesManager.KEY_ASTORE_FIRST_TIME_SETUP, true)
    }

    suspend fun setFirstTimeSetup(isFirstTime: Boolean) = withContext(Dispatchers.IO) {
        preferencesManager.saveBoolean(PreferencesManager.KEY_ASTORE_FIRST_TIME_SETUP, isFirstTime)
    }

    suspend fun getCustomPackageList(): Set<String> = withContext(Dispatchers.IO) {
        preferencesManager.loadStringSet("astore_custom_package_list", emptySet())
    }

    suspend fun setCustomPackageList(packages: Set<String>) = withContext(Dispatchers.IO) {
        preferencesManager.saveStringSet("astore_custom_package_list", packages)
    }
}
