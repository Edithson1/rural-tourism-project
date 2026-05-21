package upch.mluque.final_project.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NetworkMonitor(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isWifiConnected = MutableStateFlow(false)
    val isWifiConnected: StateFlow<Boolean> = _isWifiConnected

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            val isLocal = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                          capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true ||
                          capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            if (isLocal) {
                _isWifiConnected.value = true
            }
        }

        override fun onLost(network: Network) {
            // Solo marcamos como desconectado si ya no hay redes locales disponibles
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            val stillConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                                 capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
            if (!stillConnected) {
                _isWifiConnected.value = false
            }
        }
    }

    fun start() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        
        // Check initial state
        checkCurrentState()
    }

    fun checkCurrentState() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isLocal = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                      capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
        
        // También consideramos el estado si no hay "ActiveNetwork" pero hay una IP local (caso Hotspot servidor)
        _isWifiConnected.value = isLocal
    }

    fun stop() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
