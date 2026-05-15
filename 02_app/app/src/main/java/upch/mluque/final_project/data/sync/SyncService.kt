package upch.mluque.final_project.data.sync

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import upch.mluque.final_project.MainActivity
import upch.mluque.final_project.data.local.AppDatabase
import upch.mluque.final_project.data.local.AppSettings
import upch.mluque.final_project.data.local.Visit
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.io.PrintWriter
import kotlinx.coroutines.flow.first

import android.net.wifi.p2p.WifiP2pManager
import android.content.IntentFilter

class SyncService : Service() {
    private val tag = "SyncService"
    private val channelId = "sync_channel"
    private val notificationId = 101
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var syncManager: WifiDirectManager
    private lateinit var database: AppDatabase
    private var receiver: WifiDirectBroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        syncManager = WifiDirectManager(this)
        database = AppDatabase.getDatabase(this)
        createNotificationChannel()

        val manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        val channel = manager.initialize(this, mainLooper, null)
        receiver = WifiDirectBroadcastReceiver(
            manager, channel, syncManager,
            onConnectionChanged = { connected, ownerIp ->
                if (connected && ownerIp != null) {
                    syncManager.connectToGroupOwner(
                        ownerIp,
                        onVisitReceived = { visit ->
                            serviceScope.launch { 
                                database.visitDao().insertVisit(visit)
                                SyncManager.emitVisit(visit)
                            }
                        },
                        onInitialSyncReceived = { name, category, profilePic, visits ->
                            handleInitialSync(name, category, profilePic, visits)
                        }
                    )
                }
            },
            onPeersChanged = { peers ->
                syncManager.onPeersAvailable(
                    peers,
                    onVisitReceived = { visit ->
                        serviceScope.launch { 
                            database.visitDao().insertVisit(visit)
                            SyncManager.emitVisit(visit)
                        }
                    },
                    onInitialSyncReceived = { name, category, profilePic, visits ->
                        handleInitialSync(name, category, profilePic, visits)
                    }
                )
            }
        )
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        registerReceiver(receiver, filter)
    }

    private var intent: Intent? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        this.intent = intent
        val role = intent?.getStringExtra("role") ?: "NONE"
        val serviceName = intent?.getStringExtra("serviceName") ?: ""
        val targetDeviceName = intent?.getStringExtra("targetDeviceName")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, createNotification("Iniciando sincronización..."), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(notificationId, createNotification("Iniciando sincronización..."))
        }

        serviceScope.launch {
            try {
                if (role == "EMITTER") {
                    SyncManager.updateProgress { it.copy(step = SyncStep.CONNECTING, role = "EMITTER") }
                    startEmitter(serviceName)
                } else if (role == "RECEIVER") {
                    SyncManager.updateProgress { it.copy(step = SyncStep.CONNECTING, role = "RECEIVER") }
                    startReceiver(serviceName, targetDeviceName)
                }
            } catch (e: Exception) {
                Log.e(tag, "Service error: ${e.message}")
                SyncManager.updateProgress { it.copy(step = SyncStep.ERROR, errorMessage = e.message) }
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun handleInitialSync(name: String, category: String, profilePic: ByteArray?, visits: List<Visit>) {
        serviceScope.launch {
            SyncManager.updateProgress { it.copy(step = SyncStep.FETCHING_PROFILE) }
            val current = database.appSettingsDao().getSettingsOnce() ?: AppSettings()
            database.appSettingsDao().saveSettings(current.copy(
                businessName = name,
                businessCategory = category,
                profilePicture = profilePic
            ))

            SyncManager.updateProgress { it.copy(step = SyncStep.RECEIVING_VISITS, totalItems = visits.size) }
            
            var count = 0
            visits.forEach { 
                database.visitDao().insertVisit(it)
                count++
                val progress = count.toFloat() / visits.size
                SyncManager.updateProgress { p -> p.copy(percentage = progress, processedItems = count) }
                updateNotification("Sincronizando: $count / ${visits.size}")
            }

            SyncManager.updateProgress { it.copy(step = SyncStep.COMPLETED) }
            showCompletionNotification(count)
            stopSelf()
        }
    }

    private fun startEmitter(serviceName: String) {
        syncManager.startSync(
            role = "EMITTER",
            serviceName = serviceName,
            onVisitReceived = { visit ->
                serviceScope.launch { 
                    database.visitDao().insertVisit(visit)
                    SyncManager.emitVisit(visit)
                }
            },
            getInitialData = {
                SyncManager.updateProgress { it.copy(step = SyncStep.RECEIVING_VISITS, errorMessage = "Enviando datos...") }
                updateNotification("Dispositivo conectado. Compartiendo datos...")
                
                val settings = database.appSettingsDao().getSettingsOnce() ?: AppSettings()
                val visits = database.visitDao().getAllOnce()
                SyncPayload.InitialSyncPayload(
                    businessName = settings.businessName,
                    businessCategory = settings.businessCategory,
                    profilePicture = settings.profilePicture,
                    visits = visits
                )
            }
        )
    }

    private fun startReceiver(serviceName: String, targetDeviceName: String? = null) {
        syncManager.startSync(
            role = "RECEIVER",
            serviceName = serviceName,
            onVisitReceived = { visit ->
                serviceScope.launch {
                    database.visitDao().insertVisit(visit)
                    SyncManager.emitVisit(visit)
                }
            },
            onInitialSyncReceived = { name, category, profilePic, visits ->
                handleInitialSync(name, category, profilePic, visits)
            },
            targetP2pName = targetDeviceName
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sincronización de Datos",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Yupay Turismo Sync")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }

    private fun updateNotification(content: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, createNotification(content))
    }

    private fun showCompletionNotification(count: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sincronización Exitosa")
            .setContentText("Se han recibido $count registros correctamente.")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        manager.notify(102, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        receiver?.let { unregisterReceiver(it) }
        serviceScope.cancel()
        syncManager.disconnect()
    }
}
