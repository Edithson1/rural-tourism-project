package upch.mluque.final_project.ui.features.visits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import upch.mluque.final_project.data.local.Visit
import upch.mluque.final_project.ui.MainViewModel
import upch.mluque.final_project.utils.UiTranslations
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitDetailScreen(
    visitId: Int,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var visit by remember { mutableStateOf<Visit?>(null) }
    val settings by viewModel.appSettings.collectAsState()
    val language = settings?.language ?: "Español"
    
    LaunchedEffect(visitId) {
        visit = viewModel.getVisitDetail(visitId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(UiTranslations.getString("visits_detail_title", language)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        visit?.let { v ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with icon and name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(v.nationalityFlag, fontSize = 32.sp)
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(
                            text = v.nationality,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = UiTranslations.getString("tourist_label", language),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                DetailItem(UiTranslations.getString("visits_price", language), v.priceApprox)
                DetailItem(UiTranslations.getString("visits_services", language), UiTranslations.translateServicesList(v.services, language))
                DetailItem(UiTranslations.getString("visits_date", language), formatDate(v.registrationDate))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                
                Spacer(modifier = Modifier.height(16.dp))

                // Status section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (v.isSent) Icons.Default.CheckCircle else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (v.isSent) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (v.isSent) UiTranslations.getString("sent_record", language) else UiTranslations.getString("pending_record", language),
                        fontWeight = FontWeight.Medium,
                        color = if (v.isSent) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
                
                if (v.isSent && v.sentDate != null) {
                    Text(
                        text = "${UiTranslations.getString("sent_on", language)}: ${formatDate(v.sentDate)}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 28.dp, top = 4.dp)
                    )
                }
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

