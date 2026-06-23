package yupay.turismo.sync

import kotlinx.serialization.Serializable
import yupay.turismo.data.local.AppSettings
import yupay.turismo.data.local.Product
import yupay.turismo.data.local.Visit

@Serializable
sealed class SyncMessage {
    @Serializable
    data class Handshake(val deviceName: String, val sessionId: String) : SyncMessage()

    @Serializable
    data class SyncData(
        val settings: AppSettings,
        val visits: List<Visit>,
        val products: List<Product> = emptyList()
    ) : SyncMessage()

    @Serializable
    data class NewVisit(val visit: Visit) : SyncMessage()

    /**
     * Lista de visitas para reconciliar con el peer: además de insertar las nuevas, ACTUALIZA
     * las existentes (por `registrationDate`) — así el estado `isSent`/`remoteId` que el CLIENTE
     * fija al subir a la nube llega al SERVIDOR, y las visitas que bajan de la API se propagan.
     */
    @Serializable
    data class UpdateVisits(val visits: List<Visit>) : SyncMessage()

    @Serializable
    data class UpdateSettings(val settings: AppSettings) : SyncMessage()

    @Serializable
    data class UpdateProducts(val products: List<Product>) : SyncMessage()

    @Serializable
    data class Ping(val timestamp: Long) : SyncMessage()

    @Serializable
    data class Pong(val timestamp: Long) : SyncMessage()
    
    @Serializable
    data class Error(val message: String) : SyncMessage()

    @Serializable
    object RemoteLogout : SyncMessage()
}
