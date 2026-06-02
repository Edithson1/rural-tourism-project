 package upch.mluque.final_project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import upch.mluque.final_project.data.local.Visit
import upch.mluque.final_project.ui.MainViewModel
import upch.mluque.final_project.ui.components.BottomNavigationBar
import upch.mluque.final_project.ui.theme.Final_projectTheme
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Preview(showBackground = true)
@Composable
fun VisitsPreview() {
    Final_projectTheme {
        // Mock UI for preview since we can't easily mock MainViewModel here
        // In a real app we might use a different composable for the list content
        Scaffold(
            bottomBar = {
                BottomNavigationBar(currentRoute = "visits", onNavigate = {})
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {},
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tus Registros",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                VisitItem(
                    visit = Visit(1, "Francia", "🇫🇷", "S/ 51 - 100", "Hospedaje", System.currentTimeMillis() - 86400000, true, System.currentTimeMillis()),
                    onClick = {}
                )
                Spacer(modifier = Modifier.height(12.dp))
                VisitItem(
                    visit = Visit(2, "Estados Unidos", "🇺🇸", "S/ 201 - 500", "Alimentación", System.currentTimeMillis() - 172800000, false, null),
                    onClick = {}
                )
            }
        }
    }
}

@Composable
fun VisitsScreen(
    viewModel: MainViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onNavigate: (String) -> Unit
) {
    val visits by viewModel.allVisits.collectAsState()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp && configuration.screenWidthDp > 600

    val groupedVisits = remember(visits) {
        groupVisits(visits)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = if (isLandscape) 48.dp else 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tus Registros",
                    fontSize = if (isLandscape) 32.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "${visits.size} total",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (visits.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No hay registros aún",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxWidth(if (isLandscape) 0.8f else 1f).align(Alignment.CenterHorizontally)
                ) {
                    groupedVisits.forEach { entry ->
                        val category = entry.key
                        val visitsInCategory = entry.value
                        
                        item {
                            Text(
                                text = category,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }
                        items(visitsInCategory) { visit ->
                            VisitItem(
                                visit = visit,
                                onClick = { onNavigateToDetail(visit.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun groupVisits(visits: List<Visit>): Map<String, List<Visit>> {
    val grouped = mutableMapOf<String, MutableList<Visit>>()
    val now = Calendar.getInstance()
    
    val sortedVisits = visits.sortedByDescending { it.registrationDate }
    
    sortedVisits.forEach { visit ->
        val visitDate = Calendar.getInstance().apply { timeInMillis = visit.registrationDate }
        
        val category = when {
            isSameDay(now, visitDate) -> "Hoy"
            isYesterday(now, visitDate) -> "Ayer"
            isSameWeek(now, visitDate) -> "Esta semana"
            isSameMonth(now, visitDate) -> "Este mes"
            isSameYear(now, visitDate) -> "Este año"
            else -> "Más de un año"
        }
        
        grouped.getOrPut(category) { mutableListOf() }.add(visit)
    }
    
    val result = LinkedHashMap<String, List<Visit>>()
    listOf("Hoy", "Ayer", "Esta semana", "Este mes", "Este año", "Más de un año").forEach { cat ->
        grouped[cat]?.let { result[cat] = it }
    }
    return result
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(today: Calendar, date: Calendar): Boolean {
    val yesterday = Calendar.getInstance().apply {
        timeInMillis = today.timeInMillis
        add(Calendar.DAY_OF_YEAR, -1)
    }
    return isSameDay(yesterday, date)
}

private fun isSameWeek(today: Calendar, date: Calendar): Boolean {
    return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
           today.get(Calendar.WEEK_OF_YEAR) == date.get(Calendar.WEEK_OF_YEAR)
}

private fun isSameMonth(today: Calendar, date: Calendar): Boolean {
    return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
           today.get(Calendar.MONTH) == date.get(Calendar.MONTH)
}

private fun isSameYear(today: Calendar, date: Calendar): Boolean {
    return today.get(Calendar.YEAR) == date.get(Calendar.YEAR)
}

@Composable
fun VisitItem(visit: Visit, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Placeholder (Gray circle with Person icon)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Turista - ${visit.nationality}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = getTimeAgo(visit.registrationDate),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Status Icon
            if (visit.isSent) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Enviado",
                    tint = Color(0xFF4CAF50), // Green for success
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Schedule, // Clock icon
                    contentDescription = "Pendiente",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutes < 1 -> "Ahora mismo"
        minutes < 60 -> "Hace $minutes min"
        hours < 24 -> if (hours == 1L) "Hace 1 hora" else "Hace $hours horas"
        days == 1L -> "Ayer"
        days < 7 -> "Hace $days días"
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
