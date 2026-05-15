package upch.mluque.final_project.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import upch.mluque.final_project.data.DataRepository
import upch.mluque.final_project.data.local.AppDatabase
import upch.mluque.final_project.data.local.AppSettings
import upch.mluque.final_project.data.local.Visit
import kotlinx.coroutines.flow.*
import upch.mluque.final_project.data.sync.*
import kotlinx.coroutines.flow.first
import java.io.ByteArrayOutputStream
import java.io.InputStream
import android.util.Log
import java.net.Inet4Address
import java.net.NetworkInterface
import android.content.Intent
import android.os.Build

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DataRepository
    val appSettings: StateFlow<AppSettings?>
    val allVisits: StateFlow<List<Visit>>
    
    val syncManager = WifiDirectManager(application)
    val syncStatus = syncManager.syncStatus
    val isSyncConnected = syncManager.isConnected
    val syncProgress = SyncManager.progress

    private val _isInitialSyncing = MutableStateFlow(false)
    val isInitialSyncing = _isInitialSyncing.asStateFlow()

    private var lastSyncData: SyncData? = null

    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetworkStatus()
        }

        override fun onLost(network: Network) {
            updateNetworkStatus()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            updateNetworkStatus()
        }
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DataRepository(database.appSettingsDao(), database.visitDao())
        
        appSettings = repository.appSettings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

        allVisits = repository.allVisits.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Register connectivity callback
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        updateNetworkStatus()

        // Auto-start sync if enabled and role is set
        viewModelScope.launch {
            appSettings.collectLatest { settings ->
                if (settings != null && settings.isSyncEnabled && settings.syncRole != "NONE") {
                    // La lógica ahora vive en el SyncService, 
                    // evitamos iniciar el syncManager directamente aquí
                    // para no duplicar procesos.
                }
            }
        }

        // Initialize unique token
        viewModelScope.launch {
            appSettings.collectLatest { settings ->
                if (settings != null && settings.uniqueToken.isEmpty()) {
                    val androidId = Settings.Secure.getString(
                        application.contentResolver,
                        Settings.Secure.ANDROID_ID
                    ) ?: "unknown"
                    repository.saveSettings(settings.copy(uniqueToken = androidId))
                }
            }
        }
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            for (intl in interfaces) {
                // Priorizar interfaces de WiFi (wlan)
                if (!intl.name.contains("wlan") && !intl.name.contains("eth")) continue
                
                val addresses = intl.inetAddresses
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        Log.d("IP", "Found valid IP: ${addr.hostAddress} on ${intl.name}")
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("IP", "Error: ${e.message}")
        }
        return null
    }

    private fun updateNetworkStatus() {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        val status = when {
            (capabilities == null) || !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 0 // No WiFi
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> 2 // WiFi with Internet
            else -> 1 // WiFi without Internet
        }

        viewModelScope.launch {
            val current = repository.getSettingsOnce() ?: AppSettings()
            if (current.wifiStatus != status) {
                repository.saveSettings(current.copy(wifiStatus = status))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        connectivityManager.unregisterNetworkCallback(networkCallback)
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
            
            // Sync if emitter
            if (isSyncConnected.value && appSettings.value?.syncRole == "EMITTER") {
                syncManager.sendVisit(visit)
            }
        }
    }

    suspend fun getVisitDetail(id: Int): Visit? {
        return repository.getVisitById(id)
    }

    fun saveLanguage(language: String) {
        viewModelScope.launch {
            val current = repository.getSettingsOnce() ?: AppSettings()
            repository.saveSettings(current.copy(language = language))
        }
    }

    fun saveProfile(name: String, category: String) {
        viewModelScope.launch {
            val current = repository.getSettingsOnce() ?: AppSettings()
            repository.saveSettings(current.copy(
                businessName = name,
                businessCategory = category,
                isOnboardingCompleted = true,
                syncRole = "NONE" // Si crea perfil manual, deja de ser receptor de sincronización inicial
            ))
        }
    }

    fun updateSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getSettingsOnce() ?: AppSettings()
            val updated = current.copy(isSyncEnabled = enabled)
            repository.saveSettings(updated)
            if (enabled) {
                updateNetworkStatus()
            }
        }
    }

    fun updateVoiceSpeed(speed: Float) {
        viewModelScope.launch {
            val current = repository.getSettingsOnce() ?: AppSettings()
            repository.saveSettings(current.copy(voiceSpeed = speed))
        }
    }

    fun updateProfilePicture(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Asegurarse de que el tamaño final sea máximo 128x128
                val finalWidth = if (bitmap.width > 128) 128 else bitmap.width
                val finalHeight = if (bitmap.height > 128) 128 else bitmap.height
                
                val resizedBitmap = if (bitmap.width > 128 || bitmap.height > 128) {
                    Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
                } else {
                    bitmap
                }

                val outputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val byteArray = outputStream.toByteArray()

                val current = repository.getSettingsOnce() ?: AppSettings()
                repository.saveSettings(current.copy(profilePicture = byteArray))
                
                if (resizedBitmap != bitmap) resizedBitmap.recycle()
                // No reciclamos bitmap aquí porque viene de fuera y podría usarse para el preview
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateProfilePictureFromUri(uri: Uri) {
        val contentResolver = getApplication<Application>().contentResolver
        viewModelScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    originalBitmap?.let { updateProfilePicture(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startSyncAsEmitter(receiverData: SyncData) {
        lastSyncData = receiverData
        stopSync() // Asegurar que servicios previos se detengan
        SyncManager.reset("EMITTER")
        viewModelScope.launch {
            val current = repository.getSettingsOnce() ?: AppSettings()
            repository.saveSettings(current.copy(
                syncRole = "EMITTER",
                isOnboardingCompleted = true // Si sincroniza, ya completó el flujo
            ))
            
            // Iniciar servicio como EMISOR (el que escanea y envía)
            val intent = Intent(getApplication(), SyncService::class.java).apply {
                putExtra("role", "EMITTER")
                putExtra("serviceName", "yupay_${receiverData.token}")
                putExtra("targetDeviceName", receiverData.p2pDeviceName) // Used by Receiver to connect
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getApplication<Application>().startForegroundService(intent)
            } else {
                getApplication<Application>().startService(intent)
            }
        }
    }

    fun startSyncAsReceiver(targetData: SyncData? = null) {
        SyncManager.reset("RECEIVER")
        viewModelScope.launch {
            val current = repository.getSettingsOnce() ?: AppSettings()
            repository.saveSettings(current.copy(
                syncRole = "RECEIVER",
                isOnboardingCompleted = false,
                isSyncEnabled = true
            ))

            // Iniciar servicio como RECEPTOR (el que expone el QR y recibe, o el que escanea y busca)
            val intent = Intent(getApplication(), SyncService::class.java).apply {
                putExtra("role", "RECEIVER")
                putExtra("serviceName", "yupay_${current.uniqueToken}")
                putExtra("targetDeviceName", targetData?.p2pDeviceName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getApplication<Application>().startForegroundService(intent)
            } else {
                getApplication<Application>().startService(intent)
            }
        }
    }

    fun resetSyncProgress() {
        SyncManager.reset()
    }

    fun resetConnectionState() {
        syncManager.resetConnectionState()
        SyncManager.reset()
    }

    fun retrySyncAsEmitter() {
        lastSyncData?.let { startSyncAsEmitter(it) }
    }

    fun stopSync() {
        viewModelScope.launch {
            val intent = Intent(getApplication(), SyncService::class.java)
            getApplication<Application>().stopService(intent)
            SyncManager.reset()
        }
    }

    fun disconnectSync() {
        viewModelScope.launch {
            val current = repository.getSettingsOnce() ?: AppSettings()
            repository.saveSettings(current.copy(syncRole = "NONE", pairedToken = ""))
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                syncManager.disconnect()
            }
        }
    }
}
