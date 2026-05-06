package com.secureguard.mdm.security

import at.favre.lib.crypto.bcrypt.BCrypt
import com.secureguard.mdm.data.local.PreferencesManager
import com.secureguard.mdm.data.repository.SettingsRepository
import com.secureguard.mdm.utils.AppLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the admin password (BCrypt hashing) plus brute-force lockout.
 *
 * Lockout schedule (after consecutive failures):
 *   3 attempts: 30 s
 *   5 attempts: 5 min
 *   7 attempts: 30 min
 *   10+ attempts: 60 min (capped)
 *
 * Failures reset to zero on the first successful verification.
 */
@Singleton
class PasswordManager @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val preferencesManager: PreferencesManager
) {
    /** Creates a BCrypt hash of [password] and persists it. */
    suspend fun createAndSavePassword(password: String) {
        val hash = BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray())
        settingsRepository.setPasswordHash(hash)
        settingsRepository.setSetupComplete(true)
        resetLockout()
    }

    /**
     * Verifies [password] against the stored hash. Honours the lockout window:
     * during a lockout this returns `false` without checking the password
     * (use [getLockoutSecondsRemaining] beforehand to display a message).
     */
    suspend fun verifyPassword(password: String): Boolean {
        if (getLockoutSecondsRemaining() > 0) {
            AppLogger.w(TAG, "Verify attempt during active lockout — denied without check.")
            return false
        }
        val hash = settingsRepository.getPasswordHash() ?: return false
        val verified = BCrypt.verifyer().verify(password.toCharArray(), hash).verified
        if (verified) {
            resetLockout()
        } else {
            recordFailure()
        }
        return verified
    }

    /** True if a password was already configured. */
    suspend fun isPasswordSet(): Boolean {
        return settingsRepository.isSetupComplete() && settingsRepository.getPasswordHash() != null
    }

    /** Seconds remaining in the current lockout window, or 0 if not locked. */
    fun getLockoutSecondsRemaining(): Long {
        val attempts = preferencesManager.loadInt(PreferencesManager.KEY_PASSWORD_FAILED_ATTEMPTS, 0)
        val lockoutMs = lockoutDurationMs(attempts)
        if (lockoutMs <= 0L) return 0L
        val lastFailure = preferencesManager.loadLong(PreferencesManager.KEY_PASSWORD_LAST_FAILURE_AT, 0L)
        val elapsed = System.currentTimeMillis() - lastFailure
        if (elapsed >= lockoutMs || elapsed < 0) return 0L
        return ((lockoutMs - elapsed) + 999) / 1000 // ceil to seconds
    }

    private fun recordFailure() {
        val attempts = preferencesManager.loadInt(PreferencesManager.KEY_PASSWORD_FAILED_ATTEMPTS, 0) + 1
        preferencesManager.saveInt(PreferencesManager.KEY_PASSWORD_FAILED_ATTEMPTS, attempts)
        preferencesManager.saveLong(PreferencesManager.KEY_PASSWORD_LAST_FAILURE_AT, System.currentTimeMillis())
        AppLogger.w(TAG, "Password verification failed ($attempts consecutive failures).")
    }

    private fun resetLockout() {
        preferencesManager.saveInt(PreferencesManager.KEY_PASSWORD_FAILED_ATTEMPTS, 0)
        preferencesManager.saveLong(PreferencesManager.KEY_PASSWORD_LAST_FAILURE_AT, 0L)
    }

    private fun lockoutDurationMs(attempts: Int): Long = when {
        attempts < 3 -> 0L
        attempts < 5 -> 30_000L
        attempts < 7 -> 5 * 60_000L
        attempts < 10 -> 30 * 60_000L
        else -> 60 * 60_000L
    }

    companion object {
        private const val TAG = "PasswordManager"
        private const val BCRYPT_COST = 12
    }
}
