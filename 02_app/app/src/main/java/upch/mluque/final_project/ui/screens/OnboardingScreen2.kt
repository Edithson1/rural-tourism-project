package upch.mluque.final_project.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.runtime.Composable

@Composable
fun OnboardingScreen2(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onNext: () -> Unit
) {
    OnboardingBase(
        title = "Mira tus resultados",
        description = "Entiende cómo le va a tu emprendimiento con gráficos sencillos y fáciles de leer.",
        icon = Icons.Default.AutoGraph,
        buttonText = "Siguiente",
        pageIndex = 1,
        selectedLanguage = selectedLanguage,
        onLanguageChange = onLanguageChange,
        onNext = onNext
    )
}
