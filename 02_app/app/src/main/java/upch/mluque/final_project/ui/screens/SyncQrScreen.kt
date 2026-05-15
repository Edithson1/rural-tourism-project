package upch.mluque.final_project.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import upch.mluque.final_project.data.sync.SyncData
import upch.mluque.final_project.data.sync.SyncStep
import upch.mluque.final_project.ui.MainViewModel
import upch.mluque.final_project.ui.components.SyncProgressOverlay

import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import upch.mluque.final_project.utils.PermissionUtils
import upch.mluque.final_project.ui.components.SyncBottomSheet
import android.provider.Settings
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncQrScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.appSettings.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var hasPermissions by remember { mutableStateOf(PermissionUtils.hasAllPermissions(context)) }
    var isLocationEnabled by remember { mutableStateOf(PermissionUtils.isLocationEnabled(context)) }
    var isWifiEnabled by remember { mutableStateOf(PermissionUtils.isWifiEnabled(context)) }

    LaunchedEffect(Unit) {
        viewModel.resetConnectionState()
    }

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
    ) { results ->
        hasPermissions = PermissionUtils.hasAllPermissions(context)
        isLocationEnabled = PermissionUtils.isLocationEnabled(context)
        isWifiEnabled = PermissionUtils.isWifiEnabled(context)
        
        // CORRECTION: Only start if emitter or explicitly receiver
        if (hasPermissions && isLocationEnabled && isWifiEnabled) {
            if (settings?.syncRole == "RECEIVER") {
                viewModel.startSyncAsReceiver()
            } else {
                viewModel.retrySyncAsEmitter() // Emitters start by creating group
            }
        }
    }
    
    val qrBitmap = remember(settings, hasPermissions, isLocationEnabled, isWifiEnabled) {
        settings?.let {
            // Emisor genera QR con su propio token, datos de perfil y nombre de dispositivo P2P
            val data = SyncData(
                serviceName = "yupay_${it.uniqueToken}", 
                token = it.uniqueToken,
                businessName = it.businessName,
                businessCategory = it.businessCategory,
                p2pDeviceName = viewModel.syncManager.thisDeviceName, // Crucial for P2P connection
                role = if (it.syncRole == "EMITTER") "EMITTER" else "RECEIVER"
            )
            generateQrCode(Json.encodeToString(data))
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (settings?.syncRole == "EMITTER") "Dispositivo Vinculado" else "Exponer QR para Sincronizar", 
                        fontWeight = FontWeight.Bold
                    ) 
                },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (settings?.syncRole == "EMITTER") 
                    "Muestra este código al dispositivo receptor para confirmar la vinculación."
                else 
                    "Pide al dispositivo que tiene tus datos que escanee este código para recibirlos.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!hasPermissions || !isLocationEnabled || !isWifiEnabled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "El Receptor también necesita estos requisitos:",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 14.sp
                        )
                        RequirementItem("Permisos concedidos", hasPermissions)
                        RequirementItem("Ubicación activa (GPS)", isLocationEnabled)
                        RequirementItem("Wi-Fi activo", isWifiEnabled)
                    }
                }
                
                Button(
                    onClick = {
                        when {
                            !hasPermissions -> launcher.launch(PermissionUtils.getRequiredPermissions().toTypedArray())
                            !isLocationEnabled -> context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            !isWifiEnabled -> context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
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
                // Status indicator when in CONNECTING phase instead of BottomSheet
                if (syncProgress.step == SyncStep.CONNECTING) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (settings?.syncRole == "EMITTER") "Iniciando grupo P2P..." else "Buscando dispositivos...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (syncProgress.step == SyncStep.IDLE) {
                    Text(
                        text = "✓ Listo para vincular",
                        fontSize = 14.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // QR Container
                Surface(
                    modifier = Modifier
                        .size(280.dp)
                        .background(Color.White, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize().padding(24.dp)
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Código QR",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = "Generando...",
                                modifier = Modifier.size(100.dp),
                                tint = Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "ID del dispositivo: ${settings?.uniqueToken ?: "..."}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        if (syncProgress.step != SyncStep.IDLE && syncProgress.step != SyncStep.CONNECTING) {
            SyncBottomSheet(
                progress = syncProgress,
                onDismiss = { 
                    viewModel.resetSyncProgress()
                    onBack()
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

fun generateQrCode(text: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
