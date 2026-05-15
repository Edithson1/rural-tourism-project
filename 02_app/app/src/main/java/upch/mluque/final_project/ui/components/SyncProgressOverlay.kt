package upch.mluque.final_project.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
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
import upch.mluque.final_project.data.sync.SyncProgress
import upch.mluque.final_project.data.sync.SyncStep

@Composable
fun SyncProgressOverlay(
    progress: SyncProgress,
    onDismiss: () -> Unit
) {
    // Bloquear el botón de atrás si está en proceso
    val isProcessing = progress.step != SyncStep.COMPLETED && progress.step != SyncStep.ERROR && progress.step != SyncStep.IDLE
    BackHandler(enabled = isProcessing) {
        // No hacer nada, bloqueado
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sincronizando Datos",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // Pasos de sincronización
                SyncStepItem(
                    title = "Conectando dispositivos",
                    status = getStepStatus(SyncStep.CONNECTING, progress.step)
                )
                SyncStepItem(
                    title = "Recibiendo perfil",
                    status = getStepStatus(SyncStep.FETCHING_PROFILE, progress.step)
                )
                SyncStepItem(
                    title = "Importando visitas (${progress.processedItems}/${progress.totalItems})",
                    status = getStepStatus(SyncStep.RECEIVING_VISITS, progress.step)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Barra de progreso detallada
                LinearProgressIndicator(
                    progress = { progress.percentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                )

                Text(
                    text = "${(progress.percentage * 100).toInt()}% completado",
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (progress.step == SyncStep.COMPLETED || progress.step == SyncStep.ERROR) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (progress.step == SyncStep.ERROR) 
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        else 
                            ButtonDefaults.buttonColors()
                    ) {
                        Text(if (progress.step == SyncStep.ERROR) "REINTENTAR / CERRAR" else "¡LISTO!")
                    }
                }
                
                if (progress.errorMessage != null && progress.step != SyncStep.COMPLETED) {
                    Text(
                        text = progress.errorMessage,
                        color = if (progress.step == SyncStep.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

enum class StepStatus { PENDING, IN_PROGRESS, COMPLETED, ERROR }

@Composable
fun SyncStepItem(title: String, status: StepStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconColor by animateColorAsState(
            when (status) {
                StepStatus.PENDING -> Color.Gray.copy(alpha = 0.5f)
                StepStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                StepStatus.COMPLETED -> Color(0xFF4CAF50)
                StepStatus.ERROR -> MaterialTheme.colorScheme.error
            }
        )

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            when (status) {
                StepStatus.PENDING -> Icon(Icons.Default.HourglassEmpty, null, modifier = Modifier.size(16.dp), tint = iconColor)
                StepStatus.IN_PROGRESS -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = iconColor)
                StepStatus.COMPLETED -> Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = iconColor)
                StepStatus.ERROR -> Icon(Icons.Default.Error, null, modifier = Modifier.size(16.dp), tint = iconColor)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            fontSize = 14.sp,
            color = if (status == StepStatus.PENDING) Color.Gray else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (status == StepStatus.IN_PROGRESS) FontWeight.Bold else FontWeight.Normal
        )
    }
}

fun getStepStatus(current: SyncStep, active: SyncStep): StepStatus {
    return when {
        active == SyncStep.ERROR -> if (current == active) StepStatus.ERROR else StepStatus.PENDING
        active == SyncStep.COMPLETED -> StepStatus.COMPLETED
        current == active -> StepStatus.IN_PROGRESS
        active.ordinal > current.ordinal -> StepStatus.COMPLETED
        else -> StepStatus.PENDING
    }
}
