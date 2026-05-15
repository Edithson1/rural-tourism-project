package upch.mluque.final_project.data.sync

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log

class WifiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val wifiDirectManager: WifiDirectManager,
    private val onConnectionChanged: (Boolean, String?) -> Unit,
    private val onPeersChanged: (List<android.net.wifi.p2p.WifiP2pDevice>) -> Unit
) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Log.d("P2PReceiver", "Wi-Fi P2P is enabled")
                } else {
                    Log.d("P2PReceiver", "Wi-Fi P2P is disabled")
                }
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                manager.requestPeers(channel) { peers ->
                    onPeersChanged(peers.deviceList.toList())
                }
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val networkInfo: NetworkInfo? = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                if (networkInfo?.isConnected == true) {
                    manager.requestConnectionInfo(channel) { info ->
                        if (info.groupFormed) {
                            onConnectionChanged(true, info.groupOwnerAddress?.hostAddress)
                        }
                    }
                } else {
                    onConnectionChanged(false, null)
                }
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                val device: android.net.wifi.p2p.WifiP2pDevice? = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                device?.let { wifiDirectManager.updateThisDevice(it) }
            }
        }
    }
}
