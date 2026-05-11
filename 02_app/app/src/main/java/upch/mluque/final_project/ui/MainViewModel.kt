package upch.mluque.final_project.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import upch.mluque.final_project.data.DataRepository
import upch.mluque.final_project.data.local.AppDatabase
import upch.mluque.final_project.data.local.AppSettings
import upch.mluque.final_project.data.local.Visit

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DataRepository
    val appSettings: StateFlow<AppSettings?>
    val allVisits: StateFlow<List<Visit>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DataRepository(database.appSettingsDao(), database.visitDao())
        
        appSettings = repository.appSettings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        allVisits = repository.allVisits.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addVisit(nationality: String, flag: String, price: String, services: String) {
        viewModelScope.launch {
            val visit = Visit(
                nationality = nationality,
                nationalityFlag = flag,
                priceApprox = price,
                services = services
            )
            repository.insertVisit(visit)
        }
    }

    suspend fun getVisitDetail(id: Int): Visit? {
        return repository.getVisitById(id)
    }

    fun saveLanguage(language: String) {
        viewModelScope.launch {
            val current = appSettings.value ?: AppSettings()
            repository.saveSettings(current.copy(language = language))
        }
    }

    fun saveProfile(name: String, category: String) {
        viewModelScope.launch {
            val current = appSettings.value ?: AppSettings()
            repository.saveSettings(current.copy(
                businessName = name,
                businessCategory = category,
                isOnboardingCompleted = true
            ))
        }
    }

    fun updateSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = appSettings.value ?: AppSettings()
            repository.saveSettings(current.copy(isSyncEnabled = enabled))
        }
    }

    fun updateVoiceSpeed(speed: Float) {
        viewModelScope.launch {
            val current = appSettings.value ?: AppSettings()
            repository.saveSettings(current.copy(voiceSpeed = speed))
        }
    }
}
