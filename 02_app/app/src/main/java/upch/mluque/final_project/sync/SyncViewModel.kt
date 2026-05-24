package upch.mluque.final_project.sync

import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
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
    
    private val wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

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
    
    // Heartbeat tracking
    private var lastResponseTimestamp: Long = 0L
    
    // Indica si el dispositivo ya ha sido vinculado mediante QR alguna vez
    private var isFullyLinked: Boolean = sharedPrefs.getBoolean("is_fully_linked", false)

    init {
        val db = AppDatabase.getDatabase(application)
        repository = DataRepository(db.appSettingsDao(), db.visitDao())
        
        // Adquirir MulticastLock para asegurar descubrimiento NSD
        try {
            multicastLock = wifiManager.createMulticastLock("YupaySyncLock").apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.e("SyncViewModel", "Could not acquire MulticastLock", e)
        }
        
        syncManager = SyncManager(
            onMessageReceived = { handleMessage(it) },
            onConnectionStatusChanged = { 
                _isConnected.value = it
                addLog(if (it) "Conectado" else "Desconectado")
                if (it) {
                    syncManager.sendMessage(SyncMessage.Handshake(android.os.Build.MODEL, "SESSION"))
                    // Solo marcamos como vinculado en el intercambio de datos (SyncData)
                    // para poder diferenciar la "primera sincronización"
                    
                    // Asegurar que el onboarding se marque como completado en la DB persistente
                    markOnboardingCompleted()
                    
                    // Ya no disparamos sendAllData aquí inmediatamente, 
                    // esperamos al Handshake del otro para mayor estabilidad
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

        // Bucle de Heartbeat (Detección activa de desconexión)
        startHeartbeatLoop()
    }

    private fun startHeartbeatLoop() {
        viewModelScope.launch {
            while (true) {
                if (_isConnected.value) {
                    // 1. Enviar Ping cada 2 segundos
                    sendPing()
                    
                    // 2. Verificar timeout (5 segundos)
                    val now = System.currentTimeMillis()
                    if (lastResponseTimestamp > 0 && (now - lastResponseTimestamp) > 5000) {
                        addLog("Timeout detectado (5s sin respuesta)")
                        _isConnected.value = false
                        syncManager.stop() // Forzar cierre de socket
                    }
                }
                delay(2000)
            }
        }
    }

    private fun startAutoReconnectLoop() {
        viewModelScope.launch {
            while (true) {
                val hasLocalIp = getLocalIpAddress() != "0.0.0.0"
                // Si no hay conexión pero el dispositivo ya está vinculado, intentar reconectar
                if (!_isConnected.value && isFullyLinked && (networkMonitor.isWifiConnected.value || hasLocalIp)) {
                    if (_role.value == "CLIENT") {
                        // No detenemos discovery aquí, dejamos que NsdHelper maneje la concurrencia
                        triggerReconnect()
                    } else if (_role.value == "SERVER") {
                        // Asegurar que el servidor esté vivo si tenemos IP
                        if (!syncManager.isServerRunning()) {
                            triggerReconnect()
                        }
                    }
                }
                delay(8000) // Reintentar cada 8 segundos para mayor agilidad
            }
        }
    }

    private fun triggerReconnect() {
        val hasLocalIp = getLocalIpAddress() != "0.0.0.0"
        if (!networkMonitor.isWifiConnected.value && !hasLocalIp) return
        
        when (_role.value) {
            "SERVER" -> {
                // El servidor siempre debe estar listo y anunciándose
                if (!syncManager.isServerRunning()) {
                    startServer(lastRemotePort)
                }
            }
            "CLIENT" -> {
                if (isFullyLinked && !_isConnected.value) {
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
        // Limpieza profunda antes de iniciar un nuevo servidor
        syncManager.stop()
        nsdHelper.stop()
        
        _isConnected.value = false 
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
            // 1. Notificar al otro dispositivo antes de cerrar si estamos conectados
            if (_isConnected.value) {
                try {
                    syncManager.sendMessage(SyncMessage.RemoteLogout)
                    delay(300) // Dar un breve tiempo para que el mensaje se envíe
                } catch (e: Exception) {
                    Log.e("SyncViewModel", "Error enviando RemoteLogout", e)
                }
            }

            // 2. Detener servicios de red inmediatamente
            syncManager.stop()
            nsdHelper.stop()
            
            // 3. Reiniciar estados internos (Memoria)
            resetInternalState()
            
            // 4. Limpiar estado de sincronización persistente y preferencias
            isFullyLinked = false
            lastRemoteIp = null
            sharedPrefs.edit().clear().apply()
            
            // 5. Notificar navegación inmediata
            withContext(Dispatchers.Main) {
                onComplete()
            }

            // 6. Limpiar base de datos local en segundo plano
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
            viewModelScope.launch {
                try {
                    syncManager.sendMessage(SyncMessage.RemoteLogout)
                    delay(500) // Dar tiempo a enviar el mensaje
                } catch (e: Exception) {
                    Log.e("SyncViewModel", "Error enviando RemoteLogout", e)
                }
                
                // Desvincular localmente sin borrar datos
                unlink()
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        } else {
            // Si no hay conexión, al menos desvinculamos localmente
            unlink()
            onComplete()
        }
    }

    fun disconnectAllRemotes(onComplete: () -> Unit = {}) {
        requestRemoteLogout(onComplete)
    }

    private fun unlink() {
        syncManager.stop()
        nsdHelper.stop()
        isFullyLinked = false
        _isConnected.value = false
        _remoteDeviceName.value = null
        lastRemoteIp = null
        saveSyncState()
        addLog("Dispositivo desvinculado")
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
        // Cualquier mensaje recibido actualiza el timestamp de "vida"
        lastResponseTimestamp = System.currentTimeMillis()

        when (message) {
            is SyncMessage.SyncData -> {
                viewModelScope.launch {
                    var needsSyncBack = false
                    isProcessingRemoteUpdate = true
                    try {
                        val localSettings = repository.getSettingsOnce()
                        val remoteSettings = message.settings
                        val isFirstLink = !isFullyLinked
                        
                        // Lógica de Prioridad de Ajustes (Flujo de Vinculación):
                        val shouldUpdateLocalSettings = when {
                            // 1. Vinculación inicial: El SERVIDOR siempre es una copia del CLIENTE
                            isFirstLink && _role.value == "SERVER" -> {
                                addLog("Vinculación inicial: Clonando perfil del Cliente")
                                true
                            }
                            // 2. Vinculación inicial: El CLIENTE JAMÁS adopta lo del servidor (él es la fuente)
                            isFirstLink && _role.value == "CLIENT" -> {
                                addLog("Vinculación inicial: Ignorando datos del servidor")
                                false
                            }
                            // 3. Ya vinculados: El cambio más reciente gana (por marca de tiempo)
                            else -> localSettings == null || remoteSettings.lastModified > localSettings.lastModified
                        }

                        if (shouldUpdateLocalSettings) {
                            repository.saveSettings(remoteSettings)
                            if (isFirstLink) {
                                addLog("Perfil sincronizado desde el dispositivo principal")
                            } else {
                                addLog("Ajustes actualizados (Cambio remoto más reciente)")
                            }
                        } else if (localSettings != null && (isFirstLink || localSettings.lastModified > remoteSettings.lastModified)) {
                            // En vinculación inicial (siendo cliente) o si local es más nuevo, forzamos sync de vuelta
                            needsSyncBack = true
                        }

                        // Fusión de Visitas (Sigue siendo Unión Real)
                        val localVisits = repository.allVisits.first()
                        val localTimestamps = localVisits.map { it.registrationDate }.toSet()
                        val remoteVisits = message.visits
                        
                        val newVisitsForLocal = remoteVisits.filter { it.registrationDate !in localTimestamps }
                        
                        if (newVisitsForLocal.isNotEmpty()) {
                            newVisitsForLocal.forEach { remoteVisit ->
                                repository.insertVisit(remoteVisit.copy(id = 0))
                            }
                            addLog("Fusionadas ${newVisitsForLocal.size} visitas del remoto")
                        }

                        val remoteTimestamps = remoteVisits.map { it.registrationDate }.toSet()
                        val missingInRemote = localVisits.any { it.registrationDate !in remoteTimestamps }
                        
                        if (missingInRemote || needsSyncBack) {
                            addLog("Sincronizando datos locales hacia el remoto...")
                            delay(500)
                            sendAllData()
                        } else {
                            addLog("Sincronización completa: Dispositivos en equilibrio")
                        }

                        // Marcar como vinculado definitivamente al completar el primer intercambio
                        if (!isFullyLinked) {
                            isFullyLinked = true
                            saveSyncState()
                        }

                        _ticks.value++
                        _syncCompleted.emit(Unit)
                    } catch (e: Exception) {
                        Log.e("SyncViewModel", "Error en SyncData", e)
                    } finally {
                        delay(500)
                        isProcessingRemoteUpdate = false
                    }
                }
            }
            is SyncMessage.NewVisit -> {
                viewModelScope.launch {
                    isProcessingRemoteUpdate = true
                    try {
                        val localVisits = repository.allVisits.first()
                        if (localVisits.none { it.registrationDate == message.visit.registrationDate }) {
                            // Resetear ID para evitar sobrescribir visitas locales por colisión de ID
                            repository.insertVisit(message.visit.copy(id = 0))
                            addLog("Nueva visita recibida")
                            _ticks.value++
                        }
                    } finally {
                        delay(500)
                        isProcessingRemoteUpdate = false
                    }
                }
            }
            is SyncMessage.UpdateSettings -> {
                viewModelScope.launch {
                    try {
                        val localSettings = repository.getSettingsOnce()
                        val isFirstLink = !isFullyLinked
                        
                        // En vinculación inicial, ignoramos ajustes que vengan del servidor
                        if (isFirstLink && _role.value == "CLIENT") return@launch

                        if (localSettings == null || message.settings.lastModified > localSettings.lastModified) {
                            isProcessingRemoteUpdate = true
                            repository.saveSettings(message.settings)
                            addLog("Ajustes actualizados remotamente")
                            _ticks.value++
                        } else if (localSettings.lastModified > message.settings.lastModified) {
                            // Si recibimos un ajuste viejo, forzamos el envío de nuestro ajuste nuevo
                            syncManager.sendMessage(SyncMessage.UpdateSettings(localSettings))
                        }
                    } finally {
                        delay(500)
                        isProcessingRemoteUpdate = false
                    }
                }
            }
            is SyncMessage.Handshake -> {
                viewModelScope.launch {
                    _remoteDeviceName.value = message.deviceName
                    saveSyncState()
                    addLog("Vínculo establecido con: ${message.deviceName}")
                    
                    // Al recibir el Handshake del otro, enviamos nuestros datos
                    // El Cliente inicia con prioridad, el servidor responde
                    delay(if (_role.value == "CLIENT") 300 else 1000)
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
                addLog("Cierre de sesión remoto detectado")
                if (_role.value == "SERVER") {
                    // Si el servidor es desvinculado por el cliente, se resetea por completo
                    logout()
                } else {
                    // Si el cliente detecta que el servidor se fue, solo desvincula
                    unlink()
                }
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
        try {
            multicastLock?.let { if (it.isHeld) it.release() }
        } catch (e: Exception) {
            Log.e("SyncViewModel", "Error releasing MulticastLock", e)
        }
    }
}
