package yupay.turismo.ui.features.profile

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import yupay.turismo.tts.SupportedLanguage
import yupay.turismo.tts.TtsViewModel
import yupay.turismo.ui.MainViewModel
import yupay.turismo.utils.UiTranslations

/**
 * Pantalla dedicada "Modelos de voz" (accesible desde el item del perfil). Muestra el catálogo de
 * voces del idioma ACTUAL del usuario, permite descargar/activar/eliminar y escuchar un audio de
 * prueba ("hola, esta es mi voz") con cada voz ya descargada. Reutiliza [ModelCard].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceModelsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    ttsViewModel: TtsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val settings by viewModel.appSettings.collectAsState()
    val uiLanguage = settings?.language ?: "Español"
    val language = SupportedLanguage.fromSettings(settings?.language)
    val voiceSpeed = settings?.voiceSpeed ?: 1.0f

    val models = remember(language) { ttsViewModel.catalog(language) }
    val maxSize = remember(language) { ttsViewModel.maxSizeBytes(language) }

    // Detiene cualquier audio de prueba al salir de la pantalla.
    DisposableEffect(Unit) { onDispose { ttsViewModel.stop() } }

    // Toast con el espacio liberado al eliminar un modelo.
    LaunchedEffect(Unit) {
        ttsViewModel.freedSpace.collect { bytes ->
            val mb = "%.1f".format(bytes / 1024.0 / 1024.0)
            Toast.makeText(
                context,
                UiTranslations.getString(context, "voice_freed_space", uiLanguage, "$mb MB"),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(UiTranslations.getString(context, "profile_voice_models", uiLanguage)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = UiTranslations.getString(context, "btn_back", uiLanguage),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = UiTranslations.getString(context, "voice_models_subtitle", uiLanguage),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            models.forEach { info ->
                ModelCard(
                    info = info,
                    maxSizeBytes = maxSize,
                    uiLanguage = uiLanguage,
                    voiceSpeed = voiceSpeed,
                    viewModel = ttsViewModel,
                )
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
