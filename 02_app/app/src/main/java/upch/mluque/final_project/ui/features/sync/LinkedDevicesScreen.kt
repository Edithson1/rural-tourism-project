package upch.mluque.final_project.ui.features.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import upch.mluque.final_project.ui.components.ConnectionRequiredDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedDevicesScreen(navController: NavController, syncViewModel: SyncViewModel) {
    val isConnected by syncViewModel.isConnected.collectAsState()
    val remoteDeviceName by syncViewModel.remoteDeviceName.collectAsState()
    
    var showIndividualDisconnectDialog by remember { mutableStateOf(false) }
    var showMassDisconnectDialog by remember { mutableStateOf(false) }
    var showConnectionRequiredDialog by remember { mutableStateOf(false) }

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
                            isActive = isConnected,
                            onDisconnect = {
                                if (isConnected) {
                                    showIndividualDisconnectDialog = true
                                } else {
                                    showConnectionRequiredDialog = true
                                }
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (isConnected) {
                            showMassDisconnectDialog = true
                        } else {
                            showConnectionRequiredDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Cerrar todas las sesiones vinculadas", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    if (showIndividualDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showIndividualDisconnectDialog = false },
            title = { Text("¿Resetear servidor vinculado?") },
            text = { Text("Se borrarán todos los datos en el servidor '${remoteDeviceName}' y este volverá a su estado inicial. ¿Estás seguro?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showIndividualDisconnectDialog = false
                        syncViewModel.requestRemoteLogout {
                            navController.popBackStack()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("RESET REMOTO")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIndividualDisconnectDialog = false }) {
                    Text("CANCELAR")
                }
            }
        )
    }

    if (showMassDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showMassDisconnectDialog = false },
            title = { Text("¿Cerrar todas las sesiones?") },
            text = { Text("Esta acción enviará una orden de reset a todos los dispositivos conectados actualmente. No podrás deshacer esta acción.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMassDisconnectDialog = false
                        syncViewModel.disconnectAllRemotes {
                            navController.popBackStack()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("CERRAR TODO")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMassDisconnectDialog = false }) {
                    Text("CANCELAR")
                }
            }
        )
    }

    if (showConnectionRequiredDialog) {
        ConnectionRequiredDialog(onDismiss = { showConnectionRequiredDialog = false })
    }
}

@Composable
fun DeviceItem(name: String, isActive: Boolean, onDisconnect: () -> Unit) {
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
            IconButton(onClick = onDisconnect) {
                Icon(
                    Icons.Default.ArrowBack, // Usamos ArrowBack como placeholder para "sacar" o desconectar
                    contentDescription = "Desconectar",
                    tint = Color.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
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

