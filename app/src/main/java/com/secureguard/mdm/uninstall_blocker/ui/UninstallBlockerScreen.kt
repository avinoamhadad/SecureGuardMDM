package com.secureguard.mdm.uninstall_blocker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.secureguard.mdm.R
import com.secureguard.mdm.uninstall_blocker.UninstallBlockerManager
import com.secureguard.mdm.vpn.ui.AppPickerDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UninstallBlockerScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var blockedApps by remember {
        mutableStateOf(UninstallBlockerManager.getBlockedApps(context).toList())
    }
    
    var showAppPicker by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.uninstall_blocker_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(stringResource(R.string.uninstall_blocker_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.uninstall_blocker_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(stringResource(R.string.uninstall_blocker_blocked_apps), style = MaterialTheme.typography.titleMedium)
            
            if (blockedApps.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.uninstall_blocker_no_apps), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(blockedApps) { pkg ->
                        ProtectedAppRow(pkg = pkg, onDelete = {
                            UninstallBlockerManager.setUninstallBlocked(context, pkg, false)
                            blockedApps = UninstallBlockerManager.getBlockedApps(context).toList()
                            scope.launch { 
                                snackbarHostState.showSnackbar(context.getString(R.string.uninstall_blocker_toast_unblocked, pkg)) 
                            }
                        })
                    }
                }
            }
            
            Button(
                onClick = { showAppPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.uninstall_blocker_add_app))
            }
        }
    }
    
    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onAppSelected = { pkg ->
                showAppPicker = false
                UninstallBlockerManager.setUninstallBlocked(context, pkg, true)
                blockedApps = UninstallBlockerManager.getBlockedApps(context).toList()
                scope.launch { 
                    snackbarHostState.showSnackbar(context.getString(R.string.uninstall_blocker_toast_blocked, pkg)) 
                }
            }
        )
    }
}

@Composable
fun ProtectedAppRow(pkg: String, onDelete: () -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val appInfo = remember(pkg) {
        try { pm.getApplicationInfo(pkg, 0) } catch (e: Exception) { null }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                if (appInfo != null) {
                    Image(
                        painter = rememberDrawablePainter(drawable = appInfo.loadIcon(pm)),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = appInfo.loadLabel(pm).toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = pkg,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(pkg, style = MaterialTheme.typography.bodyLarge)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
