package yupay.turismo.ui.features.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import yupay.turismo.utils.UiTranslations

@Composable
fun OnboardingLandscapeContent(
    pagerState: PagerState,
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 64.dp, bottom = 24.dp, start = 32.dp, end = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(48.dp)
    ) {
        // Left Column: Dynamic Illustration
        // We use userScrollEnabled = true on both to allow swiping from anywhere.
        // Since they share the same pagerState, they will remain synchronized.
        Box(modifier = Modifier.weight(1f)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = true
            ) { pageIndex ->
                val icon = when (pageIndex) {
                    0 -> Icons.Default.WifiOff
                    1 -> Icons.Default.AutoGraph
                    else -> Icons.Default.Lightbulb
                }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    OnboardingIcon(icon, modifier = Modifier.aspectRatio(1f).fillMaxWidth(0.85f))
                }
            }
        }

        // Right Column: Dynamic Text (Pager) + Static Controls
        Column(
            modifier = Modifier.weight(1.2f).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Title and Description Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                userScrollEnabled = true,
                verticalAlignment = Alignment.CenterVertically
            ) { pageIndex ->
                val title = UiTranslations.getString(context, "onboarding_title_$pageIndex", selectedLanguage)
                val description = UiTranslations.getString(context, "onboarding_desc_$pageIndex", selectedLanguage)
                
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = description,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }

            // Static Controls at Bottom
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                // Indicator is below description
                OnboardingPageIndicator(pagerState = pagerState)

                Button(
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onNext()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = UiTranslations.getString(context, "onboarding_btn_${pagerState.currentPage}", selectedLanguage),
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }

                LanguageSelector(
                    selectedLanguage = selectedLanguage,
                    onLanguageChange = onLanguageChange
                )
            }
        }
    }
}
