package upch.mluque.final_project.ui.features.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import upch.mluque.final_project.utils.PermissionRequester
import upch.mluque.final_project.utils.UiTranslations
import upch.mluque.final_project.utils.getSyncPermissions

@Composable
fun OnboardingScreen1(
    navController: NavController,
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onNext: () -> Unit
) {
    var triggerPermissions by remember { mutableStateOf(false) }

    PermissionRequester(
        permissions = getSyncPermissions(includeCamera = false),
        onAllGranted = {
            navController.navigate("show_qr")
        },
        onDenied = { /* Handled by Snackbar/Dialog in PermissionRequester */ },
        explanationTitle = UiTranslations.getString("onboarding_perm_title", selectedLanguage),
        explanationMessage = UiTranslations.getString("onboarding_perm_desc", selectedLanguage),
        trigger = triggerPermissions,
        onTriggerReset = { triggerPermissions = false }
    )

    OnboardingBase(
        title = UiTranslations.getString("onboarding_title_0", selectedLanguage),
        description = UiTranslations.getString("onboarding_desc_0", selectedLanguage),
        icon = Icons.Default.WifiOff,
        buttonText = UiTranslations.getString("onboarding_btn_0", selectedLanguage),
        pageIndex = 0,
        selectedLanguage = selectedLanguage,
        onLanguageChange = onLanguageChange,
        onNext = onNext,
        extraContent = {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { triggerPermissions = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = UiTranslations.getString("onboarding_link_device", selectedLanguage),
                    fontSize = 16.sp
                )
            }
        }
    )
}

