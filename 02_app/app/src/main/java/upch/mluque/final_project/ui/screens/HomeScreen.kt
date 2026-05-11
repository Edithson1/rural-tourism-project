package upch.mluque.final_project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import upch.mluque.final_project.data.local.Visit
import upch.mluque.final_project.ui.components.BottomNavigationBar
import upch.mluque.final_project.ui.theme.Final_projectTheme
import java.util.Calendar

enum class TimeView { DAY, WEEK, MONTH, YEAR }

@Composable
fun HomeScreen(
    businessName: String,
    selectedService: String,
    visits: List<Visit>,
    onNavigateToTip: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigate: (String) -> Unit
) {
    var selectedView by remember { mutableStateOf(TimeView.MONTH) }

    val chartData = remember(visits, selectedView) {
        processChartData(visits, selectedView)
    }

    val maxCount = chartData.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "home",
                onNavigate = onNavigate
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "¡Hola, $businessName!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Rubro: $selectedService",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // View Selector
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                TimeView.entries.forEachIndexed { index, view ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = TimeView.entries.size),
                        onClick = { selectedView = view },
                        selected = selectedView == view,
                        label = { 
                            Text(
                                when(view) {
                                    TimeView.DAY -> "Día"
                                    TimeView.WEEK -> "Sem"
                                    TimeView.MONTH -> "Mes"
                                    TimeView.YEAR -> "Año"
                                },
                                fontSize = 12.sp
                            ) 
                        }
                    )
                }
            }

            // Chart Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = when(selectedView) {
                            TimeView.DAY -> "Visitas de hoy"
                            TimeView.WEEK -> "Visitas de la semana"
                            TimeView.MONTH -> "Visitas por semana (Mes)"
                            TimeView.YEAR -> "Visitas por mes (Año)"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        chartData.forEach { (label, count) ->
                            val heightFactor = (count.toFloat() / maxCount).coerceIn(0.05f, 1f)
                            BarItem(
                                label = label,
                                value = count.toString(),
                                heightFactor = heightFactor,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tip Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Tip de Emprendedor",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Optimiza tus perfiles sociales para atraer más...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onNavigateToTip,
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Visits
            Text(
                text = "Registros Recientes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Turista / País", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Gasto Est.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    
                    if (visits.isEmpty()) {
                        Text(
                            text = "No hay registros disponibles",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        visits.take(3).forEach { visit ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Text(text = visit.nationalityFlag, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = visit.nationality,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = visit.services.split(", ").firstOrNull() ?: "",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = visit.priceApprox,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End
                                )
                            }
                            if (visit != visits.take(3).last()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            }
                        }
                    }
                }
            }

            // Espacio final del tamaño del FAB para permitir scroll cómodo
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun BarItem(label: String, value: String, heightFactor: Float, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (value != "0") {
            Text(
                text = value,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        Box(
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight(heightFactor * 0.7f) // El máximo ocupa el 70% del alto total para dejar espacio a los textos
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(color)
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

private fun processChartData(visits: List<Visit>, view: TimeView): List<Pair<String, Int>> {
    val calendar = Calendar.getInstance()
    val now = calendar.timeInMillis
    
    return when (view) {
        TimeView.DAY -> {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val isDay = hour in 8..19
            val startHour = if (isDay) 8 else 20
            (0..11).map { i ->
                val h = (startHour + i) % 24
                val label = "${h}h"
                val count = visits.count { v ->
                    calendar.timeInMillis = v.registrationDate
                    calendar.get(Calendar.HOUR_OF_DAY) == h && 
                    isSameDay(v.registrationDate, now)
                }
                label to count
            }
        }
        TimeView.WEEK -> {
            val days = listOf("Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom")
            days.mapIndexed { index, name ->
                val count = visits.count { v ->
                    calendar.timeInMillis = v.registrationDate
                    val dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 // L=0, D=6
                    dayOfWeek == index && isSameWeek(v.registrationDate, now)
                }
                name to count
            }
        }
        TimeView.MONTH -> {
            (1..4).map { week ->
                val label = "Sem $week"
                val count = visits.count { v ->
                    calendar.timeInMillis = v.registrationDate
                    val weekOfMonth = ((calendar.get(Calendar.DAY_OF_MONTH) - 1) / 7) + 1
                    weekOfMonth == week && isSameMonth(v.registrationDate, now)
                }
                label to count
            }
        }
        TimeView.YEAR -> {
            val months = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
            months.mapIndexed { index, name ->
                val count = visits.count { v ->
                    calendar.timeInMillis = v.registrationDate
                    calendar.get(Calendar.MONTH) == index && isSameYear(v.registrationDate, now)
                }
                name to count
            }
        }
    }
}

private fun isSameDay(t1: Long, t2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

private fun isSameWeek(t1: Long, t2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.WEEK_OF_YEAR) == c2.get(Calendar.WEEK_OF_YEAR)
}

private fun isSameMonth(t1: Long, t2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
}

private fun isSameYear(t1: Long, t2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    Final_projectTheme {
        HomeScreen(
            businessName = "Tienda",
            selectedService = "Varios",
            visits = emptyList(),
            onNavigateToTip = {},
            onNavigateToAdd = {},
            onNavigate = {}
        )
    }
}
