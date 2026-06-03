package upch.mluque.final_project.ui.features.sync

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import upch.mluque.final_project.sync.SyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowQrScreen(navController: NavController, syncViewModel: SyncViewModel) {
    val isConnected by syncViewModel.isConnected.collectAsState()
    val localIp = syncViewModel.getLocalIpAddress()
    val port = 51234
    val sessionId = "SESS-${System.currentTimeMillis() % 10000}"

    LaunchedEffect(Unit) {
        syncViewModel.startServer(port)
    }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            navController.navigate("sync_status?role=SERVER&deviceName=Cliente&ip=$localIp&port=$port&sessionId=$sessionId")
        }
    }

    val jsonString = """
        {
          "ip": "$localIp",
          "port": $port,
          "sessionId": "$sessionId",
          "deviceName": "${android.os.Build.MODEL}"
        }
    """.trimIndent()

    val bitmap: Bitmap = try {
        BarcodeEncoder().encodeBitmap(jsonString, BarcodeFormat.QR_CODE, 600, 600)
    } catch (e: Exception) {
        Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vincular dispositivo") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Escanea este código desde el otro dispositivo",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Card(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(16.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Esperando conexión...", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            SuggestionChip(
                onClick = { },
                label = { Text("IP: $localIp") }
            )
            SuggestionChip(
                onClick = { },
                label = { Text("Sesión: ${sessionId.take(8)}") }
            )
        }
    }
}

