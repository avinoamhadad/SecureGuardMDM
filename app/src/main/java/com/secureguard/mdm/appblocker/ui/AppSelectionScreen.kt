package com.secureguard.mdm.appblocker.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.secureguard.mdm.appblocker.AppBlockerEvent
import com.secureguard.mdm.appblocker.AppBlockerViewModel
import com.secureguard.mdm.appblocker.AppFilterType
import com.secureguard.mdm.appblocker.AppInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(
    viewModel: AppBlockerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddPackageDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadAllData()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("בחר אפליקציות לחסימה") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddPackageDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "הוסף חבילה ידנית")
                    }
                    FilterMenu(
                        currentFilter = uiState.currentFilter,
                        onFilterSelected = { viewModel.onEvent(AppBlockerEvent.OnFilterChanged(it)) }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.onEvent(AppBlockerEvent.OnSaveRequest)
                coroutineScope.launch { snackbarHostState.showSnackbar("השינויים נשמרו והופעלו") }
            }) {
                Icon(Icons.Default.Save, contentDescription = "שמור")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onEvent(AppBlockerEvent.OnSearchQueryChanged(it)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text("חיפוש...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                singleLine = true
            )

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = uiState.displayedAppsForSelection, key = { it.packageName }) { appInfo ->
                        AppSelectionRow(
                            appInfo = appInfo,
                            onCheckedChange = { isChecked -> viewModel.onEvent(AppBlockerEvent.OnAppSelectionChanged(appInfo.packageName, isChecked)) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showAddPackageDialog) {
        var manualPackageName by remember { mutableStateOf("") }
        var errorText by remember { mutableStateOf<String?>(null) }

        AddPackageDialog(
            packageName = manualPackageName,
            onPackageNameChange = { manualPackageName = it },
            error = errorText,
            onDismiss = { showAddPackageDialog = false },
            onConfirm = {
                val error = viewModel.addPackageManually(manualPackageName)
                if (error == null) {
                    showAddPackageDialog = false
                } else {
                    errorText = error
                }
            }
        )
    }

    if (uiState.showCriticalAppsWarning && uiState.criticalAppsDetected.isNotEmpty()) {
        CriticalAppsWarningDialog(
            criticalApps = uiState.criticalAppsDetected,
            onDismiss = { viewModel.onEvent(AppBlockerEvent.OnDismissCriticalAppsWarning) }
        )
    }
}

@Composable
private fun FilterMenu(currentFilter: AppFilterType, onFilterSelected: (AppFilterType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Icon(Icons.Default.FilterList, contentDescription = "Filter Apps") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("אפליקציות משתמש") }, onClick = { onFilterSelected(AppFilterType.USER_ONLY); expanded = false })
            DropdownMenuItem(text = { Text("אפליקציות מסך הבית") }, onClick = { onFilterSelected(AppFilterType.LAUNCHER_ONLY); expanded = false })
            DropdownMenuItem(text = { Text("כל האפליקציות (למעט ליבה)") }, onClick = { onFilterSelected(AppFilterType.ALL_EXCEPT_CORE); expanded = false })
            DropdownMenuItem(text = { Text("הצג הכל") }, onClick = { onFilterSelected(AppFilterType.ALL); expanded = false })
        }
    }
}

@Composable
fun AddPackageDialog(
    packageName: String,
    onPackageNameChange: (String) -> Unit,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("הוסף חבילה ידנית") },
        text = {
            Column {
                OutlinedTextField(
                    value = packageName,
                    onValueChange = onPackageNameChange,
                    label = { Text("com.example.app") },
                    singleLine = true,
                    isError = error != null
                )
                if (error != null) {
                    Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("הוסף וחסימה") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } }
    )
}

@Composable
private fun AppSelectionRow(appInfo: AppInfo, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!appInfo.isBlocked) }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painter = rememberDrawablePainter(drawable = appInfo.icon), contentDescription = null, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = appInfo.appName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(16.dp))
        Checkbox(checked = appInfo.isBlocked, onCheckedChange = { onCheckedChange(it) })
    }
}

@Composable
private fun CriticalAppsWarningDialog(
    criticalApps: List<AppInfo>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("אזהרה: אפליקציות חשובות") },
        text = {
            Column {
                Text("המהלך צפוי לגרום למכשירך להרס בלתי הפיך, מכיוון שזאת אפליקציה חשובה מדי!")
                Spacer(modifier = Modifier.height(8.dp))
                Text("בקרוב יהיה אפשר לחסום גם את זה בבטחה!")
                Spacer(modifier = Modifier.height(16.dp))
                Text("אפליקציות שנבחרו:", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                criticalApps.forEach { app ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painter = rememberDrawablePainter(drawable = app.icon), contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(app.appName, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("הבנתי")
            }
        }
    )
}
