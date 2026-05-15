package upch.mluque.final_project.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import upch.mluque.final_project.data.sync.SyncProgress
import upch.mluque.final_project.data.sync.SyncStep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncBottomSheet(
    progress: SyncProgress,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { 
            // Bloquear cierre manual (swipe/tap fuera) si está procesando
            progress.step == SyncStep.COMPLETED || progress.step == SyncStep.ERROR || progress.step == SyncStep.IDLE
        }
    )
    
    val view = LocalView.current
    LaunchedEffect(progress.step) {
        if (progress.step == SyncStep.ERROR) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            if (progress.step == SyncStep.COMPLETED || progress.step == SyncStep.ERROR) {
                BottomSheetDefaults.DragHandle()
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp, start = 24.dp, end = 24.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ícono Animado con Crossfade
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = progress.step,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "SyncIcon"
                ) { step ->
                    when (step) {
                        SyncStep.COMPLETED -> {
                            val scale = remember { Animatable(0f) }
                            LaunchedEffect(Unit) {
                                scale.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .scale(scale.value)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(40.dp), tint = Color.White)
                            }
                        }
                        SyncStep.ERROR -> {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(60.dp),
                                strokeWidth = 5.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Texto de Estado con Crossfade
            AnimatedContent(
                targetState = progress,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "SyncStatusText"
            ) { targetProgress ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val statusText = when (targetProgress.step) {
                        SyncStep.CONNECTING -> {
                            if (targetProgress.role == "RECEIVER") "Esperando conexión del otro dispositivo…"
                            else "Conectando con el receptor…"
                        }
                        SyncStep.FETCHING_PROFILE -> {
                            if (targetProgress.role == "RECEIVER") "Enviando perfil del negocio…"
                            else "Obteniendo perfil del negocio…"
                        }
                        SyncStep.RECEIVING_VISITS -> {
                            if (targetProgress.role == "RECEIVER") "Enviando visitas… (${targetProgress.processedItems} / ${targetProgress.totalItems})"
                            else "Recibiendo visitas… (${targetProgress.processedItems} / ${targetProgress.totalItems})"
                        }
                        SyncStep.COMPLETED -> "¡Sincronización completada!"
                        SyncStep.ERROR -> "Error de sincronización"
                        else -> "Iniciando…"
                    }

                    Text(
                        text = statusText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Mensaje de Error / Estado Detallado (Muy visible)
                    if (targetProgress.errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = if (targetProgress.step == SyncStep.ERROR) 
                                MaterialTheme.colorScheme.errorContainer 
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = targetProgress.errorMessage!!,
                                fontSize = 13.sp,
                                color = if (targetProgress.step == SyncStep.ERROR) 
                                    MaterialTheme.colorScheme.onErrorContainer 
                                else MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Métricas de Diagnóstico (Siempre visibles mientras no sea IDLE)
                    if (targetProgress.step != SyncStep.IDLE) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    "Diagnóstico de conexión:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                val latencyText = if (targetProgress.connectionTimeMs > 0) 
                                    "${targetProgress.connectionTimeMs}ms" 
                                else "Calculando..."
                                
                                Text(
                                    "• Latencia de red: $latencyText",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "• Intento actual: ${targetProgress.currentAttempt}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                if (targetProgress.currentAttempt > 2 && targetProgress.connectionTimeMs == 0L) {
                                    Text(
                                        "⚠ No se logra establecer conexión física",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else if (targetProgress.connectionTimeMs > 2000) {
                                    Text(
                                        "⚠ Conexión lenta detectada",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botones de Acción
            AnimatedVisibility(
                visible = progress.step == SyncStep.COMPLETED || progress.step == SyncStep.ERROR,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (progress.step == SyncStep.ERROR) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Close, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CANCELAR")
                        }
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("REINTENTAR")
                        }
                    } else if (progress.step == SyncStep.COMPLETED) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("¡LISTO!", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
