package com.secureguard.mdm.firewall.ui

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.secureguard.mdm.firewall.AppFirewallVpnService
import com.secureguard.mdm.vpn.ui.AppPickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFirewallScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { android.preference.PreferenceManager.getDefaultSharedPreferences(context) }
    
    // Check if the service is running. We can do a rudimentary check via Android OS or keep state.
    // Since Android handles VPN lifetime, we'll keep a simple toggled state.
    var firewallEnabled by remember {
        mutableStateOf(sharedPrefs.getBoolean("firewall_vpn_active", false))
    }
    
    var blockedApps by remember {
        mutableStateOf(sharedPrefs.getStringSet(AppFirewallVpnService.PREF_FIREWALL_BLOCKED_APPS, emptySet<String>())?.toList() ?: emptyList<String>())
    }
    
    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        firewallEnabled = true
        sharedPrefs.edit().putBoolean("firewall_vpn_active", true).apply()
        context.startForegroundService(Intent(context, AppFirewallVpnService::class.java))
    }

    var showConflictDialog by remember { mutableStateOf(false) }

    val startFirewallFull = {
        firewallEnabled = true
        sharedPrefs.edit().putBoolean("firewall_vpn_active", true).apply()
        // Stop MDM VPN if running to avoid conflict
        com.secureguard.mdm.vpn.VpnController.stopVpn()
        context.startForegroundService(Intent(context, AppFirewallVpnService::class.java))
    }

    val toggleFirewall = {
        if (!firewallEnabled) {
            val isPcapRunning = com.secureguard.mdm.vpn.VpnController.isVpnRunning()
            if (isPcapRunning) {
                showConflictDialog = true
            } else {
                val intent = VpnService.prepare(context)
                if (intent != null) {
                    vpnLauncher.launch(intent)
                } else {
                    startFirewallFull()
                }
            }
        } else {
            firewallEnabled = false
            sharedPrefs.edit().putBoolean("firewall_vpn_active", false).apply()
            val stopIntent = Intent(context, AppFirewallVpnService::class.java).apply {
                action = AppFirewallVpnService.ACTION_STOP_FIREWALL
            }
            context.startService(stopIntent)
        }
    }
    
    var showAppPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_firewall_title)) },
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.firewall_enable), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.firewall_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = firewallEnabled,
                        onCheckedChange = { toggleFirewall() }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(stringResource(R.string.firewall_blocked_apps), style = MaterialTheme.typography.titleMedium)
            
            if (blockedApps.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.firewall_no_apps), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(blockedApps) { pkg ->
                        BlockedAppRow(pkg = pkg, onDelete = {
                            val set = sharedPrefs.getStringSet(AppFirewallVpnService.PREF_FIREWALL_BLOCKED_APPS, emptySet<String>())?.toMutableSet() ?: mutableSetOf<String>()
                            set.remove(pkg)
                            sharedPrefs.edit().putStringSet(AppFirewallVpnService.PREF_FIREWALL_BLOCKED_APPS, set).apply()
                            blockedApps = set.toList()
                            
                            if (firewallEnabled) {
                                context.startForegroundService(Intent(context, AppFirewallVpnService::class.java))
                            }
                        })
                    }
                }
            }
            
            Button(
                onClick = {
                    if (firewallEnabled) {
                        showAppPicker = true
                    } else {
                        // Show warning that firewall must be enabled
                        sharedPrefs.edit().putBoolean("show_firewall_req_toast", true).apply() // simple flag
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.firewall_block_app))
            }
        }
        
        if (showConflictDialog) {
            AlertDialog(
                onDismissRequest = { showConflictDialog = false },
                title = { Text(stringResource(R.string.vpn_conflict_title)) },
                text = { Text(stringResource(R.string.vpn_conflict_msg_firewall)) },
                confirmButton = {
                    Button(onClick = {
                        showConflictDialog = false
                        val intent = VpnService.prepare(context)
                        if (intent != null) {
                            vpnLauncher.launch(intent)
                        } else {
                            startFirewallFull()
                        }
                    }) {
                        Text(stringResource(R.string.vpn_conflict_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConflictDialog = false }) {
                        Text(stringResource(R.string.dialog_button_cancel))
                    }
                }
            )
        }
    }
    
    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onAppSelected = { pkg ->
                showAppPicker = false
                val set = sharedPrefs.getStringSet(AppFirewallVpnService.PREF_FIREWALL_BLOCKED_APPS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                set.add(pkg)
                sharedPrefs.edit().putStringSet(AppFirewallVpnService.PREF_FIREWALL_BLOCKED_APPS, set).apply()
                blockedApps = set.toList()
                
                if (firewallEnabled) {
                    context.startForegroundService(Intent(context, AppFirewallVpnService::class.java))
                }
            }
        )
    }
}

@Composable
fun BlockedAppRow(pkg: String, onDelete: () -> Unit) {
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
