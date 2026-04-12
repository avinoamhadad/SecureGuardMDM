package com.secureguard.mdm.vpn.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.emanuelef.remote_capture.CaptureService
import com.emanuelef.remote_capture.Log
import com.emanuelef.remote_capture.model.MatchList
import com.emanuelef.remote_capture.model.ListInfo
import com.secureguard.mdm.R
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.launch

data class RuleUiModel(
    val typeName: String,
    val value: String,
    val originalRule: MatchList.Rule? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnRulesScreen(
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.vpn_tab_malware), stringResource(R.string.vpn_tab_whitelist), stringResource(R.string.vpn_tab_blacklist))
    
    val context = LocalContext.current
    val sharedPrefs = remember { android.preference.PreferenceManager.getDefaultSharedPreferences(context) }
    
    val malwareList = remember { CaptureService.getMalwareWhitelist() }
    val whitelist = remember { CaptureService.getFirewallWhitelist() }
    val blocklist = remember { CaptureService.getBlocklist() }
    
    // Track VPN exceptions for the true "Bypass" Whitelist
    var vpnExceptions by remember {
        mutableStateOf(
            sharedPrefs.getStringSet(com.emanuelef.remote_capture.model.Prefs.PREF_VPN_EXCEPTIONS, emptySet())?.toList() ?: emptyList()
        )
    }
    
    // State to trigger recomposition when rules change
    var refreshKey by remember { mutableIntStateOf(0) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showAddRuleDialog by remember { mutableStateOf(false) }
    var showAppPickerDialog by remember { mutableStateOf(false) }
    
    val currentMatchList = when(selectedTab) {
        0 -> malwareList
        1 -> whitelist
        2 -> blocklist
        else -> null
    }
    
    val currentListType = when(selectedTab) {
        0 -> ListInfo.Type.MALWARE_WHITELIST
        else -> ListInfo.Type.BLOCKLIST
    }
    
    // Helper to reload the native VPN engine rules
    val reloadCurrentRules = {
        ListInfo(currentListType).reloadRules()
    }

    var showWhitelistTypeDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vpn_manage_rules)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                if (selectedTab == 1) {
                    showWhitelistTypeDialog = true
                } else {
                    showAddRuleDialog = true 
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            if (currentMatchList == null && selectedTab != 1) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.vpn_service_not_running))
                }
            } else {
                val rulesList = remember(currentMatchList, refreshKey, vpnExceptions, selectedTab) {
                    if (selectedTab == 1) {
                        val list = mutableListOf<RuleUiModel>()
                        vpnExceptions.forEach { list.add(RuleUiModel(context.getString(R.string.vpn_bypass_true), it, null)) }
                        whitelist?.let { wl ->
                            val it = wl.iterRules()
                            while(it.hasNext()) {
                                val rule = it.next()
                                list.add(RuleUiModel(rule.type.name + " (" + context.getString(R.string.vpn_bypass_engine) + ")" , rule.value.toString(), rule))
                            }
                        }
                        list
                    } else {
                        val list = mutableListOf<RuleUiModel>()
                        currentMatchList?.let {
                            val it = it.iterRules()
                            while(it.hasNext()) {
                                val rule = it.next()
                                list.add(RuleUiModel(rule.type.name, rule.value.toString(), rule))
                            }
                        }
                        list
                    }
                }
                
                if (rulesList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            Text(stringResource(R.string.vpn_no_rules_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(rulesList) { rule ->
                            RuleItem(
                                rule = rule,
                                onDelete = {
                                    if (selectedTab == 1) {
                                        // It's a VPN App Bypass exception
                                        val packageName = rule.value.toString()
                                        if (packageName == context.packageName) {
                                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.vpn_cannot_remove_mdm)) }
                                        } else {
                                            val exceptions = sharedPrefs.getStringSet(com.emanuelef.remote_capture.model.Prefs.PREF_VPN_EXCEPTIONS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                                            exceptions.remove(packageName)
                                            sharedPrefs.edit().putStringSet(com.emanuelef.remote_capture.model.Prefs.PREF_VPN_EXCEPTIONS, exceptions).apply()
                                            vpnExceptions = exceptions.toList()
                                            com.secureguard.mdm.vpn.VpnController.restartVpn(context)
                                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.vpn_app_removed)) }
                                        }
                                    } else if (rule.originalRule != null) {
                                        currentMatchList?.removeRule(rule.originalRule)
                                        currentMatchList?.save()
                                        reloadCurrentRules()
                                        refreshKey++
                                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.vpn_rule_removed)) }
                                    }
                                    Unit
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showWhitelistTypeDialog) {
        android.app.AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.vpn_whitelist_type_title))
            .setMessage(context.getString(R.string.vpn_whitelist_type_msg))
            .setPositiveButton(context.getString(R.string.vpn_whitelist_entire)) { _, _ ->
                showWhitelistTypeDialog = false
                showAppPickerDialog = true
            }
            .setNegativeButton(context.getString(R.string.vpn_whitelist_engine)) { _, _ ->
                showWhitelistTypeDialog = false
                showAddRuleDialog = true
            }
            .setOnCancelListener { showWhitelistTypeDialog = false }
            .show()
    }
    
    if (showAddRuleDialog && currentMatchList != null) {
        AddRuleDialog(
            onDismiss = { showAddRuleDialog = false },
            onAdd = { type, value ->
                when(type) {
                    MatchList.RuleType.HOST -> currentMatchList.addHost(value)
                    MatchList.RuleType.IP -> currentMatchList.addIp(value)
                    MatchList.RuleType.APP -> currentMatchList.addApp(value)
                    else -> {}
                }
                currentMatchList.save()
                reloadCurrentRules()
                
                refreshKey++
                showAddRuleDialog = false
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.vpn_rule_added)) }
                Unit
            },
            onOpenAppPicker = { showAppPickerDialog = true }
        )
    }

    if (showAppPickerDialog) {
        AppPickerDialog(
            onDismiss = { showAppPickerDialog = false },
            onAppSelected = { packageName ->
                if (selectedTab == 1) {
                    val exceptions = sharedPrefs.getStringSet(com.emanuelef.remote_capture.model.Prefs.PREF_VPN_EXCEPTIONS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                    exceptions.add(packageName)
                    sharedPrefs.edit().putStringSet(com.emanuelef.remote_capture.model.Prefs.PREF_VPN_EXCEPTIONS, exceptions).apply()
                    vpnExceptions = exceptions.toList()
                    com.secureguard.mdm.vpn.VpnController.restartVpn(context)
                    showAppPickerDialog = false
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.vpn_app_whitelisted, packageName)) }
                } else {
                    currentMatchList?.addApp(packageName)
                    currentMatchList?.save()
                    reloadCurrentRules()
                    
                    refreshKey++
                    showAppPickerDialog = false
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.vpn_app_rule_added, packageName)) }
                }
                Unit
            }
        )
    }
}

