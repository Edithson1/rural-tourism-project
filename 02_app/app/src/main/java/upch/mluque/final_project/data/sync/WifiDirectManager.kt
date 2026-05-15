package upch.mluque.final_project.data.sync

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import upch.mluque.final_project.data.local.Visit
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

@Serializable
data class SyncData(
    val serviceName: String, 
    val token: String,
    val businessName: String? = null,
    val businessCategory: String? = null,
    val p2pDeviceName: String? = null, // WiFi Direct target name
    val p2pPort: Int = 8888,          // Fixed TCP port
    val role: String? = null
)

@Serializable
sealed class SyncPayload {
    @Serializable
    data class VisitPayload(val visit: Visit) : SyncPayload()
    @Serializable
    data class InitialSyncPayload(
        val businessName: String,
        val businessCategory: String,
        val profilePicture: ByteArray? = null,
        val visits: List<Visit>
    ) : SyncPayload()
}

/**
 * WiFi Direct Manager replacing WifiSyncManager
 * Supports 1 Group Owner (Server) + Multiple Clients
 */
class WifiDirectManager(private val context: Context) {
    private val tag = "WifiDirectManager"
    private val port = 8888

    private var manager: WifiP2pManager? = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: WifiP2pManager.Channel? = manager?.initialize(context, context.mainLooper, null)

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _syncStatus = MutableStateFlow("Inactivo")
    val syncStatus = _syncStatus.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Clients maintained by the Group Owner
    private val clientSockets = CopyOnWriteArrayList<Socket>()
    
    // Socket used by a Client to connect to Group Owner
    private var clientSocket: Socket? = null
    
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var discoveryJob: Job? = null

    // FIX 2: Flag to track if a connection was explicitly initiated in this session
    private var hasInitiatedConnection = false

    var thisDeviceName: String? = null
        private set

    fun updateThisDevice(device: WifiP2pDevice) {
        thisDeviceName = device.deviceName
        Log.d(tag, "Local device: ${device.deviceName} status: ${device.status}")
    }

