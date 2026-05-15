package upch.mluque.final_project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import upch.mluque.final_project.ui.MainViewModel
import upch.mluque.final_project.ui.components.BottomNavigationBar
import upch.mluque.final_project.ui.components.OsmMapView

@Composable
fun MapScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit
) {
    val visits by viewModel.allVisits.collectAsState()
    val settings by viewModel.appSettings.collectAsState()
    
    val currentSummary = settings?.let { it.mapSummary[it.language] ?: it.mapSummary["Español"] ?: "" } ?: ""

    val serviceCounts = remember(visits) {
        val counts = mutableMapOf(
            "Hospedaje" to 0,
            "Alimentación" to 0,
            "Artesanía" to 0
        )
        visits.forEach { visit ->
            visit.services.split(", ").forEach { service ->
                if (counts.containsKey(service)) {
                    counts[service] = counts[service]!! + 1
                }
            }
        }
        counts
    }

    // Prepare colors
    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorSecondary = MaterialTheme.colorScheme.secondary
    val colorTertiary = MaterialTheme.colorScheme.tertiary

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Mapa de Visitas",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Map Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // OSM Map View
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        OsmMapView(
                            modifier = Modifier.fillMaxSize(),
                            visits = visits,
                            isInteractive = false
                        )

                        // Fullscreen button
                        IconButton(
                            onClick = { onNavigate("fullscreen_map_action") },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Expandir",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Legend Box - Improved
                    Text(
                        text = "Leyenda de Servicios",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LegendItem("Hospedaje", colorPrimary, serviceCounts["Hospedaje"] ?: 0)
                        LegendItem("Alimentación", colorSecondary, serviceCounts["Alimentación"] ?: 0)
                        LegendItem("Artesanía", colorTertiary, serviceCounts["Artesanía"] ?: 0)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Audio Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { /* Implement TTS */ },
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Reproducir",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = if (currentSummary.isEmpty()) "Escuchar resumen del mapa" else "Resumen: $currentSummary",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color, count: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = count.toString(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
