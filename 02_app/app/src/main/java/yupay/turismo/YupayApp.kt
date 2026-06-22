package yupay.turismo

import android.app.Application
import yupay.turismo.di.ServiceLocator

/**
 * Application: inicializa el localizador de servicios de la nube y arranca la
 * sincronización automática (reconexión + outbox). La sincronización P2P por LAN
 * sigue gestionándose aparte y no se ve afectada.
 */
class YupayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        ServiceLocator.startBackgroundSync()
    }
}
