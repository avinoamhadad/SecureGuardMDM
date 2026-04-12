package com.secureguard.mdm.security

import at.favre.lib.crypto.bcrypt.BCrypt
import com.secureguard.mdm.data.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * קלאס ייעודי לניהול מאובטח של סיסמת האפליקציה.
 * הוא אחראי על יצירת hash, אימות, ושמירת הסיסמה.
 * @param settingsRepository המאגר שבו נשמור את ה-hash.
 */
@Singleton
class PasswordManager @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    /**
     * יוצר hash מהסיסמה ושומר אותו באופן מאובטח.
     * @param password הסיסמה הנקייה שהזין המשתמש.
     */
    suspend fun createAndSavePassword(password: String) {
        // יוצר hash באמצעות אלגוריתם bcrypt עם "עוצמה" של 12 (ערך סטנדרטי)
        val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        settingsRepository.setPasswordHash(hash)
        settingsRepository.setSetupComplete(true)
    }

    /**
     * מאמת סיסמה שהוזנה מול ה-hash השמור.
     * @param password הסיסמה הנקייה לבדיקה.
     * @return true אם הסיסמה נכונה, false אחרת.
     */
    suspend fun verifyPassword(password: String): Boolean {
        val hash = settingsRepository.getPasswordHash() ?: return false
        val result = BCrypt.verifyer().verify(password.toCharArray(), hash)
        return result.verified
    }

    /**
     * בודק אם סיסמה כבר הוגדרה באפליקציה.
     */
    suspend fun isPasswordSet(): Boolean {
        return settingsRepository.isSetupComplete() && settingsRepository.getPasswordHash() != null
    }
}