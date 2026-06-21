package yupay.turismo.ui.features.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import yupay.turismo.ui.MainViewModel
import yupay.turismo.ui.components.UnsavedChangesDialog
import yupay.turismo.utils.UiTranslations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.appSettings.collectAsState()
    val languages = listOf("Español", "Quechua", "Inglés", "Portugués")
    
    val context = LocalContext.current
    var selectedLanguage by remember(settings) { mutableStateOf(settings?.language ?: "Español") }
    val currentLanguage = settings?.language ?: "Español"
    
    var showExitDialog by remember { mutableStateOf(false) }

    val hasChanges = selectedLanguage != currentLanguage

    BackHandler(enabled = hasChanges) {
        showExitDialog = true
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(UiTranslations.getString(context, "profile_language", currentLanguage), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasChanges) showExitDialog = true else onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = UiTranslations.getString(context, "btn_back", currentLanguage))
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(bottom = 80.dp)
            ) {
                languages.forEach { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLanguage = lang }
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = lang,
                            fontSize = 18.sp,
                            fontWeight = if (selectedLanguage == lang) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedLanguage == lang) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                        if (selectedLanguage == lang) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                }
            }

            if (hasChanges) {
                Button(
                    onClick = {
                        viewModel.saveLanguage(selectedLanguage)
                        onBack()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .height(56.dp)
                ) {
                    Text(UiTranslations.getString(context, "profile_save_changes", currentLanguage), fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showExitDialog) {
            UnsavedChangesDialog(
                language = currentLanguage,
                onContinueEditing = { showExitDialog = false },
                onExitWithoutSaving = onBack,
                onDismiss = { showExitDialog = false }
            )
        }
    }
}

