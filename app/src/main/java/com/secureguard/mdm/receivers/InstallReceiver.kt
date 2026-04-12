package com.secureguard.mdm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.secureguard.mdm.R

class InstallReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        if (status == PackageInstaller.STATUS_SUCCESS) {
            val installedPackageName = intent.getStringExtra("package_name")

            // אם החבילה שהותקנה היא NoPhone, הקפץ בקשה להגדיר כברירת מחדל
            if (installedPackageName == "org.fossify.phone") {
                Toast.makeText(context, R.string.toast_nophone_installed, Toast.LENGTH_LONG).show()
                val changeDialerIntent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                changeDialerIntent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, installedPackageName)
                changeDialerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(changeDialerIntent)
            } else {
                Toast.makeText(context, R.string.update_toast_success, Toast.LENGTH_SHORT).show()
            }
        } else {
            val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
            Toast.makeText(context, context.getString(R.string.update_toast_failed) + ": " + message, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        /**
         * Returns the set of package names that are incompatible with the app store
         * (e.g. packages blocked by MDM policy or known incompatible packages).
         */
        fun getIncompatiblePackages(context: Context): Set<String> {
            // Return empty set by default - can be extended to read from settings/policy
            return emptySet()
        }
    }
}