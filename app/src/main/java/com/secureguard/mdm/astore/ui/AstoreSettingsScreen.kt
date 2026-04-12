package com.secureguard.mdm.astore.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.secureguard.mdm.R
import com.secureguard.mdm.data.repository.SettingsRepository
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AstoreSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    var saveToDownloads by mutableStateOf(false)
        private set
    var autoInstall by mutableStateOf(true)
        private set
    var preventDoubleTap by mutableStateOf(true)
        private set
    var customPackages by mutableStateOf<Set<String>>(emptySet())
        private set
    var showAddDialog by mutableStateOf(false)
        private set

    fun load() {
        viewModelScope.launch {
            saveToDownloads = settingsRepository.getAstoreSaveToDownloads()
            autoInstall = settingsRepository.getAstoreAutoInstall()
            preventDoubleTap = settingsRepository.getAstorePreventDoubleTap()
            customPackages = settingsRepository.getAstoreCustomPackages()
        }
    }

    fun updateSaveToDownloads(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAstoreSaveToDownloads(enabled)
            saveToDownloads = enabled
        }
    }
    
    fun updateAutoInstall(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAstoreAutoInstall(enabled)
            autoInstall = enabled
        }
    }
    
    fun updatePreventDoubleTap(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAstorePreventDoubleTap(enabled)
            preventDoubleTap = enabled
        }
    }
    
    fun addCustomPackage(packageName: String) {
        viewModelScope.launch {
            val current = customPackages.toMutableSet()
            if (current.add(packageName)) {
                settingsRepository.setAstoreCustomPackages(current)
                customPackages = current
            }
        }
    }
    
    fun removeCustomPackage(packageName: String) {
        viewModelScope.launch {
            val current = customPackages.toMutableSet()
            if (current.remove(packageName)) {
                settingsRepository.setAstoreCustomPackages(current)
                customPackages = current
            }
        }
    }
    
    fun showAddDialog() {
        showAddDialog = true
    }
    
    fun hideAddDialog() {
        showAddDialog = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstoreSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AstoreSettingsViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) { viewModel.load() }
    
    var newPackageText by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.astore_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // General Settings Section
            item {
                Text(
                    text = stringResource(R.string.astore_settings_general),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
            }
            
            item {
                SettingRow(
                    title = stringResource(R.string.astore_setting_save_to_downloads),
                    subtitle = stringResource(R.string.astore_setting_save_to_downloads_desc),
                    checked = viewModel.saveToDownloads,
                    onCheckedChange = { viewModel.updateSaveToDownloads(it) }
                )
            }
            
            item {
                Spacer(Modifier.height(12.dp))
                SettingRow(
                    title = stringResource(R.string.astore_setting_auto_install),
                    subtitle = stringResource(R.string.astore_setting_auto_install_desc),
                    checked = viewModel.autoInstall,
                    onCheckedChange = { viewModel.updateAutoInstall(it) }
                )
            }
            
            item {
                Spacer(Modifier.height(12.dp))
                SettingRow(
                    title = stringResource(R.string.astore_setting_prevent_double_tap),
                    subtitle = stringResource(R.string.astore_setting_prevent_double_tap_desc),
                    checked = viewModel.preventDoubleTap,
                    onCheckedChange = { viewModel.updatePreventDoubleTap(it) }
                )
            }
            
            // Custom Packages Section
            item {
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.astore_settings_custom_packages),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { viewModel.showAddDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.astore_add_package_button))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.astore_settings_custom_packages_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
            }
            
            // Custom packages list
            if (viewModel.customPackages.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.astore_no_custom_packages),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(viewModel.customPackages.toList()) { packageName ->
                    CustomPackageItem(
                        packageName = packageName,
                        onRemove = { viewModel.removeCustomPackage(packageName) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        
        // Add package dialog
        if (viewModel.showAddDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.hideAddDialog() },
                title = { Text(stringResource(R.string.astore_add_package_title)) },
                text = {
                    TextField(
                        value = newPackageText,
                        onValueChange = { newPackageText = it },
                        placeholder = { Text(stringResource(R.string.astore_add_package_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPackageText.isNotBlank()) {
                                viewModel.addCustomPackage(newPackageText.trim())
                                newPackageText = ""
                                viewModel.hideAddDialog()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.astore_add_package_button))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideAddDialog() }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title)
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CustomPackageItem(
    packageName: String,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.astore_remove_package),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
