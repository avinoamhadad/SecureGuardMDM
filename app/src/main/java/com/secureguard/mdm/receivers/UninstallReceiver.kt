package com.secureguard.mdm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import android.widget.Toast
import com.secureguard.mdm.R

class UninstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                Log.d("UninstallReceiver", "Package uninstalled successfully.")
                Toast.makeText(context, context.getString(R.string.uninstall_success_toast), Toast.LENGTH_SHORT).show()
            }
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.e("UninstallReceiver", "Uninstall failed: $message")
                Toast.makeText(context, context.getString(R.string.uninstall_error_toast, message), Toast.LENGTH_LONG).show()
            }
        }
    }
}