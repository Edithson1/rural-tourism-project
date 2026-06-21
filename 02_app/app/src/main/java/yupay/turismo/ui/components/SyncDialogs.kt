package yupay.turismo.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun ConnectionRequiredDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                "Conexión requerida",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "No puedes realizar esta acción mientras el otro dispositivo esté desconectado. " +
                "Por favor, asegúrate de que ambos dispositivos tengan la aplicación abierta y estén en la misma red WiFi antes de desvincular."
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("ENTENDIDO")
            }
        }
    )
}
