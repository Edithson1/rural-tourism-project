package yupay.turismo.sync

import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yupay.turismo.data.AppReset
import yupay.turismo.di.ServiceLocator
import yupay.turismo.data.DataRepository
import yupay.turismo.data.local.AppDatabase
import yupay.turismo.data.local.AppSettings
import yupay.turismo.data.local.DiscountType
import yupay.turismo.data.local.Product
import yupay.turismo.data.local.SelectedProduct
import yupay.turismo.data.local.Visit
import yupay.turismo.utils.NetworkMonitor
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

    // Reset total del dispositivo SERVIDOR al cancelar la vinculación P2P (desde cualquier lado):
    // la UI lo observa para navegar al onboarding (estado de fábrica).
    private val _resetEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val resetEvent = _resetEvent.asSharedFlow()

    private val _latency = MutableStateFlow(0L)
    val latency = _latency.asStateFlow()

    private val _acks = MutableStateFlow(Pair(0, 0))
    val acks = _acks.asStateFlow()

    private var lastRemoteIp: String? = sharedPrefs.getString("last_ip", null)
    private var lastRemotePort: Int = sharedPrefs.getInt("last_port", 51234)
    private var isProcessingRemoteUpdate = false

    // Puente P2P → nube: sube a la API los cambios que llegan por LAN (con debounce).
    private var cloudBridgeJob: Job? = null
    
    // Heartbeat tracking
    private var lastResponseTimestamp: Long = 0L
    
    // Indica si el dispositivo ya ha sido vinculado mediante QR alguna vez
    private var isFullyLinked: Boolean = sharedPrefs.getBoolean("is_fully_linked", false)

    init {
        val db = AppDatabase.getDatabase(application)
        repository = DataRepository(db.appSettingsDao(), db.visitDao(), db.productDao())
        
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

        repository.allProducts
            .onEach { products ->
                if (!isProcessingRemoteUpdate && _isConnected.value) {
                    syncManager.sendMessage(SyncMessage.UpdateProducts(products))
                    addLog("Sincronizando catálogo de productos...")
                }
            }
            .launchIn(viewModelScope)

        // Observar visitas: propaga al peer las altas y, sobre todo, los cambios de estado
        // (isSent/remoteId) y las visitas que bajan de la nube. El handler de UpdateVisits sólo
        // escribe si hay cambio real, lo que corta cualquier bucle de reenvío entre ambos lados.
        repository.allVisits
            .onEach { visits ->
                if (!isProcessingRemoteUpdate && _isConnected.value) {
                    syncManager.sendMessage(SyncMessage.UpdateVisits(visits))
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

    fun addVisit(
        nationality: String,
        flag: String,
        selectedProducts: List<SelectedProduct>,
        subtotal: Double,
        discountValue: Double,
        discountType: DiscountType,
        totalAmount: Double
    ) {
        viewModelScope.launch {
            val currentSettings = repository.getSettingsOnce()
            val visit = Visit(
                deviceId = currentSettings?.deviceId ?: "",
                nationality = nationality,
                nationalityFlag = flag,
                selectedProducts = selectedProducts,
                subtotal = subtotal,
                discountValue = discountValue,
                discountType = discountType,
                totalAmount = totalAmount
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

        // Cambiar ID a modo servidor
        viewModelScope.launch {
            val current = repository.getSettingsOnce()
            if (current != null) {
                repository.saveSettings(current.copy(
                    deviceId = "SERVER_${current.hardwareDeviceId}",
                    lastModified = System.currentTimeMillis()
                ))
            }
        }

        syncManager.startServer(port)
        nsdHelper.registerService(port)
        addLog("Servidor iniciado en puerto $port")
    }

    fun cancelServerMode() {
        if (_role.value == "SERVER" && !isFullyLinked && !_isConnected.value) {
            addLog("Cancelando modo servidor (No vinculado)")
            syncManager.stop()
            nsdHelper.stop()
            
            _role.value = "CLIENT"
            
            // Restaurar ID original
            viewModelScope.launch {
                val current = repository.getSettingsOnce()
                if (current != null && current.deviceId != current.hardwareDeviceId) {
                    repository.saveSettings(current.copy(
                        deviceId = current.hardwareDeviceId,
                        lastModified = System.currentTimeMillis()
                    ))
                }
            }
            saveSyncState()
        }
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
                // El SERVIDOR resetea a fábrica al cancelar el P2P → la UI navega al onboarding
                // (cubre el caso en que el corte lo provoca el CLIENTE vía RemoteLogout).
                _resetEvent.tryEmit(Unit)
            }

            // 6. Reset TOTAL en segundo plano: igual que el "cerrar sesión" de cuenta online
            //    (sesión de nube + outbox + Room + re-siembra de ajustes por defecto).
            withContext(Dispatchers.IO) {
                AppReset.factoryReset(getApplication())
                // Asegurar que Room guarde los cambios en disco inmediatamente
                AppDatabase.getDatabase(getApplication()).openHelper.writableDatabase.execSQL("PRAGMA checkpoint(FULL)")
            }
        }
    }

    private fun resetInternalState() {
        _isConnected.value = false
        _role.value = "CLIENT"
        _remoteDeviceName.value = null
        _logs.value = emptyList()
        _ticks.value = 0
        _latency.value = 0L
        _acks.value = Pair(0, 0)
        isProcessingRemoteUpdate = false
        lastRemoteIp = null
        lastRemotePort = 51234
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

        // Restaurar ID original si era servidor
        viewModelScope.launch {
            val current = repository.getSettingsOnce()
            if (current != null && current.deviceId != current.hardwareDeviceId) {
                repository.saveSettings(current.copy(
                    deviceId = current.hardwareDeviceId,
                    lastModified = System.currentTimeMillis()
                ))
            }
        }

        resetInternalState()
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

    fun handleMessage(message: SyncMessage) {
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

                        // Fusión de Productos (Catálogo)
                        val remoteProducts = message.products
                        if (remoteProducts.isNotEmpty()) {
                            repository.replaceAllProducts(remoteProducts.map { it.copy(id = 0) })
                            addLog("Catálogo de productos actualizado")
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

                        // Si este equipo tiene cuenta online, propaga lo recibido por P2P a la nube.
                        bridgeP2pChangesToCloud()

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
                            bridgeP2pChangesToCloud()
                            _ticks.value++
                        }
                    } finally {
                        delay(500)
                        isProcessingRemoteUpdate = false
                    }
                }
            }
            is SyncMessage.UpdateVisits -> {
                viewModelScope.launch {
                    isProcessingRemoteUpdate = true
                    try {
                        val locals = repository.allVisits.first()
                        var changed = false
                        for (rv in message.visits) {
                            val existing = locals.firstOrNull { it.registrationDate == rv.registrationDate }
                            if (existing == null) {
                                // Visita nueva (id=0 → autogenera; evita colisión de ID local).
                                repository.insertVisit(rv.copy(id = 0))
                                changed = true
                            } else {
                                // Merge idempotente y monótono de los campos de estado de sync.
                                // Sólo se escribe si hay diferencia real → corta el bucle de reenvío.
                                val merged = existing.copy(
                                    remoteId = existing.remoteId ?: rv.remoteId,
                                    isSent = existing.isSent || rv.isSent,
                                    sentDate = existing.sentDate ?: rv.sentDate
                                )
                                if (merged != existing) {
                                    repository.insertVisit(merged) // misma PK = update (REPLACE)
                                    changed = true
                                }
                            }
                        }
                        if (changed) {
                            addLog("Visitas reconciliadas con el remoto")
                            bridgeP2pChangesToCloud()
                            _ticks.value++
                        }
                    } catch (e: Exception) {
                        Log.e("SyncViewModel", "Error en UpdateVisits", e)
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
                            bridgeP2pChangesToCloud()
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
            is SyncMessage.UpdateProducts -> {
                viewModelScope.launch {
                    isProcessingRemoteUpdate = true
                    try {
                        repository.replaceAllProducts(message.products.map { it.copy(id = 0) })
                        addLog("Catálogo de productos actualizado remotamente")
                        bridgeP2pChangesToCloud()
                        _ticks.value++
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

    /**
     * Sube a la nube (sólo si ESTE equipo tiene una cuenta online) los cambios que acaban de
     * entrar por P2P. Con debounce para agrupar ráfagas de mensajes de la LAN. Si el equipo no
     * tiene cuenta online, no hace nada: el P2P sigue funcionando igual que antes.
     */
    private fun bridgeP2pChangesToCloud() {
        cloudBridgeJob?.cancel()
        cloudBridgeJob = viewModelScope.launch {
            delay(1500) // debounce: agrupa la ráfaga de mensajes P2P en una sola subida
            try {
                ServiceLocator.init(getApplication())
                if (ServiceLocator.sessionManager.isLoggedIn()) {
                    addLog("Subiendo cambios a la nube...")
                    ServiceLocator.cloudSyncEngine.reconcileFromP2p()
                }
            } catch (e: Exception) {
                Log.e("SyncViewModel", "Error subiendo cambios P2P a la nube", e)
            }
        }
    }

    private fun sendAllData() {
        viewModelScope.launch {
            val settings = repository.getSettingsOnce()
            val visits = repository.allVisits.first()
            val products = repository.allProducts.first()
            if (settings != null) {
                syncManager.sendMessage(SyncMessage.SyncData(settings, visits, products))
                addLog("Enviando configuración, ${visits.size} visitas y ${products.size} productos...")
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
