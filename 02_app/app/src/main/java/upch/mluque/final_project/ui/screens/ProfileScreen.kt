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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import upch.mluque.final_project.ui.MainViewModel
import upch.mluque.final_project.sync.SyncViewModel
import upch.mluque.final_project.utils.PermissionRequester
import upch.mluque.final_project.utils.getSyncPermissions

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    syncViewModel: SyncViewModel,
    navController: NavController,
    onNavigateToEdit: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val settings by viewModel.appSettings.collectAsState()
    val role by syncViewModel.role.collectAsState()
    
    var showVoiceSpeedModal by remember { mutableStateOf(false) }
    var triggerPermissions by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    PermissionRequester(
        permissions = getSyncPermissions(includeCamera = true),
        onAllGranted = {
            navController.navigate("scan_qr")
        },
        onDenied = { },
        explanationTitle = "Permisos necesarios",
        explanationMessage = "Para vincular un dispositivo necesitamos acceso a la cámara y a las funciones de red.",
        trigger = triggerPermissions,
        onTriggerReset = { triggerPermissions = false }
    )

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(6.dp))
            
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
                    SettingsItem(
                        icon = Icons.Default.VolumeUp,
                        title = "Velocidad de voz",
                        value = "x${settings?.voiceSpeed ?: 1.0f}",
                        onClick = { showVoiceSpeedModal = true }
                    )
                    
                    if (role == "CLIENT") {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        SettingsItem(
                            icon = Icons.Default.QrCodeScanner,
                            title = "Vincular nuevo dispositivo",
                            onClick = { triggerPermissions = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        SettingsItem(
                            icon = Icons.Default.Devices,
                            title = "Dispositivos vinculados",
                            onClick = { navController.navigate("linked_devices") }
                        )
                    } else if (role == "SERVER") {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        SettingsItem(
                            icon = Icons.Default.ExitToApp,
                            title = "Cerrar sesión",
                            titleColor = Color.Red,
                            onClick = { showLogoutDialog = true }
                        )
                    }
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

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("¿Cerrar sesión?") },
                text = { Text("Se cerrará la sesión actual y el dispositivo volverá a la pantalla inicial. Deberás volver a vincularte para sincronizar datos.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            syncViewModel.logout {
                                navController.navigate("onboarding") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("CERRAR SESIÓN")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("CANCELAR")
                    }
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
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
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
            tint = if (titleColor == Color.Red) Color.Red else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            color = titleColor
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
