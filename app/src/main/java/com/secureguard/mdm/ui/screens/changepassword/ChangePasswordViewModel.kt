package com.secureguard.mdm.ui.screens.changepassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secureguard.mdm.R
import com.secureguard.mdm.security.PasswordManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordUiState(
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val errorResId: Int? = null,
    val isLoading: Boolean = false
)

sealed class ChangePasswordSideEffect {
    object NavigateBack : ChangePasswordSideEffect()
    data class ShowSnackbar(val messageResId: Int) : ChangePasswordSideEffect()
}

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val passwordManager: PasswordManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<ChangePasswordSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    fun onOldPasswordChanged(value: String) = _uiState.update { it.copy(oldPassword = value, errorResId = null) }
    fun onNewPasswordChanged(value: String) = _uiState.update { it.copy(newPassword = value, errorResId = null) }
    fun onConfirmNewPasswordChanged(value: String) = _uiState.update { it.copy(confirmNewPassword = value, errorResId = null) }

    fun onSaveClicked() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.newPassword.length < 4) {
                _uiState.update { it.copy(errorResId = R.string.setup_error_password_too_short) }
                return@launch
            }
            if (state.newPassword != state.confirmNewPassword) {
                _uiState.update { it.copy(errorResId = R.string.setup_error_passwords_do_not_match) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorResId = null) }

            if (passwordManager.verifyPassword(state.oldPassword)) {
                passwordManager.createAndSavePassword(state.newPassword)
                _sideEffect.emit(ChangePasswordSideEffect.ShowSnackbar(R.string.change_password_success))
                _sideEffect.emit(ChangePasswordSideEffect.NavigateBack)
            } else {
                _uiState.update { it.copy(errorResId = R.string.change_password_error_old_password_incorrect) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}