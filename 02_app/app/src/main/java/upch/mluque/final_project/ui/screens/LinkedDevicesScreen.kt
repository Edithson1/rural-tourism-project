package upch.mluque.final_project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import upch.mluque.final_project.sync.SyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedDevicesScreen(navController: NavController, syncViewModel: SyncViewModel) {
    val isConnected by syncViewModel.isConnected.collectAsState()
    val remoteDeviceName by syncViewModel.remoteDeviceName.collectAsState()
    
    var showDisconnectDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dispositivos vinculados") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        if (remoteDeviceName == null) {
            EmptyState()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        DeviceItem(
                            name = remoteDeviceName ?: "Desconocido",
                            isActive = isConnected
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                    }
                }

                TextButton(
                    onClick = { showDisconnectDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cerrar sesión en este dispositivo", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Se desconectará el dispositivo vinculado y volverás al inicio.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisconnectDialog = false
                        syncViewModel.logout {
                            navController.navigate("onboarding") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Cerrar sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun DeviceItem(name: String, isActive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else Color.Gray.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                if (isActive) "Conectado ahora" else "Desconectado",
                fontSize = 12.sp,
                color = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }

        if (isActive) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.Green)
            )
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Devices,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = Color.Gray.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("No hay dispositivos vinculados", fontWeight = FontWeight.Bold, color = Color.Gray)
        Text(
            "Usa 'Vincular nuevo dispositivo' en el perfil",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}
