package yupay.turismo.ui.features.info

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import yupay.turismo.ui.MainViewModel
import yupay.turismo.ui.components.AudioPlayerUI
import yupay.turismo.ui.components.CinemaEffectText
import yupay.turismo.utils.UiTranslations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipDetailScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    val settings by viewModel.appSettings.collectAsState()
    val language = settings?.language ?: "Español"
    val currentTip = settings?.let { it.entrepreneurTips[it.language] ?: it.entrepreneurTips["Español"] ?: "" } ?: ""

    // Estados para la simulación de audio
    var currentTime by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    val readingTimePerLine = 3000L
    val lines = remember(currentTip) { currentTip.split("\n").filter { it.isNotBlank() } }
    val totalDuration = remember(lines) { lines.size * readingTimePerLine }

    // Simulación del temporizador
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (currentTime < totalDuration) {
                delay(100)
                currentTime += 100
            }
            isPlaying = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(UiTranslations.getString(context, "tip_detail_title", language)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = UiTranslations.getString(context, "btn_back", language))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = UiTranslations.getString(context, "tip_detail_subtitle", language),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (currentTip.isNotEmpty()) {
                            CinemaEffectText(
                                text = currentTip,
                                currentTime = currentTime,
                                totalDuration = totalDuration.toLong(),
                                readingTimePerLine = readingTimePerLine
                            )
                        } else {
                            Text(
                                text = UiTranslations.getString(context, "tip_detail_empty", language),
                                fontSize = 18.sp,
                                lineHeight = 28.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                Column(
                    modifier = Modifier.weight(0.8f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AudioPlayerUI(
                        currentTime = currentTime,
                        totalDuration = totalDuration.toLong(),
                        isPlaying = isPlaying,
                        onPlayPauseClick = { isPlaying = !isPlaying },
                        onSeek = { fraction -> currentTime = (totalDuration * fraction).toLong() },
                        onFastForward = { currentTime = (currentTime + 10000L).coerceAtMost(totalDuration.toLong()) },
                        onRewind = { currentTime = (currentTime - 10000L).coerceAtLeast(0L) },
                        language = language
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = UiTranslations.getString(context, "tip_detail_subtitle", language),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Área de texto con efecto cine
                Box(modifier = Modifier.weight(1f)) {
                    if (currentTip.isNotEmpty()) {
                        CinemaEffectText(
                            text = currentTip,
                            currentTime = currentTime,
                            totalDuration = totalDuration.toLong(),
                            readingTimePerLine = readingTimePerLine
                        )
                    } else {
                        Text(
                            text = UiTranslations.getString(context, "tip_detail_empty", language),
                            fontSize = 18.sp,
                            lineHeight = 28.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Reproductor de Audio
                AudioPlayerUI(
                    currentTime = currentTime,
                    totalDuration = totalDuration.toLong(),
                    isPlaying = isPlaying,
                    onPlayPauseClick = { isPlaying = !isPlaying },
                    onSeek = { fraction -> currentTime = (totalDuration * fraction).toLong() },
                    onFastForward = { currentTime = (currentTime + 10000L).coerceAtMost(totalDuration.toLong()) },
                    onRewind = { currentTime = (currentTime - 10000L).coerceAtLeast(0L) },
                    language = language
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

