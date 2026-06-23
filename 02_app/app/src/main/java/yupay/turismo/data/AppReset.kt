package yupay.turismo.data

import android.content.Context
import android.provider.Settings
import yupay.turismo.data.local.AppDatabase
import yupay.turismo.data.local.AppSettings
import yupay.turismo.data.local.DefaultContent
import yupay.turismo.di.ServiceLocator

/**
 * Reset TOTAL de la app a "estado de fábrica" (como recién descargada). Es el comportamiento
 * único y bien definido que comparten:
 *  - el "Cerrar sesión (Borrar todo)" de la cuenta online ([yupay.turismo.ui.MainViewModel.clearAllAppData]), y
 *  - el reset del dispositivo SERVIDOR al cancelar la vinculación P2P ([yupay.turismo.sync.SyncViewModel.logout]).
 *
 * Limpia: sesión de nube (tokens) + outbox de cambios pendientes + todas las tablas de Room;
 * y vuelve a sembrar los ajustes por defecto (deviceId del equipo + tips/mapas iniciales).
 */
object AppReset {
    suspend fun factoryReset(context: Context) {
        val appContext = context.applicationContext

        // Sesión de nube + cola de cambios pendientes (idempotente).
        ServiceLocator.init(appContext)
        runCatching { ServiceLocator.sessionManager.clear() }
        runCatching { ServiceLocator.cloudSyncRepository.clearOutbox() }

        // Datos locales + re-siembra de ajustes por defecto.
        val db = AppDatabase.getDatabase(appContext)
        val repo = DataRepository(db.appSettingsDao(), db.visitDao(), db.productDao())
        repo.clearAllData()

        val androidId = Settings.Secure.getString(
            appContext.contentResolver, Settings.Secure.ANDROID_ID
        ) ?: "unknown"
        repo.saveSettings(
            AppSettings(
                deviceId = androidId,
                hardwareDeviceId = androidId,
                entrepreneurTips = DefaultContent.tips,
                mapSummary = DefaultContent.summaries
            )
        )
    }
}
