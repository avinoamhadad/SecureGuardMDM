package com.secureguard.mdm.ui.screens.provisioning

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secureguard.mdm.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import javax.inject.Inject

sealed class ProvisioningEvent {
    data class ShowSnackbar(val message: String) : ProvisioningEvent()
}

@HiltViewModel
class ProvisioningViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<ProvisioningEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun onCopyAdbCommand() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val adbCommand = context.getString(R.string.provisioning_adb_command)
        val clip = ClipData.newPlainText("ADB Command", adbCommand)
        clipboard.setPrimaryClip(clip)
        viewModelScope.launch {
            _eventFlow.emit(ProvisioningEvent.ShowSnackbar(context.getString(R.string.provisioning_command_copied)))
        }
    }

    fun onTryRootActivation() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val command = "dpm set-device-owner ${context.packageName}/.SecureGuardDeviceAdminReceiver"
                val process = Runtime.getRuntime().exec("su")
                DataOutputStream(process.outputStream).use {
                    it.writeBytes("$command\n")
                    it.writeBytes("exit\n")
                    it.flush()
                }

                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    _eventFlow.emit(ProvisioningEvent.ShowSnackbar(context.getString(R.string.provisioning_root_success)))
                } else {
                    throw Exception("Root command failed with exit code $exitCode")
                }
            } catch (e: Exception) {
                _eventFlow.emit(ProvisioningEvent.ShowSnackbar(context.getString(R.string.provisioning_root_failed)))
            }
        }
    }
}