package upch.mluque.final_project.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private data class QrData(
    val ip: String,
    val port: Int,
    val sessionId: String,
    val deviceName: String
)

@Composable
fun ScanQrScreen(navController: NavController) {
    val context = LocalContext.current
    val scanner = remember { GmsBarcodeScanning.getClient(context as Activity) }
    var scannedData by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) }

    // Trigger scanner automatically when entering the screen
    LaunchedEffect(Unit) {
        scanner.startScan()
            .addOnSuccessListener { barcode: Barcode ->
                scannedData = barcode.rawValue
            }
            .addOnFailureListener { 
                showError = true
            }
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (scannedData == null && !showError) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Abriendo escáner de Google...")
                }
            }
            
            if (showError) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error al abrir el escáner")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Regresar")
                    }
                }
            }
        }
    }

    if (scannedData != null) {
        val qrData = remember(scannedData) {
            try {
                val json = Json.parseToJsonElement(scannedData!!).jsonObject
                val ip = json["ip"]?.jsonPrimitive?.content ?: ""
                val port = json["port"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val sessionId = json["sessionId"]?.jsonPrimitive?.content ?: ""
                val deviceName = json["deviceName"]?.jsonPrimitive?.content ?: ""

                if (ip.isNotEmpty() && port != 0) {
                    QrData(ip, port, sessionId, deviceName)
                } else null
            } catch (e: Exception) {
                null
            }
        }

        if (qrData != null) {
            AlertDialog(
                onDismissRequest = {
                    scannedData = null
                    navController.popBackStack()
                },
                title = { Text("Dispositivo encontrado") },
                text = {
                    Column {
                        Text("Nombre: ${qrData.deviceName}")
                        Text("IP: ${qrData.ip}:${qrData.port}")
                        Text("Sesión: ${qrData.sessionId.take(8)}...")
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        navController.navigate("sync_status?role=CLIENT&deviceName=${qrData.deviceName}&ip=${qrData.ip}&port=${qrData.port}&sessionId=${qrData.sessionId}")
                    }) {
                        Text("Conectar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        scannedData = null
                        navController.popBackStack()
                    }) {
                        Text("Cancelar")
                    }
                }
            )
        } else if (scannedData != null) {
            LaunchedEffect(scannedData) {
                scannedData = null
                showError = true
            }
        }
    }
}
