package upch.mluque.final_project.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
fun PermissionRequester(
    permissions: List<String>,
    onAllGranted: () -> Unit,
    onDenied: () -> Unit,
    explanationTitle: String,
    explanationMessage: String,
    trigger: Boolean,
    onTriggerReset: () -> Unit
) {
    val context = LocalContext.current
    var showExplanation by remember { mutableStateOf(false) }
    var showSettingsSnackbar by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.all { it.value }) {
            onAllGranted()
        } else {
            showSettingsSnackbar = true
            onDenied()
        }
        onTriggerReset()
    }

    LaunchedEffect(trigger) {
        if (trigger) {
            showExplanation = true
        }
    }

    if (showExplanation) {
        AlertDialog(
            onDismissRequest = { 
                showExplanation = false
                onTriggerReset()
            },
            title = { Text(explanationTitle) },
            text = { Text(explanationMessage) },
            confirmButton = {
                Button(onClick = {
                    showExplanation = false
                    launcher.launch(permissions.toTypedArray())
                }) {
                    Text("Conceder")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showExplanation = false
                    onTriggerReset()
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showSettingsSnackbar) {
        // En una implementación real usaríamos SnackbarHostState.
        // Para simplificar esta UI, usamos un AlertDialog informativo si se deniegan.
        AlertDialog(
            onDismissRequest = { showSettingsSnackbar = false },
            title = { Text("Permisos necesarios") },
            text = { Text("Algunos permisos son necesarios para la sincronización. Por favor, actívalos en los ajustes.") },
            confirmButton = {
                Button(onClick = {
                    showSettingsSnackbar = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Abrir Ajustes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsSnackbar = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

fun getSyncPermissions(includeCamera: Boolean): List<String> {
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.CHANGE_NETWORK_STATE
    )
    
    if (includeCamera) {
        permissions.add(Manifest.permission.CAMERA)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }
    
    return permissions
}
