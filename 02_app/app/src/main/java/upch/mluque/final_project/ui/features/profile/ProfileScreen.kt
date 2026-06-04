package upch.mluque.final_project.ui.features.profile

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import upch.mluque.final_project.ui.MainViewModel
import upch.mluque.final_project.sync.SyncViewModel
import upch.mluque.final_project.ui.components.ConnectionRequiredDialog
import upch.mluque.final_project.ui.theme.Final_projectTheme
import upch.mluque.final_project.utils.PermissionRequester
import upch.mluque.final_project.utils.getSyncPermissions
import upch.mluque.final_project.utils.UiTranslations

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
    val language = settings?.language ?: "Español"
    val role by syncViewModel.role.collectAsState()
    val isConnected by syncViewModel.isConnected.collectAsState()
    val remoteDeviceName by syncViewModel.remoteDeviceName.collectAsState()
    val context = LocalContext.current
    
    var showVoiceSpeedModal by remember { mutableStateOf(false) }
    var triggerPermissions by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showConnectionRequiredDialog by remember { mutableStateOf(false) }

    PermissionRequester(
        permissions = getSyncPermissions(includeCamera = true),
        onAllGranted = {
            navController.navigate("scan_qr")
        },
        onDenied = { },
        explanationTitle = UiTranslations.getString(context, "perm_required_title", language),
        explanationMessage = UiTranslations.getString(context, "perm_required_desc", language),
        trigger = triggerPermissions,
        onTriggerReset = { triggerPermissions = false },
        language = language
    )

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.screenWidthDp > 600

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLandscape) {
                LandscapeProfileContent(
                    paddingValues, settings, role, isConnected, remoteDeviceName,
                    navController, onNavigateToEdit, onNavigateToLanguage,
                    onNavigateToHelp, onNavigateToPrivacy, language,
                    onVoiceSpeedClick = { showVoiceSpeedModal = true },
                    onLinkDeviceClick = { triggerPermissions = true },
                    onLogoutClick = {
                        if (isConnected) {
                            showLogoutDialog = true
                        } else {
                            showConnectionRequiredDialog = true
                        }
                    }
                )
            } else {
                PortraitProfileContent(
                    paddingValues, settings, role, isConnected, remoteDeviceName,
                    navController, onNavigateToEdit, onNavigateToLanguage,
                    onNavigateToHelp, onNavigateToPrivacy, language,
                    onVoiceSpeedClick = { showVoiceSpeedModal = true },
                    onLinkDeviceClick = { triggerPermissions = true },
                    onLogoutClick = {
                        if (isConnected) {
                            showLogoutDialog = true
                        } else {
                            showConnectionRequiredDialog = true
                        }
                    }
                )
            }

            // Indicador de Conexión en la esquina superior derecha
            if (remoteDeviceName != null) {
                Surface(
                    modifier = Modifier
                        .padding(top = 16.dp, end = 24.dp)
                        .align(Alignment.TopEnd),
                    color = if (isConnected) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isConnected) Color(0xFF4CAF50) else Color.Red)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) Color(0xFF4CAF50) else Color.Red)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isConnected) UiTranslations.getString(context, "profile_online", language) else UiTranslations.getString(context, "profile_offline", language),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) Color(0xFF2E7D32) else Color.Red
                        )
                    }
                }
            }
        }

        if (showVoiceSpeedModal) {
            VoiceSpeedModal(
                currentSpeed = settings?.voiceSpeed ?: 1.0f,
                language = language,
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
                title = { Text(UiTranslations.getString(context, "profile_logout_confirm_title", language)) },
                text = { Text(UiTranslations.getString(context, "profile_logout_confirm_desc", language)) },
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
                        Text(UiTranslations.getString(context, "profile_logout", language))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text(UiTranslations.getString(context, "btn_cancel", language))
                    }
                }
            )
        }

        if (showConnectionRequiredDialog) {
            ConnectionRequiredDialog(onDismiss = { showConnectionRequiredDialog = false })
        }
    }
}

