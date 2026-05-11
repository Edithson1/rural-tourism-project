package upch.mluque.final_project.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import upch.mluque.final_project.R
import upch.mluque.final_project.ui.MainViewModel
import upch.mluque.final_project.ui.components.BottomNavigationBar
import kotlin.random.Random

@Composable
fun MapScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit
) {
    val visits by viewModel.allVisits.collectAsState()

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

    // Prepare colors outside Canvas
    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorSecondary = MaterialTheme.colorScheme.secondary
    val colorTertiary = MaterialTheme.colorScheme.tertiary
    val colorOutline = MaterialTheme.colorScheme.outline

    Scaffold(
        bottomBar = {
            BottomNavigationBar(currentRoute = "map", onNavigate = onNavigate)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

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
                    // Map with dots
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.world),
                            contentDescription = "World Map",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            colorFilter = if (isSystemInDarkTheme()) 
                                ColorFilter.tint(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) 
                                else null
                        )

                        // Draw dots based on visits
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            visits.forEach { visit ->
                                val random = Random(visit.id.toLong())
                                // Approximate map coordinates
                                val x = random.nextFloat() * size.width * 0.8f + size.width * 0.1f
                                val y = random.nextFloat() * size.height * 0.6f + size.height * 0.2f
                                
                                val primaryService = visit.services.split(", ").firstOrNull()
                                val color = when (primaryService) {
                                    "Hospedaje" -> colorPrimary
                                    "Alimentación" -> colorSecondary
                                    "Artesanía" -> colorTertiary
                                    else -> colorOutline
                                }

                                drawCircle(
                                    color = color,
                                    radius = 4.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Legend Box
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.width(120.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            LegendItem(colorPrimary, serviceCounts["Hospedaje"] ?: 0)
                            LegendItem(colorSecondary, serviceCounts["Alimentación"] ?: 0)
                            LegendItem(colorTertiary, serviceCounts["Artesanía"] ?: 0)
                        }
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
                        text = "Escuchar resumen del mapa",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = count.toString(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