@Composable
fun RuleItem(rule: RuleUiModel, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rule.typeName.first().toString(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.value,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1
                )
                Text(
                    text = rule.typeName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onAdd: (MatchList.RuleType, String) -> Unit,
    onOpenAppPicker: () -> Unit
) {
    var type by remember { mutableStateOf(MatchList.RuleType.HOST) }
    var value by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Rule") },
        text = {
            Column {
                Text("Type", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterChip(
                        selected = type == MatchList.RuleType.HOST,
                        onClick = { type = MatchList.RuleType.HOST },
                        label = { Text("Host") }
                    )
                    FilterChip(
                        selected = type == MatchList.RuleType.IP,
                        onClick = { type = MatchList.RuleType.IP },
                        label = { Text("IP") }
                    )
                    FilterChip(
                        selected = type == MatchList.RuleType.APP,
                        onClick = { type = MatchList.RuleType.APP },
                        label = { Text("App") }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (type == MatchList.RuleType.APP) {
                    Button(
                        onClick = onOpenAppPicker,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick App from List")
                    }
                    
                    Text(
                        "OR enter package name manually:",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(if (type == MatchList.RuleType.APP) "Package Name" else "Value") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (value.isNotBlank()) onAdd(type, value) },
                enabled = value.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
@Composable
fun AppPickerDialog(
    onDismiss: () -> Unit,
    onAppSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    var apps by remember { mutableStateOf(emptyList<ApplicationInfo>()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf(0) } // 0: User, 1: Launcher, 2: All
    
    LaunchedEffect(Unit) {
        apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        isLoading = false
    }

    val filteredApps = remember(apps, searchQuery, filterType) {
        apps.filter { app ->
            val matchesSearch = if (searchQuery.isBlank()) true 
                               else app.loadLabel(packageManager).toString().contains(searchQuery, ignoreCase = true) || 
                                    app.packageName.contains(searchQuery, ignoreCase = true)
            
            val matchesFilter = when(filterType) {
                0 -> (app.flags and ApplicationInfo.FLAG_SYSTEM == 0) || (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0)
                1 -> packageManager.getLaunchIntentForPackage(app.packageName) != null
                else -> true
            }
            
            matchesSearch && matchesFilter
        }.sortedBy { it.loadLabel(packageManager).toString().lowercase() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    stringResource(R.string.app_picker_title), 
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.app_picker_search_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        R.string.app_filter_user,
                        R.string.app_filter_launcher,
                        R.string.app_filter_all
                    ).forEachIndexed { index, resId ->
                        FilterChip(
                            selected = filterType == index,
                            onClick = { filterType = index },
                            label = { 
                                Text(
                                    stringResource(resId), 
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1
                                ) 
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            AppPickerRow(app, onAppSelected)
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.dialog_button_cancel))
                    }
                }
            }
        }
    }
}

@Composable
fun AppPickerRow(app: ApplicationInfo, onAppSelected: (String) -> Unit) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    
    // Efficiently load label and icon
    val label = remember(app.packageName) { app.loadLabel(packageManager).toString() }
    
    Surface(
        onClick = { onAppSelected(app.packageName) },
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = remember(app.packageName) { app.loadIcon(packageManager) }
            Image(
                painter = rememberDrawablePainter(drawable = icon),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
