package upch.mluque.final_project.ui.features.map

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import upch.mluque.final_project.ui.MainViewModel
import upch.mluque.final_project.ui.components.AudioPlayerUI
import upch.mluque.final_project.ui.components.MapSubtitles
import upch.mluque.final_project.utils.UiTranslations

@Composable
fun FullscreenMapScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val visits by viewModel.allVisits.collectAsState()
    val settings by viewModel.appSettings.collectAsState()
    val currentSummary = settings?.let { it.mapSummary[it.language] ?: it.mapSummary["Español"] ?: "" } ?: ""
    val context = LocalContext.current
    val language = settings?.language ?: "Español"

    // Estados para la simulación de audio
    var currentTime by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    val readingTimePerLine = 4000L
    val lines = remember(currentSummary) { currentSummary.split("\n", ". ").filter { it.isNotBlank() } }
    val totalDuration = remember(lines) { (lines.size * readingTimePerLine).coerceAtLeast(1000L) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (currentTime < totalDuration) {
                delay(100)
                currentTime += 100
            }
            isPlaying = false
        }
    }

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

    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorSecondary = MaterialTheme.colorScheme.secondary
    val colorTertiary = MaterialTheme.colorScheme.tertiary

    // Handle system back button
    BackHandler {
        val activity = context.findActivity()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onBack()
    }
    
    // Force Landscape orientation
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // OSM Map View
        OsmMapView(
            modifier = Modifier.fillMaxSize(),
            viewModel = viewModel,
            visits = visits,
            isInteractive = true,
            zoomLevel = 4.0,
            showLabels = true
        )

        // Subtítulos flotantes
        if (isPlaying) {
            MapSubtitles(
                text = lines.joinToString("\n"),
                currentTime = currentTime,
                readingTimePerLine = readingTimePerLine,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)
            )
        }

        // Top Controls: Back Button and Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            IconButton(
                onClick = {
                    val activity = context.findActivity()
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    onBack()
                },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.primary)
            }

            // Legend in fullscreen
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.width(180.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        UiTranslations.getString(context, "map_legend", language),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    LegendItemFullScreen(UiTranslations.translateService("Hospedaje", language, context), colorPrimary, serviceCounts["Hospedaje"] ?: 0)
                    LegendItemFullScreen(UiTranslations.translateService("Alimentación", language, context), colorSecondary, serviceCounts["Alimentación"] ?: 0)
                    LegendItemFullScreen(UiTranslations.translateService("Artesanía", language, context), colorTertiary, serviceCounts["Artesanía"] ?: 0)
                }
            }
        }

        // Bottom Control: Audio Player
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.width(300.dp)
            ) {
                AudioPlayerUI(
                    currentTime = currentTime,
                    totalDuration = totalDuration,
                    isPlaying = isPlaying,
                    onPlayPauseClick = { isPlaying = !isPlaying },
                    onSeek = { fraction -> currentTime = (totalDuration * fraction).toLong() },
                    onFastForward = { currentTime = (currentTime + 10000L).coerceAtMost(totalDuration) },
                    onRewind = { currentTime = (currentTime - 10000L).coerceAtLeast(0L) },
                    compact = true,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun LegendItemFullScreen(label: String, color: Color, count: Int) {
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
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

