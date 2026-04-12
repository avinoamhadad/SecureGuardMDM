package com.secureguard.mdm.vpn.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuelef.remote_capture.CaptureService
import com.emanuelef.remote_capture.model.CaptureStats
import com.emanuelef.remote_capture.model.MatchList
import com.secureguard.mdm.R
import com.secureguard.mdm.ui.theme.SecureGuardTheme
import com.secureguard.mdm.vpn.VpnController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnControlScreen(
    onNavigateBack: () -> Unit,
    onNavigateToConnections: () -> Unit,
    onNavigateToRules: () -> Unit,
    onNavigateToTracks: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var isRunning by remember { mutableStateOf(VpnController.isVpnRunning()) }
    var isMdmExcluded by remember { mutableStateOf(VpnController.isAppExcluded(context, context.packageName)) }
    
    // Stats state
    var stats by remember { mutableStateOf(CaptureService.getStats()) }
    var malwareRulesCount by remember { mutableIntStateOf(CaptureService.getMalwareWhitelist()?.getSize() ?: 0) }
    var firewallRulesCount by remember { mutableIntStateOf((CaptureService.getBlocklist()?.getSize() ?: 0) + (CaptureService.getFirewallWhitelist()?.getSize() ?: 0)) }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            VpnController.startVpn(context)
            isRunning = true
        }
    }

    // Poll VPN state and stats
    LaunchedEffect(Unit) {
        while (true) {
            isRunning = VpnController.isVpnRunning()
            isMdmExcluded = VpnController.isAppExcluded(context, context.packageName)
            if (isRunning) {
                stats = CaptureService.getStats()
                malwareRulesCount = CaptureService.getMalwareWhitelist()?.getSize() ?: 0
                firewallRulesCount = (CaptureService.getBlocklist()?.getSize() ?: 0) + (CaptureService.getFirewallWhitelist()?.getSize() ?: 0)
            }
            delay(1500)
        }
    }

    var showConflictDialog by remember { mutableStateOf(false) }
    
    val startVpnFull = {
        // Stop App Firewall if running to avoid VPN conflict
        val stopFirewallIntent = Intent(context, com.secureguard.mdm.firewall.AppFirewallVpnService::class.java).apply {
            action = com.secureguard.mdm.firewall.AppFirewallVpnService.ACTION_STOP_FIREWALL
        }
        context.startService(stopFirewallIntent)
        android.preference.PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("firewall_vpn_active", false).apply()

        VpnController.prepareVpn(context, vpnPermissionLauncher)
        // Permission result will set isRunning = true
        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.vpn_starting)) }
    }

    val onToggle = {
        if (isRunning) {
            VpnController.stopVpn()
            isRunning = false
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.vpn_stopped)) }
        } else {
            val isFirewallActive = android.preference.PreferenceManager.getDefaultSharedPreferences(context).getBoolean("firewall_vpn_active", false)
            if (isFirewallActive) {
                showConflictDialog = true
            } else {
                startVpnFull()
            }
        }
        Unit
    }

    SecureGuardTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.vpn_control_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // MDM Protection Status
                if (isMdmExcluded) {
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.vpn_mdm_excluded),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                // ── VPN Status Hero Card ──
                VpnStatusCard(
                    isRunning = isRunning,
                    onToggle = onToggle
                )
                
                if (isRunning) {
                    // ── Live Traffic Quick Link ──
                    InfoCard(
                        icon = Icons.Default.SwapCalls,
                        title = stringResource(R.string.vpn_live_traffic),
                        subtitle = "↑ ${com.emanuelef.remote_capture.Utils.formatBytes(stats.bytes_sent)}  ↓ ${com.emanuelef.remote_capture.Utils.formatBytes(stats.bytes_rcvd)}",
                        isActive = true,
                        onClick = onNavigateToConnections
                    )
                }

                // ── Connection Info ──
                InfoCard(
                    icon = Icons.Default.Dns,
                    title = stringResource(R.string.vpn_dns_server),
                    subtitle = if (isRunning) stringResource(R.string.vpn_dns_active) else stringResource(R.string.vpn_dns_inactive),
                    isActive = isRunning
                )

                InfoCard(
                    icon = Icons.Default.Security,
                    title = stringResource(R.string.vpn_malware_detection),
                    subtitle = if (isRunning) stringResource(R.string.vpn_malware_active, malwareRulesCount) else stringResource(R.string.vpn_malware_inactive),
                    isActive = isRunning,
                    onClick = if (isRunning) onNavigateToRules else null
                )

                InfoCard(
                    icon = Icons.Default.FilterList,
                    title = stringResource(R.string.vpn_firewall_rules),
                    subtitle = if (isRunning) stringResource(R.string.vpn_firewall_active, firewallRulesCount) else stringResource(R.string.vpn_firewall_inactive),
                    isActive = isRunning,
                    onClick = if (isRunning) onNavigateToRules else null
                )

                InfoCard(
                    icon = Icons.Default.Route,
                    title = stringResource(R.string.vpn_tracks_title),
                    subtitle = stringResource(R.string.vpn_tracks_subtitle),
                    isActive = isRunning,
                    onClick = onNavigateToTracks
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        if (showConflictDialog) {
            AlertDialog(
                onDismissRequest = { showConflictDialog = false },
                title = { Text(stringResource(R.string.vpn_conflict_title)) },
                text = { Text(stringResource(R.string.vpn_conflict_msg_pcap)) },
                confirmButton = {
                    Button(onClick = {
                        showConflictDialog = false
                        startVpnFull()
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
}

@Composable
private fun VpnStatusCard(
    isRunning: Boolean,
    onToggle: () -> Unit
) {
    val statusColor by animateColorAsState(
        targetValue = if (isRunning) Color(0xFF388E3C) else MaterialTheme.colorScheme.error,
        animationSpec = tween(500),
        label = "statusColor"
    )

    val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                statusColor.copy(alpha = 0.3f),
                                statusColor.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_vpn_lock),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = statusColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isRunning) stringResource(R.string.vpn_status_protected) else stringResource(R.string.vpn_status_disconnected),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )

            Text(
                text = if (isRunning) "Your network traffic is being monitored and filtered"
                else "Tap the button below to enable network protection",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            Button(
                onClick = onToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "STOP VPN" else "START VPN",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) Color(0xFF388E3C) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                )
            }
        }
    }
}
