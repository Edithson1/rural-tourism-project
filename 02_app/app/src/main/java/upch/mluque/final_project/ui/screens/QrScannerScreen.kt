package upch.mluque.final_project.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.serialization.json.Json
import upch.mluque.final_project.data.sync.SyncData
import upch.mluque.final_project.data.sync.SyncStep
import upch.mluque.final_project.ui.MainViewModel
import upch.mluque.final_project.ui.components.SyncBottomSheet

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import upch.mluque.final_project.utils.PermissionUtils

import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    viewModel: MainViewModel,
    onScanSuccess: (SyncData) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val syncProgress by viewModel.syncProgress.collectAsState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var hasPermissions by remember { mutableStateOf(PermissionUtils.hasAllPermissions(context)) }
    var isLocationEnabled by remember { mutableStateOf(PermissionUtils.isLocationEnabled(context)) }
    var isWifiEnabled by remember { mutableStateOf(PermissionUtils.isWifiEnabled(context)) }

    LaunchedEffect(Unit) {
        viewModel.resetConnectionState()
    }

    // Actualizar estados cuando la app vuelve al primer plano
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermissions = PermissionUtils.hasAllPermissions(context)
                isLocationEnabled = PermissionUtils.isLocationEnabled(context)
                isWifiEnabled = PermissionUtils.isWifiEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d("QrScanner", "Permission results: $permissions")
        hasPermissions = PermissionUtils.hasAllPermissions(context)
        isLocationEnabled = PermissionUtils.isLocationEnabled(context)
        isWifiEnabled = PermissionUtils.isWifiEnabled(context)
    }

    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()
    val scanner = GmsBarcodeScanning.getClient(context, options)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Escanear QR", fontWeight = FontWeight.Bold) },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Apunta la cámara al código QR del otro dispositivo para sincronizar.",
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                "Nota: Asegúrate de que el código esté bien iluminado.",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            val allRequirementsMet = hasPermissions && isLocationEnabled && isWifiEnabled

            if (!allRequirementsMet) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Requisitos necesarios:",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        RequirementItem("Permisos concedidos", hasPermissions)
                        RequirementItem("Ubicación activa (GPS)", isLocationEnabled)
                        RequirementItem("Wi-Fi activo", isWifiEnabled)
                    }
                }
                
                Button(
                    onClick = {
                        val allPerms = PermissionUtils.getRequiredPermissions()
                        val currentPermissions = PermissionUtils.hasAllPermissions(context)
                        
                        if (!currentPermissions) {
                            val needed = allPerms.filter { 
                                androidx.core.content.ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED 
                            }
                            Log.d("QrScanner", "Requesting permissions: $needed")
                            launcher.launch(needed.toTypedArray())
                        } else {
                            // Si el check interno dice que sí pero el estado de Compose no se enteró
                            hasPermissions = true
                            if (!isLocationEnabled) {
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            } else if (!isWifiEnabled) {
                                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    val buttonText = when {
                        !hasPermissions -> "CONCEDER PERMISOS"
                        !isLocationEnabled -> "ACTIVAR UBICACIÓN"
                        !isWifiEnabled -> "ACTIVAR WI-FI"
                        else -> "RE-VERIFICAR"
                    }
                    Text(buttonText)
                }
            } else {
                Button(
                    onClick = {
                        scanner.startScan()
                            .addOnSuccessListener { barcode ->
                                val rawValue = barcode.rawValue
                                if (rawValue == null) {
                                    Toast.makeText(context, "No se detectó contenido", Toast.LENGTH_SHORT).show()
                                    return@addOnSuccessListener
                                }
                                
                                try {
                                    val syncData = Json.decodeFromString<SyncData>(rawValue)
                                    // In this app:
                                    // EMITTER shows QR, RECEIVER scans it.
                                    // The scanner (RECEIVER) initiates the connection.
                                    viewModel.startSyncAsReceiver(syncData)
                                } catch (e: Exception) {
                                    Log.e("QrScanner", "Error decoding: ${e.message}")
                                    Toast.makeText(context, "QR no es de Yupay Turismo", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("QrScanner", "Error: ${e.message}")
                                val msg = when {
                                    e.message?.contains("canceled") == true -> "Escaneo cancelado"
                                    e.message?.contains("waiting") == true -> "Descargando módulo de escaneo... reintenta en un momento"
                                    else -> "Error al escanear: ${e.message}"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("INICIAR ESCANEO")
                }
            }
        }

        if (syncProgress.step != SyncStep.IDLE && syncProgress.step != SyncStep.CONNECTING) {
            SyncBottomSheet(
                progress = syncProgress,
                onDismiss = { 
                    viewModel.resetSyncProgress()
                    onBack() // Volver tras finalizar
                },
                onRetry = { 
                    if (syncProgress.role == "EMITTER") viewModel.retrySyncAsEmitter()
                    else viewModel.startSyncAsReceiver() 
                },
                onCancel = { viewModel.stopSync() }
            )
        }
    }
}

@Composable
fun RequirementItem(text: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (isMet) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = if (isMet) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
        )
    }
}
