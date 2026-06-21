package yupay.turismo.ui.features.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
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
import kotlinx.coroutines.delay
import yupay.turismo.ui.MainViewModel
import yupay.turismo.ui.components.AudioPlayerUI
import yupay.turismo.ui.components.MapSubtitles
import yupay.turismo.utils.UiTranslations

@Composable
fun MapScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit
) {
    val visits by viewModel.allVisits.collectAsState()
    val settings by viewModel.appSettings.collectAsState()
    val language = settings?.language ?: "Español"
    val context = LocalContext.current
    
    val currentSummary = settings?.let { it.mapSummary[it.language] ?: it.mapSummary["Español"] ?: "" } ?: ""

    // Estados para la simulación de audio
    var currentTime by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(MapViewMode.POINTS) }
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

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp && configuration.screenWidthDp > 600

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left side: Map
                Column(modifier = Modifier.weight(1.5f)) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = UiTranslations.getString(context, "map_title", language),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            OsmMapView(
                                modifier = Modifier.fillMaxSize(),
                                viewModel = viewModel,
                                visits = visits,
                                isInteractive = false,
                                showLabels = false,
                                viewMode = viewMode
                            )

                            if (isPlaying) {
                                MapSubtitles(
                                    text = lines.joinToString("\n"),
                                    currentTime = currentTime,
                                    readingTimePerLine = readingTimePerLine,
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                                )
                            }

                            IconButton(
                                onClick = { onNavigate("fullscreen_map") },
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

                            // Mode Toggle
                            IconButton(
                                onClick = { viewMode = if (viewMode == MapViewMode.POINTS) MapViewMode.BUBBLES else MapViewMode.POINTS },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (viewMode == MapViewMode.POINTS) Icons.Default.BubbleChart else Icons.Default.LocationOn,
                                    contentDescription = "Modo",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Right side: Legend and Audio
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(50.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = UiTranslations.getString(context, "map_legend", language),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            productCounts.forEachIndexed { index, (name, count) ->
                                LegendItemRow(name, colors.getOrElse(index) { Color.Gray }, count)
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = UiTranslations.getString(context, "map_summary_title", language),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            AudioPlayerUI(
                                currentTime = currentTime,
                                totalDuration = totalDuration,
                                isPlaying = isPlaying,
                                onPlayPauseClick = { isPlaying = !isPlaying },
                                onSeek = { fraction -> currentTime = (totalDuration * fraction).toLong() },
                                onFastForward = { currentTime = (currentTime + 10000L).coerceAtMost(totalDuration) },
                                onRewind = { currentTime = (currentTime - 10000L).coerceAtLeast(0L) },
                                compact = true
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = UiTranslations.getString(context, "map_title", language),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            OsmMapView(
                                modifier = Modifier.fillMaxSize(),
                                viewModel = viewModel,
                                visits = visits,
                                isInteractive = false,
                                showLabels = false,
                                viewMode = viewMode
                            )

                            if (isPlaying) {
                                MapSubtitles(
                                    text = lines.joinToString("\n"),
                                    currentTime = currentTime,
                                    readingTimePerLine = readingTimePerLine,
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                                )
                            }

                            IconButton(
                                onClick = { onNavigate("fullscreen_map") },
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

                            // Mode Toggle
                            IconButton(
                                onClick = { viewMode = if (viewMode == MapViewMode.POINTS) MapViewMode.BUBBLES else MapViewMode.POINTS },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (viewMode == MapViewMode.POINTS) Icons.Default.BubbleChart else Icons.Default.LocationOn,
                                    contentDescription = "Modo",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = UiTranslations.getString(context, "map_legend", language),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            productCounts.forEachIndexed { index, (name, count) ->
                                LegendItem(name, colors.getOrElse(index) { Color.Gray }, count)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = UiTranslations.getString(context, "map_summary_title", language),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        AudioPlayerUI(
                            currentTime = currentTime,
                            totalDuration = totalDuration,
                            isPlaying = isPlaying,
                            onPlayPauseClick = { isPlaying = !isPlaying },
                            onSeek = { fraction -> currentTime = (totalDuration * fraction).toLong() },
                            onFastForward = { currentTime = (currentTime + 10000L).coerceAtMost(totalDuration) },
                            onRewind = { currentTime = (currentTime - 10000L).coerceAtLeast(0L) },
                            compact = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItemRow(label: String, color: Color, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
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

@Composable
fun LegendItem(label: String, color: Color, count: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp).width(80.dp)
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
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Text(
            text = count.toString(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
