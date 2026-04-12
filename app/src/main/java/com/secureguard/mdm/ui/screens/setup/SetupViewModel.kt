package com.secureguard.mdm.ui.screens.setup

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.secureguard.mdm.utils.AppLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secureguard.mdm.R
import com.secureguard.mdm.boot.BootCompletedReceiver
import com.secureguard.mdm.security.PasswordManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val passwordValue: String = "",
    val confirmPasswordValue: String = "",
    val errorResId: Int? = null, // Use resource ID for errors
    val isLoading: Boolean = false
)

sealed class SetupEvent {
    data class PasswordChanged(val value: String) : SetupEvent()
    data class ConfirmPasswordChanged(val value: String) : SetupEvent()
    object Submit : SetupEvent()
}

@HiltViewModel
class SetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val passwordManager: PasswordManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState = _uiState.asStateFlow()

    private val _setupCompleteEvent = MutableSharedFlow<Unit>()
    val setupCompleteEvent = _setupCompleteEvent.asSharedFlow()

    fun onEvent(event: SetupEvent) {
        when (event) {
            is SetupEvent.PasswordChanged -> _uiState.update { it.copy(passwordValue = event.value, errorResId = null) }
            is SetupEvent.ConfirmPasswordChanged -> _uiState.update { it.copy(confirmPasswordValue = event.value, errorResId = null) }
            SetupEvent.Submit -> submitPassword()
        }
    }

    private fun submitPassword() {
        viewModelScope.launch {
            val password = _uiState.value.passwordValue
            val confirmPassword = _uiState.value.confirmPasswordValue

            if (password.length < 4) {
                _uiState.update { it.copy(errorResId = R.string.setup_error_password_too_short) }
                return@launch
            }
            if (password != confirmPassword) {
                _uiState.update { it.copy(errorResId = R.string.setup_error_passwords_do_not_match) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorResId = null) }

            try {
                passwordManager.createAndSavePassword(password)
                enableBootReceiver()
                _setupCompleteEvent.emit(Unit)
            } catch (e: Exception) {
                // Handle potential errors from password manager
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun enableBootReceiver() {
        val receiver = ComponentName(context, BootCompletedReceiver::class.java)
        context.packageManager.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        AppLogger.i("SetupViewModel", "BootCompletedReceiver has been programmatically enabled.")
    }
}