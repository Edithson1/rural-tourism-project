package upch.mluque.final_project.ui.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import upch.mluque.final_project.data.local.AppSettings
import upch.mluque.final_project.data.local.Visit
import upch.mluque.final_project.utils.CurrencyUtils
import java.util.*

class DashboardViewModel : ViewModel() {

    private val _visits = MutableStateFlow<List<Visit>>(emptyList())
    private val _settings = MutableStateFlow<AppSettings?>(null)

    private val _filter = MutableStateFlow(DashboardFilter.ALL)
    val filter: StateFlow<DashboardFilter> = _filter.asStateFlow()

    val filteredVisits = combine(_visits, _filter) { visits, filter ->
        val now = Calendar.getInstance()
        when (filter) {
            DashboardFilter.ALL -> visits
            DashboardFilter.THIS_MONTH -> visits.filter {
                val cal = Calendar.getInstance().apply { timeInMillis = it.registrationDate }
                cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
            }
            DashboardFilter.THIS_YEAR -> visits.filter {
                val cal = Calendar.getInstance().apply { timeInMillis = it.registrationDate }
                cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
            }
            DashboardFilter.LAST_7_DAYS -> {
                val sevenDaysAgo = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                }.timeInMillis
                visits.filter { it.registrationDate >= sevenDaysAgo }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalVisitors = filteredVisits.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalItemsSold = filteredVisits.map { visits ->
        visits.sumOf { v -> v.selectedProducts.sumOf { it.quantity } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val topNationalities = filteredVisits.map { visits ->
        visits.groupBy { it.nationality to it.nationalityFlag }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val serviceDistribution = combine(filteredVisits, _settings) { visits, settings ->
        val totalVisits = visits.size
        if (totalVisits == 0) return@combine emptyList<Pair<String, Float>>()

        val counts = mutableMapOf<String, Int>()
        visits.forEach { visit ->
            visit.selectedProducts.forEach { item ->
                counts[item.name] = counts.getOrDefault(item.name, 0) + item.quantity
            }
        }

        val totalItems = counts.values.sum().toFloat()
        if (totalItems == 0f) return@combine emptyList<Pair<String, Float>>()

        counts.toList()
            .map { it.first to (it.second.toFloat() / totalItems) }
            .sortedByDescending { it.second }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val revenueByService = combine(filteredVisits, _settings) { visits, settings ->
        val prefCurrency = settings?.preferredCurrency ?: "S/"
        val usdRate = settings?.usdExchangeRate ?: 3.8
        val eurRate = settings?.eurExchangeRate ?: 4.1
        
        val totals = mutableMapOf<String, Double>()
        visits.forEach { visit ->
            visit.selectedProducts.forEach { item ->
                val priceConverted = CurrencyUtils.convert(item.priceAtSale, item.currency, prefCurrency, usdRate, eurRate)
                totals[item.name] = totals.getOrDefault(item.name, 0.0) + priceConverted * item.quantity
            }
        }
        totals.toList().sortedByDescending { it.second }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val revenueEstimates = combine(filteredVisits, _settings) { visits, settings ->
        val prefCurrency = settings?.preferredCurrency ?: "S/"
        val usdRate = settings?.usdExchangeRate ?: 3.8
        val eurRate = settings?.eurExchangeRate ?: 4.1
        
        val totalConverted = visits.sumOf { visit ->
            CurrencyUtils.convert(visit.totalAmount, visit.currency, prefCurrency, usdRate, eurRate)
        }
        mapOf(prefCurrency to totalConverted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val totalRevenue = combine(filteredVisits, _settings) { visits, settings ->
        val prefCurrency = settings?.preferredCurrency ?: "S/"
        val usdRate = settings?.usdExchangeRate ?: 3.8
        val eurRate = settings?.eurExchangeRate ?: 4.1
        
        visits.sumOf { visit ->
            CurrencyUtils.convert(visit.totalAmount, visit.currency, prefCurrency, usdRate, eurRate)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val averageTicket = combine(filteredVisits, _settings) { visits, settings ->
        if (visits.isEmpty()) return@combine 0.0
        
        val prefCurrency = settings?.preferredCurrency ?: "S/"
        val usdRate = settings?.usdExchangeRate ?: 3.8
        val eurRate = settings?.eurExchangeRate ?: 4.1
        
        val totalRevenueConverted = visits.sumOf { visit ->
            CurrencyUtils.convert(visit.totalAmount, visit.currency, prefCurrency, usdRate, eurRate)
        }
        totalRevenueConverted / visits.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val visitsByWeekday = filteredVisits.map { visits ->
        val counts = IntArray(7)
        val cal = Calendar.getInstance()
        visits.forEach {
            cal.timeInMillis = it.registrationDate
            val idx = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
            counts[idx]++
        }
        counts.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(7) { 0 })

    val partOfDayDistribution = filteredVisits.map { visits ->
        val buckets = IntArray(3)
        val cal = Calendar.getInstance()
        visits.forEach {
            cal.timeInMillis = it.registrationDate
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val idx = when (hour) {
                in 5..11 -> 0
                in 12..18 -> 1
                else -> 2
            }
            buckets[idx]++
        }
        buckets.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(3) { 0 })

    val revenueSeries = combine(filteredVisits, _filter, _settings) { visits, filter, settings ->
        val prefCurrency = settings?.preferredCurrency ?: "S/"
        val usdRate = settings?.usdExchangeRate ?: 3.8
        val eurRate = settings?.eurExchangeRate ?: 4.1
        
        buildSeries(visits, filter) { visit ->
            CurrencyUtils.convert(visit.totalAmount, visit.currency, prefCurrency, usdRate, eurRate)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        RevenueSeries(RevenueGranularity.MONTH, emptyList())
    )

    val visitorsSeries = combine(filteredVisits, _filter) { visits, filter ->
        buildSeries(visits, filter) { 1.0 }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        RevenueSeries(RevenueGranularity.MONTH, emptyList())
    )

    private fun buildSeries(
        visits: List<Visit>,
        filter: DashboardFilter,
        value: (Visit) -> Double
    ): RevenueSeries {
        val cal = Calendar.getInstance()
        return when (filter) {
            DashboardFilter.LAST_7_DAYS -> {
                val points = (6 downTo 0).map { offset ->
                    val day = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
                    val dow = (day.get(Calendar.DAY_OF_WEEK) + 5) % 7
                    val sum = visits.filter {
                        cal.timeInMillis = it.registrationDate
                        cal.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
                            cal.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
                    }.sumOf(value)
                    dow to sum
                }
                RevenueSeries(RevenueGranularity.DAY, points)
            }
            DashboardFilter.THIS_MONTH -> {
                val weeksInMonth = Calendar.getInstance().getActualMaximum(Calendar.WEEK_OF_MONTH).coerceIn(4, 6)
                val points = (1..weeksInMonth).map { week ->
                    val sum = visits.filter {
                        cal.timeInMillis = it.registrationDate
                        ((cal.get(Calendar.DAY_OF_MONTH) - 1) / 7) + 1 == week
                    }.sumOf(value)
                    week to sum
                }
                RevenueSeries(RevenueGranularity.WEEK_OF_MONTH, points)
            }
            else -> {
                val points = (0..11).map { month ->
                    val sum = visits.filter {
                        cal.timeInMillis = it.registrationDate
                        cal.get(Calendar.MONTH) == month
                    }.sumOf(value)
                    month to sum
                }
                RevenueSeries(RevenueGranularity.MONTH, points)
            }
        }
    }

    val peakHours = filteredVisits.map { visits ->
        val hours = IntArray(24) { 0 }
        val cal = Calendar.getInstance()
        visits.forEach {
            cal.timeInMillis = it.registrationDate
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hours[hour]++
        }
        hours.toList().mapIndexed { index, count -> index to count }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(24) { it to 0 })

    val leaderCountry = topNationalities.map { data ->
        data.firstOrNull()?.let { "${it.first.first} ${it.first.second}" } ?: "-"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "-")

    val starService = serviceDistribution.map {
        it.firstOrNull()?.first ?: "-"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "-")

    fun updateVisits(visits: List<Visit>) {
        _visits.value = visits
    }

    fun updateSettings(settings: AppSettings?) {
        _settings.value = settings
    }

    fun setFilter(filter: DashboardFilter) {
        _filter.value = filter
    }

    fun getInsightsSummary(language: String): String {
        val total = totalVisitors.value
        val leader = leaderCountry.value
        val star = starService.value

        return when(language) {
            "Quechua" -> "Kaymi rurukuna: Llapanpiqa $total watukuqkunan hamurqan. Ñawpaq suyuqa $leader kachkan. Aswan munasqa ruruqa $star."
            "Inglés" -> "Here are the insights: A total of $total tourists visited. The leading country is $leader. The top product is $star."
            "Portugués" -> "Aqui estão os resultados: Um total de $total turistas visitaram. O país líder é $leader. O produto principal é $star."
            else -> "Aquí tienes los resultados: Un total de $total turistas visitaron tu negocio. El país líder es $leader y tu producto más vendido es $star."
        }
    }
}
