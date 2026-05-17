package upch.mluque.final_project.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import upch.mluque.final_project.data.DataRepository
import upch.mluque.final_project.data.local.AppDatabase
import upch.mluque.final_project.data.local.AppSettings
import upch.mluque.final_project.data.local.Visit
import java.net.Inet4Address
import java.net.NetworkInterface

class SyncViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DataRepository
    private val syncManager: SyncManager
    private val nsdHelper = NsdHelper(application)

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private val _ticks = MutableStateFlow(0)
    val ticks = _ticks.asStateFlow()

    private val _syncCompleted = MutableSharedFlow<Unit>()
    val syncCompleted = _syncCompleted.asSharedFlow()

    private val _latency = MutableStateFlow(0L)
    val latency = _latency.asStateFlow()

    private val _acks = MutableStateFlow(Pair(0, 0)) // (Exitosos, Totales)
    val acks = _acks.asStateFlow()

    private var role = "CLIENT"
    private var lastRemoteIp: String? = null
    private var lastRemotePort: Int = 51234
    private var isProcessingRemoteUpdate = false

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
                    if (role == "CLIENT") {
                        sendAllData()
                    }
                } else {
                    // Intento de reconexión automática si somos clientes
                    if (role == "CLIENT" && lastRemoteIp != null) {
                        viewModelScope.launch {
                            delay(5000)
                            if (!_isConnected.value) connectToServer(lastRemoteIp!!, lastRemotePort)
                        }
                    }
                }
            }
        )
    }

    // Métodos para cambios locales que disparan el envío incremental
    fun addVisit(nationality: String, flag: String, price: String, services: String) {
        viewModelScope.launch {
            val visit = Visit(
                nationality = nationality,
                nationalityFlag = flag,
                priceApprox = price,
                services = services
            )
            // 1. Guardar localmente de inmediato
            repository.insertVisit(visit)
            
            // 2. Enviar solo esta nueva visita si hay conexión
            if (_isConnected.value) {
                syncManager.sendMessage(SyncMessage.NewVisit(visit))
                addLog("Nueva visita enviada")
            }
        }
    }

    fun saveProfile(name: String, category: String) {
        viewModelScope.launch {
            val current = repository.getSettingsOnce() ?: AppSettings()
            val updated = current.copy(businessName = name, businessCategory = category)
            
            repository.saveSettings(updated)
            
            if (_isConnected.value) {
                syncManager.sendMessage(SyncMessage.UpdateSettings(updated))
                addLog("Perfil actualizado y enviado")
            }
        }
    }

    fun startServer(port: Int) {
        role = "SERVER"
        lastRemotePort = port
        syncManager.startServer(port)
        nsdHelper.registerService(port)
        addLog("Servidor iniciado en puerto $port")
    }

    fun connectToServer(ip: String, port: Int) {
        role = "CLIENT"
        lastRemoteIp = ip
        lastRemotePort = port
        syncManager.connectTo(ip, port)
        addLog("Conectando a $ip:$port...")
    }

    private fun handleMessage(message: SyncMessage) {
        when (message) {
            is SyncMessage.SyncData -> {
                viewModelScope.launch {
                    isProcessingRemoteUpdate = true
                    repository.saveSettings(message.settings)
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
                    isProcessingRemoteUpdate = true
                    repository.saveSettings(message.settings)
                    addLog("Configuración actualizada remotamente")
                    _ticks.value++
                    isProcessingRemoteUpdate = false
                }
            }
            is SyncMessage.Handshake -> {
                addLog("Vínculo con: ${message.deviceName}")
                if (role == "CLIENT") {
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
                // El cliente también considera terminada su parte tras enviar
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
    }
}
