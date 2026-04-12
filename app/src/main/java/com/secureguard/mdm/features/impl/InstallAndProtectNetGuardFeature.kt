package com.secureguard.mdm.features.impl

import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.secureguard.mdm.R
import com.secureguard.mdm.features.api.ProtectionFeature
import com.secureguard.mdm.receivers.InstallReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object InstallAndProtectNetGuardFeature : ProtectionFeature {
    override val id: String = "install_protect_netguard"
    override val titleRes: Int = R.string.feature_install_netguard_title
    override val descriptionRes: Int = R.string.feature_install_netguard_description
    override val iconRes: Int = R.drawable.ic_netguard_shield

    private const val NETGUARD_PACKAGE_NAME = "eu.faircode.netguard"
    private const val NETGUARD_ASSET_NAME = "netguard.apk"
    private const val TAG = "NetGuardFeature"

    override fun applyPolicy(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enable: Boolean) {
        if (enable) {
            CoroutineScope(Dispatchers.IO).launch {
                if (!isAppInstalled(context, NETGUARD_PACKAGE_NAME)) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, R.string.toast_installing_netguard, Toast.LENGTH_SHORT).show()
                    }
                    installNetGuardApp(context)
                }
                try {
                    dpm.setUninstallBlocked(admin, NETGUARD_PACKAGE_NAME, true)
                    Log.d(TAG, "Successfully blocked uninstall for NetGuard.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to block uninstall for NetGuard.", e)
                }
            }
        } else {
            // -- התיקון כאן: הסרת ה-Toast --
            // הלוגיקה הזו מבוצעת בכל שמירה, ולכן ההודעה צריכה להיות מנוהלת ע"י ה-ViewModel
            // שיודע מתי באמת התבצע שינוי במצב.
            try {
                dpm.setUninstallBlocked(admin, NETGUARD_PACKAGE_NAME, false)
                Log.d(TAG, "Successfully unblocked uninstall for NetGuard.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unblock uninstall for NetGuard.", e)
            }
        }
    }

    override fun isPolicyActive(context: Context, dpm: DevicePolicyManager, admin: ComponentName): Boolean {
        return isAppInstalled(context, NETGUARD_PACKAGE_NAME) && dpm.isUninstallBlocked(admin, NETGUARD_PACKAGE_NAME)
    }

    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun installNetGuardApp(context: Context) {
        try {
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            context.assets.open(NETGUARD_ASSET_NAME).use { assetStream ->
                session.openWrite("netguard_install_session", 0, -1).use { sessionStream ->
                    assetStream.copyTo(sessionStream)
                    session.fsync(sessionStream)
                }
            }
            val intent = Intent(context, InstallReceiver::class.java)

            val pendingIntent = PendingIntent.getBroadcast(
                context, sessionId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            )

            session.commit(pendingIntent.intentSender)
            session.close()
            Log.d(TAG, "NetGuard installation session committed.")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to install NetGuard app from assets.", e)
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Failed to install NetGuard: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}