    /**
     * FIX 1: Reset all P2P state to avoid stale connections
     */
    @SuppressLint("MissingPermission")
    fun resetConnectionState() {
        Log.d(tag, "Resetting P2P connection state...")
        hasInitiatedConnection = false
        targetDeviceName = null
        _isConnected.value = false
        _syncStatus.value = "Inactivo"
        
        // Cleanup sockets first
        disconnect()

        // Stop discovery
        manager?.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { Log.d(tag, "Discovery stopped") }
            override fun onFailure(reason: Int) { Log.e(tag, "Failed to stop discovery: $reason") }
        })
        
        // Remove existing group
        manager?.requestGroupInfo(channel) { group ->
            if (group != null) {
                manager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.d(tag, "Stale group removed")
                    }
                    override fun onFailure(reason: Int) {
                        Log.e(tag, "Failed to remove stale group: $reason")
                    }
                })
            }
        }
    }

    private var targetDeviceName: String? = null

    /**
     * Start the sync process based on the assigned role
     */
    @SuppressLint("MissingPermission")
    fun startSync(
        role: String,
        serviceName: String,
        onVisitReceived: (Visit) -> Unit,
        onInitialSyncReceived: ((String, String, ByteArray?, List<Visit>) -> Unit)? = null,
        getInitialData: (suspend () -> SyncPayload.InitialSyncPayload)? = null,
        targetP2pName: String? = null // New parameter for Receiver
    ) {
        hasInitiatedConnection = true // FIX 2: Set flag when sync starts explicitly
        if (role == "EMITTER") {
            // Emitter acts as Group Owner
            _syncStatus.value = "Iniciando Grupo P2P..."
            
            // Start TCP Server BEFORE creating group so it's ready
            startServer(onVisitReceived, getInitialData)
            
            // BUG 2 FIX: Check for stale group before creating a new one
            manager?.requestGroupInfo(channel) { group ->
                if (group != null) {
                    Log.d(tag, "Removing stale P2P group...")
                    manager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() = createP2pGroup()
                        override fun onFailure(reason: Int) = createP2pGroup()
                    })
                } else {
                    createP2pGroup()
                }
            }
        } else {
            // Receiver acts as Client, starts peer discovery
            targetDeviceName = targetP2pName
            _syncStatus.value = "Buscando dispositivos..."
            startDiscovery()
        }
    }

    @SuppressLint("MissingPermission")
    private fun createP2pGroup() {
        manager?.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(tag, "P2P Group Created Successfully")
                _syncStatus.value = "Grupo Creado. Esperando clientes..."
            }

            override fun onFailure(reason: Int) {
                Log.e(tag, "Failed to create group: $reason")
                _syncStatus.value = "Error al crear grupo: $reason"
                SyncManager.updateProgress { it.copy(step = SyncStep.ERROR, errorMessage = "Error P2P: $reason") }
            }
        })
    }

    /**
     * Called by BroadcastReceiver when peers are found
     */
    fun onPeersAvailable(peers: List<WifiP2pDevice>, onVisitReceived: (Visit) -> Unit, onInitialSyncReceived: ((String, String, ByteArray?, List<Visit>) -> Unit)?) {
        if (!hasInitiatedConnection) return // FIX 2: Guard with flag

        val target = targetDeviceName ?: return
        val device = peers.find { it.deviceName == target }
        if (device != null && !isConnected.value) {
            Log.d(tag, "Target peer found: ${device.deviceName}. Connecting...")
            connectToPeer(device, onVisitReceived, onInitialSyncReceived)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startDiscovery() {
        // BUG 1 FIX: Check permissions before discovery
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) 
                != PackageManager.PERMISSION_GRANTED) {
                Log.e(tag, "Missing NEARBY_WIFI_DEVICES permission")
                _syncStatus.value = "Faltan permisos de dispositivos cercanos"
                return
            }
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
                Log.e(tag, "Missing ACCESS_FINE_LOCATION permission")
                _syncStatus.value = "Falta permiso de ubicación"
                return
            }
        }

        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(tag, "Peer discovery started")
            }

            override fun onFailure(reason: Int) {
                Log.e(tag, "Discovery failed: $reason")
                _syncStatus.value = "Fallo al buscar dispositivos: $reason"
            }
        })
    }

    /**
     * Connect to a specific peer (called when a peer is selected/found via QR or list)
     */
    @SuppressLint("MissingPermission")
    fun connectToPeer(
        device: WifiP2pDevice,
        onVisitReceived: (Visit) -> Unit,
        onInitialSyncReceived: ((String, String, ByteArray?, List<Visit>) -> Unit)? = null
    ) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }

        _syncStatus.value = "Conectando a ${device.deviceName}..."
        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(tag, "Connection initiated to ${device.deviceName}")
            }

            override fun onFailure(reason: Int) {
                Log.e(tag, "Connection failed: $reason")
                _syncStatus.value = "Error de conexión: $reason"
            }
        })
    }

    /**
     * TCP Server logic for Group Owner (EMITTER)
     */
    private fun startServer(
        onVisitReceived: (Visit) -> Unit,
        getInitialData: (suspend () -> SyncPayload.InitialSyncPayload)? = null
    ) {
        serverJob?.cancel()
        serverJob = scope.launch {
            try {
                serverSocket?.close()
                serverSocket = ServerSocket(port).apply { reuseAddress = true }
                
                while (isActive) {
                    val client = try {
                        serverSocket?.accept()
                    } catch (e: Exception) {
                        null
                    } ?: break

                    Log.d(tag, "New client connected: ${client.inetAddress}")
                    clientSockets.add(client)
                    _isConnected.value = true
                    _syncStatus.value = "Conectado (${clientSockets.size} clientes)"

                    // Handle each client in a separate coroutine
                    launch {
                        handleSocketCommunication(client, "EMITTER", onVisitReceived, null, getInitialData)
                        clientSockets.remove(client)
                        if (clientSockets.isEmpty()) {
                            _isConnected.value = false
                            _syncStatus.value = "Esperando clientes..."
                        } else {
                            _syncStatus.value = "Conectado (${clientSockets.size} clientes)"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Server error: ${e.message}")
            }
        }
    }

    /**
     * Connect to Group Owner via TCP (RECEIVER)
     */
    fun connectToGroupOwner(
        ownerIp: String,
        onVisitReceived: (Visit) -> Unit,
        onInitialSyncReceived: ((String, String, ByteArray?, List<Visit>) -> Unit)? = null
    ) {
        scope.launch {
            try {
                _syncStatus.value = "Conectando al servidor..."
                val socket = Socket()
                socket.connect(InetSocketAddress(ownerIp, port), 10000)
                clientSocket = socket
                _isConnected.value = true
                _syncStatus.value = "Sincronizando..."

                handleSocketCommunication(socket, "RECEIVER", onVisitReceived, onInitialSyncReceived, null)
            } catch (e: Exception) {
                Log.e(tag, "Failed to connect to GO: ${e.message}")
                _syncStatus.value = "Error: No se pudo conectar"
            }
        }
    }

    private suspend fun handleSocketCommunication(
        s: Socket,
        role: String,
        onVisitReceived: (Visit) -> Unit,
        onInitialSyncReceived: ((String, String, ByteArray?, List<Visit>) -> Unit)? = null,
        getInitialData: (suspend () -> SyncPayload.InitialSyncPayload)? = null
    ) {
        try {
            val reader = BufferedReader(InputStreamReader(s.getInputStream()))
            val writer = PrintWriter(BufferedWriter(OutputStreamWriter(s.getOutputStream())), true)

            // If I am EMITTER (Server), send initial data first
            if (role == "EMITTER") {
                getInitialData?.invoke()?.let { initialData ->
                    val json = Json.encodeToString(SyncPayload.InitialSyncPayload.serializer(), initialData)
                    writer.println(json)
                    Log.d(tag, "Initial data sent to client")
                }
            }

            while (currentCoroutineContext().isActive && !s.isClosed) {
                val line = withContext(Dispatchers.IO) {
                    try { reader.readLine() } catch (e: Exception) { null }
                } ?: break

                try {
                    val payload = Json.decodeFromString<SyncPayload>(line)
                    withContext(Dispatchers.Main) {
                        when (payload) {
                            is SyncPayload.InitialSyncPayload -> {
                                if (role == "RECEIVER") {
                                    onInitialSyncReceived?.invoke(
                                        payload.businessName,
                                        payload.businessCategory,
                                        payload.profilePicture,
                                        payload.visits
                                    )
                                }
                            }
                            is SyncPayload.VisitPayload -> {
                                onVisitReceived(payload.visit)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error decoding payload: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Communication error: ${e.message}")
        } finally {
            withContext(NonCancellable) {
                s.close()
            }
        }
    }

    /**
     * Broadcast a new visit to all connected clients (EMITTER only)
     */
    fun sendVisit(visit: Visit) {
        scope.launch {
            val payload = SyncPayload.VisitPayload(visit)
            val json = Json.encodeToString(SyncPayload.VisitPayload.serializer(), payload)
            
            // If Emitter (Server), send to all connected clients
            clientSockets.forEach { socket ->
                try {
                    val writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
                    writer.println(json)
                } catch (e: Exception) {
                    Log.e(tag, "Error sending to client: ${e.message}")
                }
            }

            // If Receiver (Client), send to Group Owner
            clientSocket?.let { socket ->
                try {
                    val writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
                    writer.println(json)
                } catch (e: Exception) {
                    Log.e(tag, "Error sending to GO: ${e.message}")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        serverJob?.cancel()
        discoveryJob?.cancel()
        
        scope.launch(Dispatchers.IO) {
            clientSocket?.close()
            clientSockets.forEach { it.close() }
            clientSockets.clear()
            serverSocket?.close()
            
            manager?.removeGroup(channel, null)
            manager?.stopPeerDiscovery(channel, null)
        }
        
        _isConnected.value = false
        _syncStatus.value = "Inactivo"
    }
}
