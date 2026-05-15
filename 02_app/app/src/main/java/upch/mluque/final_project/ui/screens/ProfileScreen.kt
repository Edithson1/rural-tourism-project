package upch.mluque.final_project.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import upch.mluque.final_project.ui.MainViewModel
import upch.mluque.final_project.ui.components.BottomNavigationBar

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onNavigateToEdit: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val settings by viewModel.appSettings.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isSyncConnected by viewModel.isSyncConnected.collectAsState()
    
    val isReceiver = settings?.syncRole == "RECEIVER"
    
    var showVoiceSpeedModal by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (isReceiver) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Button(
                        onClick = { viewModel.disconnectSync() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DESVINCULAR DISPOSITIVO", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Mi Perfil",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Profile Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F3D3E)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (settings?.profilePicture != null) {
                            AsyncImage(
                                model = settings?.profilePicture,
                                contentDescription = "Foto de perfil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = settings?.businessName ?: "Cargando...",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = settings?.businessCategory ?: "",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onNavigateToEdit,
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, Color.LightGray),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Text("Editar", fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Settings Section
            SectionTitle("Configuración de la App")
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Language,
                        title = "Idioma",
                        value = settings?.language ?: "",
                        onClick = onNavigateToLanguage
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    SettingsToggleItem(
                        icon = Icons.Default.CloudSync,
                        title = "Sincronización",
                        wifiStatus = settings?.wifiStatus ?: 0,
                        onCheckedChange = { viewModel.updateSyncEnabled(it) },
                        checked = settings?.isSyncEnabled ?: true
                    )
                    
                    if (settings?.syncRole != "RECEIVER" || isSyncConnected) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        SettingsItem(
                            icon = Icons.Default.QrCode,
                            title = "QR de sincronización",
                            onClick = { onNavigate("sync_qr") }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        SettingsItem(
                            icon = Icons.Default.Devices,
                            title = "Dispositivo vinculado",
                            value = if (isSyncConnected) "1 conectado" else "Ninguno",
                            onClick = { onNavigate("linked_devices") }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    SettingsItem(
                        icon = Icons.Default.VolumeUp,
                        title = "Velocidad de voz",
                        value = "x${settings?.voiceSpeed ?: 1.0f}",
                        onClick = { showVoiceSpeedModal = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Information Section
            SectionTitle("Información")
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.HelpOutline,
                        title = "Ayuda y Soporte",
                        onClick = onNavigateToHelp
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    SettingsItem(
                        icon = Icons.Default.Security,
                        title = "Política de Privacidad",
                        onClick = onNavigateToPrivacy
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        if (showVoiceSpeedModal) {
            VoiceSpeedModal(
                currentSpeed = settings?.voiceSpeed ?: 1.0f,
                onDismiss = { showVoiceSpeedModal = false },
                onSelectSpeed = { 
                    viewModel.updateVoiceSpeed(it)
                    showVoiceSpeedModal = false
                }
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    value: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color(0xFF0F3D3E)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    wifiStatus: Int = 0,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color(0xFF0F3D3E)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (checked) {
                val statusText = when (wifiStatus) {
                    2 -> "Con wifi con acceso a internet"
                    1 -> "Con wifi sin acceso a internet"
                    else -> "Sin wifi"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val (wifiIcon, wifiColor) = when (wifiStatus) {
                        2 -> Icons.Default.Wifi to Color(0xFF4CAF50) // Verde: Con internet
                        1 -> Icons.Default.Wifi to Color(0xFFFF9800) // Naranja: Sin internet
                        else -> Icons.Default.WifiOff to Color(0xFFF44336) // Rojo: No wifi
                    }
                    Icon(
                        imageVector = wifiIcon,
                        contentDescription = "Estado WiFi",
                        modifier = Modifier.size(16.dp),
                        tint = wifiColor
                    )
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun VoiceSpeedModal(
    currentSpeed: Float,
    onDismiss: () -> Unit,
    onSelectSpeed: (Float) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Velocidad de voz") },
        text = {
            Column {
                val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                speeds.forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSpeed(speed) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSpeed == speed,
                            onClick = { onSelectSpeed(speed) }
                        )
                        Text(
                            text = "x$speed",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CERRAR")
            }
        }
    )
}
