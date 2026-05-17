package upch.mluque.final_project.sync

import kotlinx.serialization.Serializable
import upch.mluque.final_project.data.local.AppSettings
import upch.mluque.final_project.data.local.Visit

@Serializable
sealed class SyncMessage {
    @Serializable
    data class Handshake(val deviceName: String, val sessionId: String) : SyncMessage()

    @Serializable
    data class SyncData(val settings: AppSettings, val visits: List<Visit>) : SyncMessage()

    @Serializable
    data class NewVisit(val visit: Visit) : SyncMessage()

    @Serializable
    data class UpdateSettings(val settings: AppSettings) : SyncMessage()

    @Serializable
    data class Ping(val timestamp: Long) : SyncMessage()

    @Serializable
    data class Pong(val timestamp: Long) : SyncMessage()
    
    @Serializable
    data class Error(val message: String) : SyncMessage()
}
