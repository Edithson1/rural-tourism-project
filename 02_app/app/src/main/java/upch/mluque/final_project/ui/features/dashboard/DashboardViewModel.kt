package upch.mluque.final_project.ui.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import upch.mluque.final_project.data.local.Visit
import java.util.*

class DashboardViewModel : ViewModel() {

    private val _visits = MutableStateFlow<List<Visit>>(emptyList())
    
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

    val topNationalities = filteredVisits.map { visits ->
        visits.groupBy { it.nationality to it.nationalityFlag }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val serviceDistribution = filteredVisits.map { visits ->
        val totalVisits = visits.size
        if (totalVisits == 0) return@map emptyList<Pair<String, Float>>()

        val counts = mutableMapOf<String, Int>()
        visits.forEach { visit ->
            visit.selectedProducts.forEach { item ->
                counts[item.name] = counts.getOrDefault(item.name, 0) + item.quantity
            }
        }
        
        val totalItems = counts.values.sum().toFloat()
        if (totalItems == 0f) return@map emptyList<Pair<String, Float>>()

        counts.toList()
            .map { it.first to (it.second.toFloat() / totalItems) }
            .sortedByDescending { it.second }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val revenueEstimates = filteredVisits.map { visits ->
        mapOf("S/" to visits.sumOf { it.totalAmount })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

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

enum class DashboardFilter {
    ALL, LAST_7_DAYS, THIS_MONTH, THIS_YEAR
}
