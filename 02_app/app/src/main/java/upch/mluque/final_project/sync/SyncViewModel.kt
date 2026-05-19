package upch.mluque.final_project.sync

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                    if (!isFullyLinked) {
                        isFullyLinked = true
                        saveSyncState()
                    }
                    // Asegurar que el onboarding se marque como completado en la DB persistente
                    markOnboardingCompleted()
                    
                    // Forzar resincronización de datos al reconectar (para cambios offline)
                    sendAllData()
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

        // Bucle de reconexión automática (tipo WhatsApp)
        startAutoReconnectLoop()
    }

    private fun startAutoReconnectLoop() {
        viewModelScope.launch {
            while (true) {
                // Si no hay conexión pero el dispositivo ya está vinculado, intentar reconectar
                if (!_isConnected.value && isFullyLinked && networkMonitor.isWifiConnected.value) {
                    if (_role.value == "CLIENT") {
                        nsdHelper.stopDiscovery() // Forzar reinicio de búsqueda
                    }
                    triggerReconnect()
                }
                delay(10000) // Reintentar cada 10 segundos
            }
        }
    }

    private fun triggerReconnect() {
        if (!networkMonitor.isWifiConnected.value) return
        
        when (_role.value) {
            "SERVER" -> {
                // El servidor siempre debe estar listo y anunciándose
                startServer(lastRemotePort)
            }
            "CLIENT" -> {
                if (isFullyLinked) {
                    discoverAndConnect()
                }
            }
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
                isOnboardingCompleted = true,
                lastModified = System.currentTimeMillis()
            )
            repository.saveSettings(updated)
        }
    }

    /**
     * Asegura que el onboarding se marque como completado en la base de datos persistente.
     * Esto es crucial cuando se vincula como dispositivo adicional.
     */
    fun markOnboardingCompleted() {
        viewModelScope.launch {
            val current = repository.getSettingsOnce() ?: AppSettings()
            if (!current.isOnboardingCompleted) {
                repository.saveSettings(current.copy(
                    isOnboardingCompleted = true,
                    lastModified = System.currentTimeMillis()
                ))
                addLog("Estado de onboarding persistido")
            }
        }
    }

    fun startServer(port: Int) {
        _isConnected.value = false // Reiniciar estado para evitar saltos de UI
        _role.value = "SERVER"
        lastRemotePort = port
        saveSyncState()
        syncManager.startServer(port)
        nsdHelper.registerService(port)
        addLog("Servidor iniciado en puerto $port")
    }

    fun connectToServer(ip: String, port: Int) {
        _isConnected.value = false // Reiniciar estado para evitar saltos de UI
        _role.value = "CLIENT"
        lastRemoteIp = ip
        lastRemotePort = port
        saveSyncState()
        syncManager.connectTo(ip, port)
        addLog("Conectando a $ip:$port...")
    }

    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            // 1. Detener servicios de red inmediatamente
            syncManager.stop()
            nsdHelper.stop()
            
            // 2. Reiniciar estados internos (Memoria)
            resetInternalState()
            
            // 3. Limpiar estado de sincronización persistente y preferencias
            isFullyLinked = false
            lastRemoteIp = null
            sharedPrefs.edit().clear().apply()
            
            // 4. Notificar navegación inmediata
            withContext(Dispatchers.Main) {
                onComplete()
            }

            // 5. Limpiar base de datos local en segundo plano
            withContext(Dispatchers.IO) {
                repository.clearAllData()
                // Asegurar que Room guarde los cambios en disco inmediatamente
                AppDatabase.getDatabase(getApplication()).openHelper.writableDatabase.execSQL("PRAGMA checkpoint(FULL)")
            }
        }
    }

    private fun resetInternalState() {
        _isConnected.value = false
        _remoteDeviceName.value = null
        _logs.value = emptyList()
        _ticks.value = 0
        _latency.value = 0L
        _acks.value = Pair(0, 0)
        isProcessingRemoteUpdate = false
    }

    fun requestRemoteLogout(onComplete: () -> Unit = {}) {
        if (_isConnected.value) {
            syncManager.sendMessage(SyncMessage.RemoteLogout)
            addLog("Solicitando reset remoto al servidor...")
            // Opcionalmente el cliente también puede limpiar su vínculo local
            // pero mantenemos el cliente con sus datos, solo rompe el puente
            viewModelScope.launch {
                delay(500) // Dar tiempo a enviar el mensaje
                syncManager.stop()
                _isConnected.value = false
                _remoteDeviceName.value = null
                saveSyncState()
                onComplete()
            }
        }
    }

    fun disconnectAllRemotes(onComplete: () -> Unit = {}) {
        // En este contexto, el cliente cierra su conexión actual de forma definitiva
        requestRemoteLogout(onComplete)
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
                    var settingsSaved = false
                    try {
                        val localSettings = repository.getSettingsOnce()
                        val remoteSettings = message.settings
                        
                        val shouldUpdateLocal = localSettings == null || 
                            (remoteSettings.businessName.isNotBlank() && localSettings.businessName.isBlank()) ||
                            (remoteSettings.lastModified > localSettings.lastModified)

                        if (shouldUpdateLocal) {
                            isProcessingRemoteUpdate = true
                            settingsSaved = true
                            repository.saveSettings(remoteSettings)
                            addLog("Ajustes sincronizados desde dispositivo remoto")
                        } else if (localSettings != null && localSettings.lastModified > remoteSettings.lastModified) {
                            syncManager.sendMessage(SyncMessage.UpdateSettings(localSettings))
                            addLog("Ajustes locales conservados y enviados al remoto")
                        }
                        
                        message.visits.forEach { repository.insertVisit(it) }
                        addLog("Sincronización inicial completa")
                        _ticks.value++
                        _syncCompleted.emit(Unit)
                    } catch (e: Exception) {
                        Log.e("SyncViewModel", "Error in SyncData", e)
                    } finally {
                        if (settingsSaved) {
                            delay(500)
                        }
                        isProcessingRemoteUpdate = false
                    }
                }
            }
            is SyncMessage.NewVisit -> {
                viewModelScope.launch {
                    isProcessingRemoteUpdate = true
                    try {
                        repository.insertVisit(message.visit)
                        addLog("Nueva visita recibida")
                        _ticks.value++
                    } finally {
                        delay(500)
                        isProcessingRemoteUpdate = false
                    }
                }
            }
            is SyncMessage.UpdateSettings -> {
                viewModelScope.launch {
                    var settingsSaved = false
                    try {
                        val localSettings = repository.getSettingsOnce()
                        if (localSettings == null || message.settings.lastModified > localSettings.lastModified) {
                            isProcessingRemoteUpdate = true
                            settingsSaved = true
                            repository.saveSettings(message.settings)
                            addLog("Ajustes actualizados remotamente")
                        }
                        _ticks.value++
                    } finally {
                        if (settingsSaved) {
                            delay(500)
                        }
                        isProcessingRemoteUpdate = false
                    }
                }
            }
            is SyncMessage.Handshake -> {
                viewModelScope.launch {
                    _remoteDeviceName.value = message.deviceName
                    saveSyncState()
                    addLog("Vínculo establecido con: ${message.deviceName}")
                    
                    // Pequeña espera para asegurar que el socket esté listo para el flujo de datos
                    delay(500)
                    sendAllData()
                }
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
            is SyncMessage.RemoteLogout -> {
                addLog("Cierre de sesión remoto solicitado")
                logout()
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
