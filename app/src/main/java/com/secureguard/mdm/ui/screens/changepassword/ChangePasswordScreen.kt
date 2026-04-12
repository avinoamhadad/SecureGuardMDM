package com.secureguard.mdm.ui.screens.changepassword

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.secureguard.mdm.R
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    viewModel: ChangePasswordViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collectLatest { effect ->
            when(effect) {
                is ChangePasswordSideEffect.NavigateBack -> onNavigateBack()
                is ChangePasswordSideEffect.ShowSnackbar -> {
                    Toast.makeText(context, context.getString(effect.messageResId), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.change_password_title)) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            OutlinedTextField(
                value = uiState.oldPassword, onValueChange = viewModel::onOldPasswordChanged,
                label = { Text(stringResource(id = R.string.change_password_old_password)) }, isError = uiState.errorResId != null,
                visualTransformation = if (oldPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    val image = if (oldPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { oldPasswordVisible = !oldPasswordVisible }) {
                        Icon(imageVector = image, contentDescription = if (oldPasswordVisible) "Hide password" else "Show password")
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.newPassword, onValueChange = viewModel::onNewPasswordChanged,
                label = { Text(stringResource(id = R.string.change_password_new_password)) }, isError = uiState.errorResId != null,
                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    val image = if (newPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                        Icon(imageVector = image, contentDescription = if (newPasswordVisible) "Hide password" else "Show password")
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.confirmNewPassword, onValueChange = viewModel::onConfirmNewPasswordChanged,
                label = { Text(stringResource(id = R.string.change_password_confirm_new_password)) }, isError = uiState.errorResId != null,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(imageVector = image, contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password")
                    }
                }
            )
            uiState.errorResId?.let {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(id = it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::onSaveClicked, enabled = !uiState.isLoading, modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else Text(stringResource(id = R.string.change_password_button_save))
            }
        }
    }
}