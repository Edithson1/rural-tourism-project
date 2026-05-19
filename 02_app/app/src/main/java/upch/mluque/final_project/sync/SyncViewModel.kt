package upch.mluque.final_project.sync

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import upch.mluque.final_project.data.DataRepository
import upch.mluque.final_project.data.local.AppDatabase
import upch.mluque.final_project.data.local.AppSettings
import upch.mluque.final_project.data.local.Visit
import upch.mluque.final_project.utils.NetworkMonitor
import java.net.Inet4Address
import java.net.NetworkInterface

class SyncViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DataRepository
    private val syncManager: SyncManager
    private val nsdHelper = NsdHelper(application)
    private val networkMonitor = NetworkMonitor(application)
    private val sharedPrefs = application.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _role = MutableStateFlow(sharedPrefs.getString("last_role", "CLIENT") ?: "CLIENT")
    val role = _role.asStateFlow()

    private val _remoteDeviceName = MutableStateFlow<String?>(sharedPrefs.getString("remote_device_name", null))
    val remoteDeviceName = _remoteDeviceName.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private val _ticks = MutableStateFlow(0)
    val ticks = _ticks.asStateFlow()

    private val _syncCompleted = MutableSharedFlow<Unit>()
    val syncCompleted = _syncCompleted.asSharedFlow()

    private val _latency = MutableStateFlow(0L)
    val latency = _latency.asStateFlow()

    private val _acks = MutableStateFlow(Pair(0, 0))
    val acks = _acks.asStateFlow()

    private var lastRemoteIp: String? = sharedPrefs.getString("last_ip", null)
    private var lastRemotePort: Int = sharedPrefs.getInt("last_port", 51234)
    private var isProcessingRemoteUpdate = false
    
    // Indica si el dispositivo ya ha sido vinculado mediante QR alguna vez
    private var isFullyLinked: Boolean = sharedPrefs.getBoolean("is_fully_linked", false)

    init {
        val db = AppDatabase.getDatabase(application)
        repository = DataRepository(db.appSettingsDao(), db.visitDao())
        
        syncManager = SyncManager(
            onMessageReceived = { handleMessage(it) },
            onConnectionStatusChanged = { 
                _isConnected.value = it
                addLog(if (it) "Conectado" else "Desconectado")
                if (it) {
                    syncManager.sendMessage(SyncMessage.Handshake(android.os.Build.MODEL, "SESSION"))
                    // Al conectar por primera vez (vía QR o reconexión), marcamos como vinculado
                    if (!isFullyLinked && _role.value == "CLIENT") {
                        isFullyLinked = true
                        saveSyncState()
                    }
                }
            }
        )

        // Observar cambios en settings para sincronización automática
        repository.appSettings
            .onEach { settings ->
                if (settings != null && !isProcessingRemoteUpdate && _isConnected.value) {
                    syncManager.sendMessage(SyncMessage.UpdateSettings(settings))
                    addLog("Sincronizando cambios de ajustes...")
                }
            }
            .launchIn(viewModelScope)

        // Monitor de Red para Reconexión Automática
        networkMonitor.start()
        
        // Disparar reconexión inicial si ya estamos en WiFi
        if (networkMonitor.isWifiConnected.value) {
            triggerReconnect()
        }

        networkMonitor.isWifiConnected
            .onEach { isWifi ->
                if (isWifi) {
                    addLog("WiFi detectado")
                    triggerReconnect()
                } else {
                    addLog("WiFi perdido")
                    _isConnected.value = false
                }
            }
            .launchIn(viewModelScope)
    }

    private fun triggerReconnect() {
        if (_role.value == "SERVER") {
            startServer(lastRemotePort)
        } else if (_role.value == "CLIENT" && isFullyLinked) {
            discoverAndConnect()
        }
    }

    private fun discoverAndConnect() {
        addLog("Buscando dispositivos en la red...")
        nsdHelper.discoverServices { info ->
            if (!_isConnected.value) {
                connectToServer(info.host.hostAddress ?: "", info.port)
            }
        }
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
            
            if (_isConnected.value) {
                syncManager.sendMessage(SyncMessage.NewVisit(visit))
                addLog("Nueva visita enviada")
            }
        }
    }

    fun saveProfile(name: String, category: String) {
        viewModelScope.launch {
            val current = repository.getSettingsOnce() ?: AppSettings()
            val updated = current.copy(
                businessName = name, 
                businessCategory = category,
                isOnboardingCompleted = true
            )
            repository.saveSettings(updated)
        }
    }

    fun startServer(port: Int) {
        _role.value = "SERVER"
        lastRemotePort = port
        saveSyncState()
        syncManager.startServer(port)
        nsdHelper.registerService(port)
        addLog("Servidor iniciado en puerto $port")
    }

    fun connectToServer(ip: String, port: Int) {
        _role.value = "CLIENT"
        lastRemoteIp = ip
        lastRemotePort = port
        saveSyncState()
        syncManager.connectTo(ip, port)
        addLog("Conectando a $ip:$port...")
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            // 1. Desconectar
            syncManager.stop()
            nsdHelper.stop()
            _isConnected.value = false
            
            // 2. Limpiar estado de sincronización
            isFullyLinked = false
            _remoteDeviceName.value = null
            lastRemoteIp = null
            sharedPrefs.edit().clear().apply()
            
            // 3. Limpiar base de datos local (opcional, pero sugerido para "comenzar cuenta nueva")
            // No borramos todo por seguridad, solo reseteamos onboarding si es necesario
            val current = repository.getSettingsOnce() ?: AppSettings()
            repository.saveSettings(current.copy(isOnboardingCompleted = false))
            
            addLog("Sesión cerrada")
            onComplete()
        }
    }

    private fun saveSyncState() {
        sharedPrefs.edit().apply {
            putString("last_role", _role.value)
            putString("last_ip", lastRemoteIp)
            putInt("last_port", lastRemotePort)
            putBoolean("is_fully_linked", isFullyLinked)
            putString("remote_device_name", _remoteDeviceName.value)
            apply()
        }
    }

    private fun handleMessage(message: SyncMessage) {
        when (message) {
            is SyncMessage.SyncData -> {
                viewModelScope.launch {
                    isProcessingRemoteUpdate = true
                    val localSettings = repository.getSettingsOnce()
                    
                    // Resolución de conflictos: Solo actualizar si el remoto es más reciente
                    if (localSettings == null || message.settings.lastModified > localSettings.lastModified) {
                        repository.saveSettings(message.settings)
                        addLog("Ajustes sincronizados (remoto más reciente)")
                    } else {
                        addLog("Ajustes locales conservados (más recientes)")
                        // Si el local es más reciente, forzar el envío de vuelta para corregir al otro
                        syncManager.sendMessage(SyncMessage.UpdateSettings(localSettings))
                    }
                    
                    message.visits.forEach { repository.insertVisit(it) }
                    addLog("Sincronización inicial completa")
                    _ticks.value++
                    _syncCompleted.emit(Unit)
                    isProcessingRemoteUpdate = false
                }
            }
            is SyncMessage.NewVisit -> {
                viewModelScope.launch {
                    isProcessingRemoteUpdate = true
                    repository.insertVisit(message.visit)
                    addLog("Nueva visita recibida")
                    _ticks.value++
                    isProcessingRemoteUpdate = false
                }
            }
            is SyncMessage.UpdateSettings -> {
                viewModelScope.launch {
                    val localSettings = repository.getSettingsOnce()
                    if (localSettings == null || message.settings.lastModified > localSettings.lastModified) {
                        isProcessingRemoteUpdate = true
                        repository.saveSettings(message.settings)
                        addLog("Ajustes actualizados remotamente")
                        isProcessingRemoteUpdate = false
                    }
                    _ticks.value++
                }
            }
            is SyncMessage.Handshake -> {
                _remoteDeviceName.value = message.deviceName
                saveSyncState()
                addLog("Vínculo con: ${message.deviceName}")
                sendAllData()
            }
            is SyncMessage.Ping -> {
                syncManager.sendMessage(SyncMessage.Pong(message.timestamp))
                _ticks.value++
            }
            is SyncMessage.Pong -> {
                val now = System.currentTimeMillis()
                val currentLatency = now - message.timestamp
                _latency.value = currentLatency
                
                val currentAcks = _acks.value
                _acks.value = Pair(currentAcks.first + 1, currentAcks.second + 1)
                
                addLog("ACK recibido: ${currentLatency}ms")
                _ticks.value++
            }
            is SyncMessage.Error -> {
                val currentAcks = _acks.value
                _acks.value = Pair(currentAcks.first, currentAcks.second + 1)
                addLog("Error remoto: ${message.message}")
            }
        }
    }

    fun sendPing() {
        val currentAcks = _acks.value
        _acks.value = Pair(currentAcks.first, currentAcks.second + 1)
        syncManager.sendMessage(SyncMessage.Ping(System.currentTimeMillis()))
        addLog("Enviando señal (Ping)...")
    }

    private fun sendAllData() {
        viewModelScope.launch {
            val settings = repository.getSettingsOnce()
            val visits = repository.allVisits.first()
            if (settings != null) {
                syncManager.sendMessage(SyncMessage.SyncData(settings, visits))
                addLog("Enviando configuración y ${visits.size} visitas...")
                _syncCompleted.emit(Unit)
            }
        }
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: "0.0.0.0"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "0.0.0.0"
    }

    private fun addLog(message: String) {
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(0, "[${System.currentTimeMillis() % 100000}] $message")
        if (currentLogs.size > 20) currentLogs.removeAt(20)
        _logs.value = currentLogs
    }

    override fun onCleared() {
        super.onCleared()
        syncManager.stop()
        nsdHelper.stop()
        networkMonitor.stop()
    }
}
