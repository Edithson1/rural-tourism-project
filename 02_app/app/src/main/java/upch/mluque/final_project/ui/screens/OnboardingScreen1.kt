package upch.mluque.final_project.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.runtime.Composable

@Composable
fun OnboardingScreen1(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onNext: () -> Unit
) {
    OnboardingBase(
        title = "Registra visitas sin internet",
        description = "Lleva el control de tus visitantes en cualquier lugar, incluso sin conexión a datos o Wi-Fi.",
        icon = Icons.Default.WifiOff,
        buttonText = "Comenzar",
        pageIndex = 0,
        selectedLanguage = selectedLanguage,
        onLanguageChange = onLanguageChange,
        onNext = onNext
    )
}
