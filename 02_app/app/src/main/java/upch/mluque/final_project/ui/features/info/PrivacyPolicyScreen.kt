package upch.mluque.final_project.ui.features.info

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import upch.mluque.final_project.utils.UiTranslations

@Composable
fun PrivacyPolicyScreen(
    language: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    InfoTextScreen(
        title = UiTranslations.getString(context, "profile_privacy", language),
        content = UiTranslations.getString(context, "privacy_content", language),
        onBack = onBack
    )
}
