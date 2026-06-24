package yupay.turismo.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.deviceDataStore by preferencesDataStore(name = "device_prefs")

/**
 * Preferencias **por-dispositivo** (no por-cuenta), guardadas en su propio DataStore.
 *
 * Deliberadamente fuera de [yupay.turismo.data.local.AppSettings]/Room porque:
 *  - `AppSettings` se sincroniza a la nube y por P2P; activar notificaciones en un teléfono no debe
 *    activarlas en el otro dispositivo emparejado.
 *  - La base Room usa `fallbackToDestructiveMigration`: añadir una columna borraría los datos locales.
 *
 * Sigue el mismo patrón que [yupay.turismo.data.session.SessionManager].
 */
class DevicePreferences(appContext: Context) {

    private val ds = appContext.applicationContext.deviceDataStore

    private object Keys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val LAST_SEEN_WATERMARK = longPreferencesKey("last_seen_watermark")
    }

    /** ¿El usuario activó las notificaciones? Por defecto false (opt-in explícito). */
    val notificationsEnabledFlow: Flow<Boolean> =
        ds.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: false }

    suspend fun isNotificationsEnabled(): Boolean =
        ds.data.first()[Keys.NOTIFICATIONS_ENABLED] ?: false

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        ds.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    /** Marca de agua (epoch ms) de lo último que el usuario ya vio al abrir la app. */
    suspend fun lastSeenWatermark(): Long =
        ds.data.first()[Keys.LAST_SEEN_WATERMARK] ?: 0L

    suspend fun setLastSeenWatermark(value: Long) {
        ds.edit { it[Keys.LAST_SEEN_WATERMARK] = value }
    }
}
