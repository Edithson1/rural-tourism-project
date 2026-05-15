package upch.mluque.final_project.data.sync

enum class SyncStep {
    IDLE,
    CONNECTING,
    FETCHING_PROFILE,
    RECEIVING_VISITS,
    COMPLETED,
    ERROR
}

data class SyncProgress(
    val step: SyncStep = SyncStep.IDLE,
    val role: String = "NONE", // EMITTER, RECEIVER
    val percentage: Float = 0f,
    val totalItems: Int = 0,
    val processedItems: Int = 0,
    val errorMessage: String? = null,
    // Métricas de diagnóstico
    val connectionTimeMs: Long = 0,
    val transferTimeMs: Long = 0,
    val bytesPerSecond: Long = 0,
    val currentAttempt: Int = 0
)
