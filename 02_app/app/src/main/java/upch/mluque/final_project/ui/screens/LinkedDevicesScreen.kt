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

data class LinkedDevice(
    val id: String,
    val name: String,
    val model: String,
    val lastSync: String,
    val isActive: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedDevicesScreen(navController: NavController) {
    // TODO: SYNC-ENGINE — reemplazar lista hardcodeada con datos reales persistidos en Room
    val devices = listOf(
        LinkedDevice("1", "Galaxy S23", "Samsung Galaxy S23", "Ahora mismo", true),
        LinkedDevice("2", "Pixel 7", "Google Pixel 7", "Hace 2 horas", false),
        LinkedDevice("3", "Mi 13", "Xiaomi Mi 13", "Hace 1 día", false)
    )

    var showDisconnectAllDialog by remember { mutableStateOf(false) }

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
        if (devices.isEmpty()) {
            EmptyState()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(devices) { device ->
                        DeviceItem(device)
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                    }
                }

                TextButton(
                    onClick = { showDisconnectAllDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cerrar todas las sesiones", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDisconnectAllDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectAllDialog = false },
            title = { Text("¿Cerrar todas las sesiones?") },
            text = { Text("Se desconectarán todos los dispositivos vinculados actualmente.") },
            confirmButton = {
                TextButton(onClick = { showDisconnectAllDialog = false }) {
                    Text("Cerrar sesiones")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectAllDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun DeviceItem(device: LinkedDevice) {
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
                    if (device.isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else Color.Gray.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = if (device.isActive) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(device.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(device.model, fontSize = 14.sp, color = Color.Gray)
            Text(
                device.lastSync,
                fontSize = 12.sp,
                color = if (device.isActive) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }

        if (device.isActive) {
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
