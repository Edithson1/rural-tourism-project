package yupay.turismo.ui.features.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import yupay.turismo.data.local.Visit
import yupay.turismo.data.local.AppSettings
import yupay.turismo.ui.features.dashboard.components.FilterSelector
import yupay.turismo.ui.features.dashboard.tabs.SalesTab
import yupay.turismo.ui.features.dashboard.tabs.SummaryTab
import yupay.turismo.ui.features.dashboard.tabs.TimesTab
import yupay.turismo.ui.features.dashboard.tabs.VisitorsTab
import yupay.turismo.utils.UiTranslations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    visits: List<Visit>,
    settings: AppSettings?,
    language: String,
    onBack: () -> Unit
) {
    val dashboardViewModel: DashboardViewModel = viewModel()
    val context = LocalContext.current

    val totalVisitors by dashboardViewModel.totalVisitors.collectAsState()
    val totalItemsSold by dashboardViewModel.totalItemsSold.collectAsState()
    val totalRevenue by dashboardViewModel.totalRevenue.collectAsState()
    val topNationalities by dashboardViewModel.topNationalities.collectAsState()
    val serviceDistribution by dashboardViewModel.serviceDistribution.collectAsState()
    val revenueByService by dashboardViewModel.revenueByService.collectAsState()
    val leaderCountry by dashboardViewModel.leaderCountry.collectAsState()
    val starService by dashboardViewModel.starService.collectAsState()
    val currentFilter by dashboardViewModel.filter.collectAsState()
    val revenueEstimates by dashboardViewModel.revenueEstimates.collectAsState()
    val peakHours by dashboardViewModel.peakHours.collectAsState()
    val averageTicket by dashboardViewModel.averageTicket.collectAsState()
    val visitsByWeekday by dashboardViewModel.visitsByWeekday.collectAsState()
    val partOfDay by dashboardViewModel.partOfDayDistribution.collectAsState()
    val revenueSeries by dashboardViewModel.revenueSeries.collectAsState()
    val visitorsSeries by dashboardViewModel.visitorsSeries.collectAsState()

    var selectedTab by remember { mutableStateOf(DashboardTab.SUMMARY) }

    LaunchedEffect(visits, settings) {
        dashboardViewModel.updateVisits(visits)
        dashboardViewModel.updateSettings(settings)
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Filtro de periodo compartido por todas las pestañas
            FilterSelector(currentFilter, language) { dashboardViewModel.setFilter(it) }

            // Pestañas temáticas
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                DashboardTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                UiTranslations.getString(context, tab.labelKey, language),
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                DashboardTab.SUMMARY -> SummaryTab(
                    language = language,
                    total = totalVisitors,
                    leader = leaderCountry,
                    star = starService,
                    averageTicket = averageTicket,
                    totalItems = totalItemsSold,
                    totalRevenue = totalRevenue,
                    revenue = revenueEstimates,
                    revenueSeries = revenueSeries
                )
                DashboardTab.VISITORS -> VisitorsTab(
                    language = language,
                    topNationalities = topNationalities,
                    visitsByWeekday = visitsByWeekday,
                    visitorsSeries = visitorsSeries
                )
                DashboardTab.SALES -> SalesTab(
                    language = language,
                    revenueSeries = revenueSeries,
                    averageTicket = averageTicket,
                    serviceDistribution = serviceDistribution,
                    revenueByService = revenueByService
                )
                DashboardTab.TIMES -> TimesTab(
                    language = language,
                    peakHours = peakHours,
                    visitsByWeekday = visitsByWeekday,
                    partOfDay = partOfDay
                )
            }
        }
    }
}
