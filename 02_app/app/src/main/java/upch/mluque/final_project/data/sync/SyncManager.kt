package upch.mluque.final_project.data.sync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import upch.mluque.final_project.data.local.Visit

/**
 * Singleton to share sync state between Service and UI
 */
object SyncManager {
    private val _progress = MutableStateFlow(SyncProgress())
    val progress = _progress.asStateFlow()

    private val _visitReceived = MutableSharedFlow<Visit>()
    val visitReceived = _visitReceived.asSharedFlow()

    fun updateProgress(update: (SyncProgress) -> SyncProgress) {
        _progress.value = update(_progress.value)
    }

    suspend fun emitVisit(visit: Visit) {
        _visitReceived.emit(visit)
    }

    fun reset(role: String = "NONE") {
        _progress.value = SyncProgress(role = role, step = if (role != "NONE") SyncStep.CONNECTING else SyncStep.IDLE)
    }
}
