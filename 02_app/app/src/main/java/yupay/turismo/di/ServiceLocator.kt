package yupay.turismo.di

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import yupay.turismo.data.local.AppDatabase
import yupay.turismo.data.remote.HttpModule
import yupay.turismo.data.remote.YupayApiService
import yupay.turismo.data.repository.AuthRepository
import yupay.turismo.data.repository.CloudSyncRepository
import yupay.turismo.data.session.SessionManager
import yupay.turismo.data.sync.CloudSyncEngine
import yupay.turismo.utils.NetworkMonitor

/**
 * Localizador de servicios simple (sin framework de DI), acorde al estilo del proyecto
 * (`AppDatabase.getDatabase`). Construye y mantiene los singletons de la capa de nube.
 *
 * [init] es idempotente y barato; lo llaman tanto [yupay.turismo.YupayApp] como los
 * ViewModels de forma defensiva. [startBackgroundSync] arranca los observadores una sola vez.
 */
object ServiceLocator {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var initialized = false
    @Volatile private var started = false

    lateinit var sessionManager: SessionManager
        private set
    lateinit var apiService: YupayApiService
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var cloudSyncRepository: CloudSyncRepository
        private set
    lateinit var cloudSyncEngine: CloudSyncEngine
        private set
    lateinit var networkMonitor: NetworkMonitor
        private set

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val app = context.applicationContext
            val db = AppDatabase.getDatabase(app)

            sessionManager = SessionManager(app)
            apiService = HttpModule.create(sessionManager)
            authRepository = AuthRepository(apiService, sessionManager, db.appSettingsDao())
            cloudSyncRepository = CloudSyncRepository(
                api = apiService,
                session = sessionManager,
                appSettingsDao = db.appSettingsDao(),
                productDao = db.productDao(),
                visitDao = db.visitDao(),
                pendingOpDao = db.pendingOpDao()
            )
            networkMonitor = NetworkMonitor(app)
            cloudSyncEngine = CloudSyncEngine(cloudSyncRepository, sessionManager, networkMonitor)

            initialized = true
        }
    }

    /** Arranca el monitor de red y los disparadores automáticos de sync (una sola vez). */
    fun startBackgroundSync() {
        if (started) return
        synchronized(this) {
            if (started) return
            networkMonitor.start()
            cloudSyncEngine.start(appScope)
            started = true
        }
    }
}