@Composable
fun PortraitProfileContent(
    paddingValues: PaddingValues,
    settings: upch.mluque.final_project.data.local.AppSettings?,
    role: String,
    isConnected: Boolean,
    remoteDeviceName: String?,
    navController: NavController,
    onNavigateToEdit: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    language: String,
    onVoiceSpeedClick: () -> Unit,
    onLinkDeviceClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(6.dp))
        ProfileTitle(language)
        Spacer(modifier = Modifier.height(24.dp))
        ProfileHeaderCard(settings, language, onNavigateToEdit)
        Spacer(modifier = Modifier.height(32.dp))
        SettingsSection(settings, role, remoteDeviceName, language, onNavigateToLanguage, onVoiceSpeedClick, onLinkDeviceClick, onLogoutClick, navController)
        Spacer(modifier = Modifier.height(24.dp))
        InfoSection(language, onNavigateToHelp, onNavigateToPrivacy)
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun LandscapeProfileContent(
    paddingValues: PaddingValues,
    settings: upch.mluque.final_project.data.local.AppSettings?,
    role: String,
    isConnected: Boolean,
    remoteDeviceName: String?,
    navController: NavController,
    onNavigateToEdit: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    language: String,
    onVoiceSpeedClick: () -> Unit,
    onLinkDeviceClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileTitle(language)
            Spacer(modifier = Modifier.height(24.dp))
            ProfileHeaderCard(settings, language, onNavigateToEdit)
        }
        
        Spacer(modifier = Modifier.width(24.dp))
        
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            SettingsSection(settings, role, remoteDeviceName, language, onNavigateToLanguage, onVoiceSpeedClick, onLinkDeviceClick, onLogoutClick, navController)
            Spacer(modifier = Modifier.height(24.dp))
            InfoSection(language, onNavigateToHelp, onNavigateToPrivacy)
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ProfileTitle(language: String) {
    val context = LocalContext.current
    Text(
        text = UiTranslations.getString(context, "profile_title", language),
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun ProfileHeaderCard(settings: upch.mluque.final_project.data.local.AppSettings?, language: String, onNavigateToEdit: () -> Unit) {
    val context = LocalContext.current
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
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (settings?.profilePicture != null) {
                    AsyncImage(
                        model = settings?.profilePicture,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
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
                        text = UiTranslations.translateService(settings?.businessCategory ?: "", language, context),
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
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text(UiTranslations.getString(context, "profile_edit", language), fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun SettingsSection(
    settings: upch.mluque.final_project.data.local.AppSettings?,
    role: String,
    remoteDeviceName: String?,
    language: String,
    onNavigateToLanguage: () -> Unit,
    onVoiceSpeedClick: () -> Unit,
    onLinkDeviceClick: () -> Unit,
    onLogoutClick: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    SectionTitle(UiTranslations.getString(context, "profile_settings_title", language))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            SettingsItem(
                icon = Icons.Default.Language,
                title = UiTranslations.getString(context, "profile_language", language),
                value = settings?.language ?: "",
                onClick = onNavigateToLanguage
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            SettingsItem(
                icon = Icons.Default.VolumeUp,
                title = UiTranslations.getString(context, "profile_voice_speed", language),
                value = "x${settings?.voiceSpeed ?: 1.0f}",
                onClick = onVoiceSpeedClick
            )
            
            if (role == "CLIENT") {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                SettingsItem(
                    icon = Icons.Default.QrCodeScanner,
                    title = UiTranslations.getString(context, "profile_link_device", language),
                    onClick = onLinkDeviceClick
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                SettingsItem(
                    icon = Icons.Default.Devices,
                    title = if (remoteDeviceName != null) "${UiTranslations.getString(context, "profile_linked_devices", language)} (1)" else UiTranslations.getString(context, "profile_linked_devices", language),
                    onClick = { navController.navigate("linked_devices") }
                )
            } else if (role == "SERVER") {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                SettingsItem(
                    icon = Icons.Default.ExitToApp,
                    title = UiTranslations.getString(context, "profile_logout", language),
                    titleColor = Color.Red,
                    onClick = onLogoutClick
                )
            }
        }
    }
}

@Composable
fun InfoSection(language: String, onNavigateToHelp: () -> Unit, onNavigateToPrivacy: () -> Unit) {
    val context = LocalContext.current
    SectionTitle(UiTranslations.getString(context, "profile_info_title", language))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            SettingsItem(
                icon = Icons.Default.HelpOutline,
                title = UiTranslations.getString(context, "profile_help", language),
                onClick = onNavigateToHelp
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            SettingsItem(
                icon = Icons.Default.Security,
                title = UiTranslations.getString(context, "profile_privacy", language),
                onClick = onNavigateToPrivacy
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

@Preview(showBackground = true, widthDp = 800, heightDp = 480)
@Composable
fun ProfileLandscapePreview() {
    Final_projectTheme {
        LandscapeProfileContent(
            paddingValues = PaddingValues(0.dp),
            settings = null,
            role = "CLIENT",
            isConnected = true,
            remoteDeviceName = "Tablet",
            navController = androidx.navigation.compose.rememberNavController(),
            onNavigateToEdit = {},
            onNavigateToLanguage = {},
            onNavigateToHelp = {},
            onNavigateToPrivacy = {},
            language = "Español",
            onVoiceSpeedClick = {},
            onLinkDeviceClick = {},
            onLogoutClick = {}
        )
    }
}
@Composable
fun VoiceSpeedModal(
    currentSpeed: Float,
    language: String,
    onDismiss: () -> Unit,
    onSelectSpeed: (Float) -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(UiTranslations.getString(context, "profile_voice_speed", language)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
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
                Text(UiTranslations.getString(context, "btn_close", language))
            }
        }
    )
}

