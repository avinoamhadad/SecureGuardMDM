package com.secureguard.mdm.ui.screens.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secureguard.mdm.utils.AppLogger
import com.secureguard.mdm.utils.LogEntry
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(onNavigateBack: () -> Unit) {
    val logs by AppLogger.logsFlow.collectAsState(initial = emptyList())
    var selectedCategory by remember { mutableStateOf("App") }
    var selectedLevel by remember { mutableStateOf("All") }

    val filteredLogs = remember(logs, selectedCategory, selectedLevel) {
        logs.filter {
            val matchesCategory = when (selectedCategory) {
                "App" -> !it.isVpn
                "VPN" -> it.isVpn
                else -> true // "All"
            }
            val matchesLevel = when (selectedLevel) {
                "Errors" -> it.level == "E"
                "Warnings" -> it.level == "W" || it.level == "E"
                else -> true
            }
            matchesCategory && matchesLevel
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Viewer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(
                selectedTabIndex = when (selectedCategory) {
                    "All" -> 0
                    "App" -> 1
                    else -> 2
                }
            ) {
                Tab(
                    selected = selectedCategory == "All",
                    onClick = { selectedCategory = "All" },
                    text = { Text("All Logs") }
                )
                Tab(
                    selected = selectedCategory == "App",
                    onClick = { selectedCategory = "App" },
                    text = { Text("App Only") }
                )
                Tab(
                    selected = selectedCategory == "VPN",
                    onClick = { selectedCategory = "VPN" },
                    text = { Text("VPN Only") }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Warnings", "Errors").forEach { level ->
                    FilterChip(
                        selected = selectedLevel == level,
                        onClick = { selectedLevel = level },
                        label = { Text(level) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredLogs) { log ->
                    LogItemRow(log)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
                if (filteredLogs.isEmpty()) {
                    item {
                        Text(
                            text = "No logs available.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault())

@Composable
fun LogItemRow(log: LogEntry) {
    val levelColor = when (log.level) {
        "E" -> Color(0xFFD32F2F)
        "W" -> Color(0xFFF57C00)
        "D" -> Color(0xFF1976D2)
        "V" -> Color(0xFF757575)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${dateFormat.format(Date(log.timestamp))} [${log.level}]",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = levelColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = log.tag,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = log.message,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = levelColor
        )
    }
}
