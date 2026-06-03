package upch.mluque.final_project.ui.features.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.runtime.Composable
import upch.mluque.final_project.utils.UiTranslations

@Composable
fun OnboardingScreen3(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onNext: () -> Unit
) {
    OnboardingBase(
        title = UiTranslations.getString("onboarding_title_2", selectedLanguage),
        description = UiTranslations.getString("onboarding_desc_2", selectedLanguage),
        icon = Icons.Default.Lightbulb,
        buttonText = UiTranslations.getString("onboarding_btn_2", selectedLanguage),
        pageIndex = 2,
        selectedLanguage = selectedLanguage,
        onLanguageChange = onLanguageChange,
        onNext = onNext
    )
}

