package com.secureguard.mdm.vpn.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.emanuelef.remote_capture.CaptureService
import com.emanuelef.remote_capture.ConnectionsRegister
import com.emanuelef.remote_capture.interfaces.ConnectionsListener
import com.emanuelef.remote_capture.model.ConnectionDescriptor
import com.secureguard.mdm.R
import com.secureguard.mdm.ui.theme.SecureGuardTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnConnectionsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val connsRegister = remember { 
        if (CaptureService.isServiceActive()) CaptureService.getConnsRegister() else null 
    }
    
    // State for the list of connections (Thread-safe list for Compose)
    val connections = remember { mutableStateListOf<ConnectionDescriptor>() }
    
    // Update connections list via listener
    LaunchedEffect(connsRegister) {
        if (connsRegister == null) return@LaunchedEffect
        
        val listener = object : ConnectionsListener {
            override fun connectionsAdded(pos: Int, conns: Array<out ConnectionDescriptor>?) {
                conns?.forEach { conn ->
                    // Add to the top of the list (newest first)
                    connections.add(0, conn)
                }
                // Keep list size manageable
                while (connections.size > 500) {
                    connections.removeAt(connections.size - 1)
                }
            }
            
            override fun connectionsRemoved(pos: Int, conns: Array<out ConnectionDescriptor>?) {
                conns?.forEach { conn ->
                    connections.removeAll { it.incr_id == conn.incr_id }
                }
            }
            
            override fun connectionsUpdated(posIntArray: IntArray?) {
                // When connections update (bytes count, status), we need to trigger recomposition
                posIntArray?.forEach { pos ->
                    val conn = connsRegister.getConn(pos)
                    if (conn != null) {
                        val index = connections.indexOfFirst { it.incr_id == conn.incr_id }
                        if (index != -1) {
                            // Replace item to trigger Compose update
                            connections[index] = conn
                        }
                    }
                }
            }
            
            override fun connectionsChanges(numConns: Int) {
                // Full sync if needed
                if (connections.isEmpty() && numConns > 0) {
                    val list = mutableListOf<ConnectionDescriptor>()
                    for (i in 0 until connsRegister.connCount) {
                        connsRegister.getConn(i)?.let { list.add(it) }
                    }
                    connections.clear()
                    connections.addAll(list.reversed())
                }
            }
        }
        
        // Initial load
        val initialList = mutableListOf<ConnectionDescriptor>()
        for (i in 0 until connsRegister.connCount) {
            connsRegister.getConn(i)?.let { initialList.add(it) }
        }
        connections.clear()
        connections.addAll(initialList.reversed())
        
        connsRegister.addListener(listener)
        try {
            // Wait while the effect is active
            kotlinx.coroutines.awaitCancellation()
        } finally {
            connsRegister.removeListener(listener)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vpn_live_connections)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (connections.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (CaptureService.isServiceActive()) stringResource(R.string.vpn_no_active_connections) else stringResource(R.string.vpn_disconnected_msg),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(connections, key = { it.incr_id }) { conn ->
                        ConnectionItem(conn)
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionItem(conn: ConnectionDescriptor) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    
    val appInfo = remember(conn.uid) {
        val packages = packageManager.getPackagesForUid(conn.uid)
        if (packages != null && packages.isNotEmpty()) {
            try {
                packageManager.getApplicationInfo(packages[0], 0)
            } catch (e: Exception) {
                null
            }
        } else null
    }
    
    val appName = remember(appInfo) {
        appInfo?.loadLabel(packageManager)?.toString() ?: "Unknown App"
    }
    
    val appIcon = remember(appInfo) {
        appInfo?.loadIcon(packageManager)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = conn.dst_ip + (if (conn.info != null && conn.info.isNotEmpty()) " (${conn.info})" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when {
                        conn.is_blocked -> Color.Red
                        conn.isBlacklisted -> Color(0xFFFFA500) // Orange
                        else -> Color(0xFF4CAF50) // Green
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    Text(
                        text = when {
                            conn.is_blocked -> "Blocked"
                            conn.isBlacklisted -> "Malicious"
                            else -> "Allowed"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                    
                    if (conn.l7proto != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• ${conn.l7proto}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "↑ ${com.emanuelef.remote_capture.Utils.formatBytes(conn.sent_bytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "↓ ${com.emanuelef.remote_capture.Utils.formatBytes(conn.rcvd_bytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
