package upch.mluque.final_project.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.runtime.Composable

@Composable
fun OnboardingScreen3(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onNext: () -> Unit
) {
    OnboardingBase(
        title = "Recibe consejos",
        description = "Escucha recomendaciones personalizadas en voz alta en quechua o español para mejorar tus servicios.",
        icon = Icons.Default.Lightbulb,
        buttonText = "Crear mi perfil",
        pageIndex = 2,
        selectedLanguage = selectedLanguage,
        onLanguageChange = onLanguageChange,
        onNext = onNext
    )
}
