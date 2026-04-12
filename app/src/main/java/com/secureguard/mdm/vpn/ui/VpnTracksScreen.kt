package com.secureguard.mdm.vpn.ui

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuelef.remote_capture.CaptureService
import com.emanuelef.remote_capture.activities.PathType
import com.emanuelef.remote_capture.model.Prefs
import com.secureguard.mdm.ui.theme.SecureGuardTheme
import androidx.compose.ui.res.stringResource
import com.secureguard.mdm.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class TrackItem(
    val type: PathType,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnTracksScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    
    var currentTrack by remember { 
        mutableStateOf(prefs.getString("mode", PathType.MULTIMEDIA.name) ?: PathType.MULTIMEDIA.name) 
    }
    
    var isNetfreeEnabled by remember { mutableStateOf(prefs.getBoolean(Prefs.PREF_NETFREE, false)) }
    var isRimonEnabled by remember { mutableStateOf(prefs.getBoolean(Prefs.PREF_RIMON, false)) }
    var isNetfreeBEnabled by remember { mutableStateOf(prefs.getBoolean(Prefs.PREF_NETFREEb, false)) }
    
    // Status from CaptureService (populated via CheckNetfreeTask)
    var isNetfreeDetected by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        while(true) {
            // In a real app, you might want to expose this status via a StateFlow in CaptureService
            // For now, we simulate or read from a static field if available
            // isNetfreeDetected = CaptureService.isNetfreeDetected() 
            delay(2000)
        }
    }

    val tracks = listOf(
        TrackItem(PathType.MULTIMEDIA, stringResource(R.string.vpn_track_multimedia_name), stringResource(R.string.vpn_track_multimedia_desc), Icons.Default.FilterCenterFocus, Color(0xFF2196F3)),
        TrackItem(PathType.EVERYTHING, stringResource(R.string.vpn_track_all_name), stringResource(R.string.vpn_track_all_desc), Icons.Default.Public, Color(0xFF4CAF50)),
        TrackItem(PathType.MAPS, stringResource(R.string.vpn_track_maps_name), stringResource(R.string.vpn_track_maps_desc), Icons.Default.Map, Color(0xFFFF9800)),
        TrackItem(PathType.WAZE, stringResource(R.string.vpn_track_waze_name), stringResource(R.string.vpn_track_waze_desc), Icons.Default.Navigation, Color(0xFF00BCD4)),
        TrackItem(PathType.MAIL, stringResource(R.string.vpn_track_mail_name), stringResource(R.string.vpn_track_mail_desc), Icons.Default.Email, Color(0xFFE91E63)),
        TrackItem(PathType.NAVIGATIONMUSICAPPS, stringResource(R.string.vpn_track_nav_music_name), stringResource(R.string.vpn_track_nav_music_desc), Icons.Default.MusicNote, Color(0xFF9C27B0)),
        TrackItem(PathType.WHATSAPP, stringResource(R.string.vpn_track_whatsapp_name), stringResource(R.string.vpn_track_whatsapp_desc), Icons.Default.Chat, Color(0xFF25D366)),
        TrackItem(PathType.MANUAL, stringResource(R.string.vpn_track_manual_name), stringResource(R.string.vpn_track_manual_desc), Icons.Default.Edit, Color(0xFF795548))
    )

    SecureGuardTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.vpn_tracks_filter_title), fontWeight = FontWeight.Bold) },
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Professional Filter Section ──
                item {
                    Text(
                        stringResource(R.string.vpn_tracks_professional_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    FilterStatusCard(
                        isNetfree = isNetfreeEnabled,
                        isRimon = isRimonEnabled,
                        isNetfreeB = isNetfreeBEnabled,
                        onNetfreeToggle = { 
                            isNetfreeEnabled = it
                            prefs.edit().putBoolean(Prefs.PREF_NETFREE, it).apply()
                        },
                        onRimonToggle = {
                            isRimonEnabled = it
                            prefs.edit().putBoolean(Prefs.PREF_RIMON, it).apply()
                        },
                        onNetfreeBToggle = {
                            isNetfreeBEnabled = it
                            prefs.edit().putBoolean(Prefs.PREF_NETFREEb, it).apply()
                        }
                    )
                }

                // ── Tracks Section ──
                item {
                    Text(
                        stringResource(R.string.vpn_tracks_active_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(tracks) { track ->
                    TrackCard(
                        track = track,
                        isSelected = currentTrack == track.type.name,
                        onClick = {
                            currentTrack = track.type.name
                            prefs.edit().putString("mode", track.type.name).apply()
                            // Trigger blacklist refresh
                            CaptureService.requestBlacklistsUpdate()
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { CaptureService.requestBlacklistsUpdate() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.vpn_tracks_refresh_button), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterStatusCard(
    isNetfree: Boolean,
    isRimon: Boolean,
    isNetfreeB: Boolean,
    onNetfreeToggle: (Boolean) -> Unit,
    onRimonToggle: (Boolean) -> Unit,
    onNetfreeBToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            FilterToggleRow(
                title = stringResource(R.string.vpn_filter_netfree_title),
                subtitle = stringResource(R.string.vpn_filter_netfree_subtitle),
                isSelected = isNetfree,
                onToggle = onNetfreeToggle
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            FilterToggleRow(
                title = stringResource(R.string.vpn_filter_rimon_title),
                subtitle = stringResource(R.string.vpn_filter_rimon_subtitle),
                isSelected = isRimon,
                onToggle = onRimonToggle
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            FilterToggleRow(
                title = stringResource(R.string.vpn_filter_crt_title),
                subtitle = stringResource(R.string.vpn_filter_crt_subtitle),
                isSelected = isNetfreeB,
                onToggle = onNetfreeBToggle
            )
        }
    }
}

@Composable
fun FilterToggleRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = isSelected,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF4CAF50),
                checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun TrackCard(
    track: TrackItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "border")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(
                elevation = if (isSelected) 8.dp else 0.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = track.color.copy(alpha = 0.5f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                track.color.copy(alpha = 0.15f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(2.dp, track.color.copy(alpha = borderAlpha))
        else 
            null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(track.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = track.icon,
                    contentDescription = null,
                    tint = track.color,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) track.color else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    track.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = track.color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
