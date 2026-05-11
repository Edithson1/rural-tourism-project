package upch.mluque.final_project.data

import kotlinx.coroutines.flow.Flow
import upch.mluque.final_project.data.local.AppSettings
import upch.mluque.final_project.data.local.AppSettingsDao
import upch.mluque.final_project.data.local.Visit
import upch.mluque.final_project.data.local.VisitDao

class DataRepository(
    private val appSettingsDao: AppSettingsDao,
    private val visitDao: VisitDao
) {
    val appSettings: Flow<AppSettings?> = appSettingsDao.getSettings()
    val allVisits: Flow<List<Visit>> = visitDao.getAllVisits()

    suspend fun saveSettings(settings: AppSettings) {
        appSettingsDao.saveSettings(settings)
    }

    suspend fun insertVisit(visit: Visit) {
        visitDao.insertVisit(visit)
    }

    suspend fun getVisitById(id: Int): Visit? {
        return visitDao.getVisitById(id)
    }

    suspend fun updateLanguage(language: String) {
        val current = AppSettings(language = language) // Simple for now, ideally we merge with current
        // In a real app we would get the current first, but for id=1 it replaces
        saveSettings(current)
    }
}
