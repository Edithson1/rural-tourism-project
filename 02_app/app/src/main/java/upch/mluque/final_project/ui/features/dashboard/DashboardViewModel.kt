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
            visit.services.split(", ").filter { it.isNotBlank() }.forEach { service ->
                counts[service] = counts.getOrDefault(service, 0) + 1
            }
        }
        
        counts.toList()
            .map { it.first to (it.second.toFloat() / totalVisits) }
            .sortedByDescending { it.second }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val revenueEstimates = filteredVisits.map { visits ->
        visits.groupBy { it.priceCurrency }
            .mapValues { entry ->
                entry.value.sumOf { visit ->
                    parsePriceValue(visit.priceValue)
                }
            }
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

    private fun parsePriceValue(value: String): Double {
        return try {
            val cleanValue = value.replace(",", ".").trim()
            if (cleanValue.endsWith("+")) {
                cleanValue.removeSuffix("+").trim().toDoubleOrNull() ?: 0.0
            } else if (cleanValue.contains("-")) {
                val parts = cleanValue.split("-").map { it.trim().toDoubleOrNull() ?: 0.0 }
                if (parts.size == 2) (parts[0] + parts[1]) / 2.0 else parts.getOrNull(0) ?: 0.0
            } else {
                cleanValue.toDoubleOrNull() ?: 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }

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
            "Quechua" -> "Kaymi rurukuna: Llapanpiqa $total watukuqkunan hamurqan. Ñawpaq suyuqa $leader kachkan. Aswan munasqa yanapakuyqa $star."
            "Inglés" -> "Here are the insights: A total of $total tourists visited. The leading country is $leader. The most requested service is $star."
            "Portugués" -> "Aqui estão os resultados: Um total de $total turistas visitaram. O país líder é $leader. O serviço mais solicitado é $star."
            else -> "Aquí tienes los resultados: Un total de $total turistas visitaron tu negocio. El país líder es $leader y tu servicio estrella es $star."
        }
    }
}

enum class DashboardFilter {
    ALL, LAST_7_DAYS, THIS_MONTH, THIS_YEAR
}
