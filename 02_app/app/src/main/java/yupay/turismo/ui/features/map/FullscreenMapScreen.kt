package yupay.turismo.ui.features.map

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
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.lifecycle.viewmodel.compose.viewModel
import yupay.turismo.tts.SupportedLanguage
import yupay.turismo.tts.audio.AudioPlaybackViewModel
import yupay.turismo.ui.MainViewModel
import yupay.turismo.ui.components.AudioPlayerUI
import yupay.turismo.ui.components.MapSubtitles
import yupay.turismo.utils.UiTranslations

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

    // Reproductor de audio real. Owner "map" compartido con MapScreen (misma página).
    var viewMode by remember { mutableStateOf(MapViewMode.POINTS) }
    val ttsLanguage = SupportedLanguage.fromSettings(settings?.language)
    val voiceSpeed = settings?.voiceSpeed ?: 1.0f
    val owner = "map"
    val audioVm: AudioPlaybackViewModel = viewModel()
    val audio by audioVm.state.collectAsState()
    val isOwner = audio.ownerKey == owner
    val currentTime = if (isOwner) audio.positionMs else 0L
    val totalDuration = if (isOwner) audio.durationMs else 0L
    val isPlaying = isOwner && audio.isPlaying
    val isPreparing = !isOwner || audio.isPreparing
    val audioReady = isOwner && audio.ready
    val hasVoice = !isOwner || audio.hasVoice

    LaunchedEffect(currentSummary, language) { audioVm.prepare(owner, currentSummary, ttsLanguage) }
    LaunchedEffect(voiceSpeed) { audioVm.setSpeed(voiceSpeed) }
    DisposableEffect(owner) { onDispose { audioVm.releaseIfOwner(owner) } }

    val productCounts = remember(visits) {
        val counts = mutableMapOf<String, Int>()
        visits.forEach { visit ->
            visit.selectedProducts.forEach { item ->
                counts[item.name] = counts.getOrDefault(item.name, 0) + item.quantity
            }
        }
        counts.toList().sortedByDescending { it.second }.take(3)
    }

    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorSecondary = MaterialTheme.colorScheme.secondary
    val colorTertiary = MaterialTheme.colorScheme.tertiary
    val colors = listOf(colorPrimary, colorSecondary, colorTertiary)

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
            showLabels = true,
            viewMode = viewMode
        )

        // Subtítulos flotantes
        if (isPlaying) {
            MapSubtitles(
                text = currentSummary,
                currentTime = currentTime,
                durationMs = totalDuration,
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
                Icon(Icons.Default.ArrowBack, contentDescription = UiTranslations.getString(context, "map_cd_back", language), tint = MaterialTheme.colorScheme.primary)
            }

            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Mode Toggle
                IconButton(
                    onClick = { viewMode = if (viewMode == MapViewMode.POINTS) MapViewMode.BUBBLES else MapViewMode.POINTS },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (viewMode == MapViewMode.POINTS) Icons.Default.BubbleChart else Icons.Default.LocationOn,
                        contentDescription = UiTranslations.getString(context, "map_cd_mode", language),
                        tint = MaterialTheme.colorScheme.primary
                    )
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
                        productCounts.forEachIndexed { index, (name, count) ->
                            LegendItemFullScreen(name, colors.getOrElse(index) { Color.Gray }, count)
                        }
                    }
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
                    onPlayPauseClick = { audioVm.togglePlayPause() },
                    onSeek = { fraction -> audioVm.seekToFraction(fraction) },
                    onFastForward = { audioVm.forward10() },
                    onRewind = { audioVm.rewind10() },
                    compact = true,
                    modifier = Modifier.padding(8.dp),
                    language = language,
                    isPreparing = isPreparing,
                    ready = audioReady,
                    hasVoice = hasVoice
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
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
