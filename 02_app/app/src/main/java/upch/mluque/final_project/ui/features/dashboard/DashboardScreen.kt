package upch.mluque.final_project.ui.features.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import upch.mluque.final_project.data.local.Visit
import upch.mluque.final_project.utils.UiTranslations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    visits: List<Visit>,
    language: String,
    onBack: () -> Unit
) {
    val dashboardViewModel: DashboardViewModel = viewModel()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp && configuration.screenWidthDp > 600
    
    val totalVisitors by dashboardViewModel.totalVisitors.collectAsState()
    val topNationalities by dashboardViewModel.topNationalities.collectAsState()
    val serviceDistribution by dashboardViewModel.serviceDistribution.collectAsState()
    val leaderCountry by dashboardViewModel.leaderCountry.collectAsState()
    val starService by dashboardViewModel.starService.collectAsState()
    val currentFilter by dashboardViewModel.filter.collectAsState()
    val revenueEstimates by dashboardViewModel.revenueEstimates.collectAsState()
    val peakHours by dashboardViewModel.peakHours.collectAsState()

    LaunchedEffect(visits) {
        dashboardViewModel.updateVisits(visits)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(UiTranslations.getString(context, "dashboard_title", language), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val summary = dashboardViewModel.getInsightsSummary(language)
                    android.widget.Toast.makeText(context, summary, android.widget.Toast.LENGTH_LONG).show()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = UiTranslations.getString(context, "insights_tts_description", language))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLandscape) {
            LandscapeDashboardContent(
                padding, language, totalVisitors, leaderCountry, starService, 
                currentFilter, topNationalities, serviceDistribution, 
                revenueEstimates, peakHours, onFilterSelected = { dashboardViewModel.setFilter(it) }
            )
        } else {
            PortraitDashboardContent(
                padding, language, totalVisitors, leaderCountry, starService, 
                currentFilter, topNationalities, serviceDistribution, 
                revenueEstimates, peakHours, onFilterSelected = { dashboardViewModel.setFilter(it) }
            )
        }
    }
}

@Composable
fun PortraitDashboardContent(
    padding: PaddingValues,
    language: String,
    totalVisitors: Int,
    leaderCountry: String,
    starService: String,
    currentFilter: DashboardFilter,
    topNationalities: List<Pair<Pair<String, String>, Int>>,
    serviceDistribution: List<Pair<String, Float>>,
    revenueEstimates: Map<String, Double>,
    peakHours: List<Pair<Int, Int>>,
    onFilterSelected: (DashboardFilter) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            FilterSelector(currentFilter, language, onFilterSelected)
        }

        item {
            KpiSection(totalVisitors, leaderCountry, starService, revenueEstimates, language)
        }

        item {
            ChartSection(
                title = UiTranslations.getString(context, "insights_top_countries", language),
                content = { NationalityBarChart(topNationalities) }
            )
        }

        item {
            ChartSection(
                title = UiTranslations.getString(context, "insights_peak_hours", language),
                content = { PeakHoursChart(peakHours) }
            )
        }

        item {
            ChartSection(
                title = UiTranslations.getString(context, "insights_services", language),
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        serviceDistribution.forEach { (service, percentage) ->
                            ServiceProgressRow(service, percentage, language)
                        }
                    }
                }
            )
        }
        
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun LandscapeDashboardContent(
    padding: PaddingValues,
    language: String,
    totalVisitors: Int,
    leaderCountry: String,
    starService: String,
    currentFilter: DashboardFilter,
    topNationalities: List<Pair<Pair<String, String>, Int>>,
    serviceDistribution: List<Pair<String, Float>>,
    revenueEstimates: Map<String, Double>,
    peakHours: List<Pair<Int, Int>>,
    onFilterSelected: (DashboardFilter) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp)
    ) {
        // Left Column: Filter and Charts
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(end = 10.dp)
        ) {
            FilterSelector(currentFilter, language, onFilterSelected)
            Spacer(modifier = Modifier.height(16.dp))
            ChartSection(
                title = UiTranslations.getString(context, "insights_top_countries", language),
                content = { NationalityBarChart(topNationalities) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            ChartSection(
                title = UiTranslations.getString(context, "insights_peak_hours", language),
                content = { PeakHoursChart(peakHours) }
            )
            Spacer(modifier = Modifier.height(100.dp))
        }

        // Right Column: KPIs and Services
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 10.dp)
        ) {
            KpiSection(totalVisitors, leaderCountry, starService, revenueEstimates, language)
            Spacer(modifier = Modifier.height(16.dp))
            ChartSection(
                title = UiTranslations.getString(context, "insights_services", language),
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        serviceDistribution.forEach { (service, percentage) ->
                            ServiceProgressRow(service, percentage, language)
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun FilterSelector(
    currentFilter: DashboardFilter,
    language: String,
    onFilterSelected: (DashboardFilter) -> Unit
) {
    val context = LocalContext.current
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(DashboardFilter.entries.toTypedArray()) { filter ->
            val labelKey = when(filter) {
                DashboardFilter.ALL -> "insights_filter_all"
                DashboardFilter.LAST_7_DAYS -> "insights_filter_week"
                DashboardFilter.THIS_MONTH -> "insights_filter_month"
                DashboardFilter.THIS_YEAR -> "insights_filter_year"
            }
            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(UiTranslations.getString(context, labelKey, language), fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun KpiSection(
    total: Int,
    leader: String,
    star: String,
    revenue: Map<String, Double>,
    language: String
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiCard(
                modifier = Modifier.weight(1f),
                title = UiTranslations.getString(context, "insights_total_visitors", language),
                value = total.toString(),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = UiTranslations.getString(context, "insights_leader_country", language),
                value = leader,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        
        KpiCard(
            modifier = Modifier.fillMaxWidth(),
            title = UiTranslations.getString(context, "insights_star_service", language),
            value = star,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )

        if (revenue.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = UiTranslations.getString(context, "insights_revenue_est", language),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    revenue.forEach { (currency, amount) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = currency, fontWeight = FontWeight.Medium)
                            Text(text = String.format("%.2f", amount), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChartSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun KpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = contentColor.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}

@Composable
fun NationalityBarChart(data: List<Pair<Pair<String, String>, Int>>) {
    val maxCount = if (data.isNotEmpty()) data.maxOf { it.second } else 1
    
    Column {
        data.forEach { (nationality, count) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = nationality.second, modifier = Modifier.width(30.dp), fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val widthProgress by animateFloatAsState(
                        targetValue = count.toFloat() / maxCount,
                        animationSpec = tween(1200)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(widthProgress)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = count.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        if (data.isEmpty()) Text("No data", color = Color.Gray)
    }
}

@Composable
fun PeakHoursChart(hours: List<Pair<Int, Int>>) {
    val maxVisits = if (hours.isNotEmpty()) hours.maxOf { it.second } else 1
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        hours.filterIndexed { index, _ -> index in 7..21 }.forEach { (hour, count) ->
            val heightProgress by animateFloatAsState(
                targetValue = if (maxVisits > 0) count.toFloat() / maxVisits else 0.1f,
                animationSpec = tween(1200)
            )
            
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(heightProgress.coerceAtLeast(0.05f))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (count == maxVisits && count > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary)
                )
                Text(text = "${hour}h", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ServiceProgressRow(service: String, percentage: Float, language: String) {
    val context = LocalContext.current
    val translatedService = UiTranslations.translateService(service, language, context)
    val progress by animateFloatAsState(targetValue = percentage, animationSpec = tween(1200))

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = translatedService, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = "${(percentage * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.secondaryContainer
        )
    }
}
