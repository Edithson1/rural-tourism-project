package upch.mluque.final_project.ui.features.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import upch.mluque.final_project.utils.UiTranslations

@Composable
fun OnboardingScreen2(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    OnboardingBase(
        title = UiTranslations.getString(context, "onboarding_title_1", selectedLanguage),
        description = UiTranslations.getString(context, "onboarding_desc_1", selectedLanguage),
        icon = Icons.Default.AutoGraph,
        buttonText = UiTranslations.getString(context, "onboarding_btn_1", selectedLanguage),
        pageIndex = 1,
        selectedLanguage = selectedLanguage,
        onLanguageChange = onLanguageChange,
        onNext = onNext
    )
}

