package upch.mluque.final_project.ui.features.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import upch.mluque.final_project.ui.theme.Final_projectTheme

@Composable
fun OnboardingScreen(
    pageIndex: Int,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onNext: () -> Unit
) {
    val title = when (pageIndex) {
        0 -> "Registra visitas sin internet"
        1 -> "Mira tus resultados"
        else -> "Recibe consejos"
    }

    val description = when (pageIndex) {
        0 -> "Lleva el control de tus visitantes en cualquier lugar, incluso sin conexión a datos o Wi-Fi."
        1 -> "Entiende cómo le va a tu emprendimiento con gráficos sencillos y fáciles de leer."
        else -> "Escucha recomendaciones personalizadas en voz alta en quechua o español para mejorar tus servicios."
    }

    val icon = when (pageIndex) {
        0 -> Icons.Default.WifiOff
        1 -> Icons.Default.AutoGraph
        else -> Icons.Default.Lightbulb
    }

    val buttonText = when (pageIndex) {
        0 -> "Comenzar"
        1 -> "Siguiente"
        else -> "Crear mi perfil"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Illustration Area
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
 {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = description,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Page Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pageIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = buttonText,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Español",
                    fontSize = 14.sp,
                    fontWeight = if (selectedLanguage == "Español") FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedLanguage == "Español") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { onLanguageSelected("Español") }
                )
                Text(
                    text = "|",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                )
                Text(
                    text = "Quechua",
                    fontSize = 14.sp,
                    fontWeight = if (selectedLanguage == "Quechua") FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedLanguage == "Quechua") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { onLanguageSelected("Quechua") }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingPreview0() {
    Final_projectTheme {
        OnboardingScreen(
            pageIndex = 0,
            selectedLanguage = "Español",
            onLanguageSelected = {},
            onNext = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingPreview1() {
    Final_projectTheme {
        OnboardingScreen(
            pageIndex = 1,
            selectedLanguage = "Español",
            onLanguageSelected = {},
            onNext = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingPreview2() {
    Final_projectTheme {
        OnboardingScreen(
            pageIndex = 2,
            selectedLanguage = "Español",
            onLanguageSelected = {},
            onNext = {}
        )
    }
}

