package upch.mluque.final_project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import upch.mluque.final_project.ui.MainViewModel

data class LinkedDevice(
    val id: String,
    val name: String,
    val status: String,
    val icon: ImageVector,
    val isActive: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedDevicesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val isSyncConnected by viewModel.isSyncConnected.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    val devices = if (isSyncConnected) {
        listOf(LinkedDevice("1", "Dispositivo Receptor", "Conectado ahora", Icons.Default.Smartphone, true))
    } else {
        emptyList()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Dispositivos Vinculados", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = if (isSyncConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            }

            Text(
                text = if (isSyncConnected) 
                    "Tus datos se están sincronizando actualmente." 
                else 
                    "No hay dispositivos vinculados activos.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 24.dp),
                lineHeight = 20.sp
            )
            
            Text(
                text = "Estado: $syncStatus",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (devices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Esperando conexión...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(devices) { device ->
                        DeviceItem(device = device, onDisconnect = { viewModel.disconnectSync() })
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceItem(device: LinkedDevice, onDisconnect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = device.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = device.status,
                fontSize = 12.sp,
                color = if (device.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onDisconnect) {
            Icon(
                imageVector = Icons.Default.LinkOff,
                contentDescription = "Desvincular",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
