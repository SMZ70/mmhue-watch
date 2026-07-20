package com.smz70.mmhue.watch

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

/**
 * Keeps Wi-Fi alive while the app is on screen, and pins the app's traffic to it.
 *
 * The problem this solves: a Pixel Watch treats its Bluetooth link to the phone
 * as the internet connection and drops Wi-Fi to save battery. But the mmhue panel
 * lives on the LAN at mm.fritz.box, which the Bluetooth proxy cannot route to --
 * only Wi-Fi can. So the watch happily reports "connected", every request to the
 * panel fails, and the radio the app actually needs stays off.
 *
 * requestNetwork() with a TRANSPORT_WIFI request is the documented way to tell
 * the OS "bring Wi-Fi up, I need it". When it arrives we bind the whole process
 * to that network so OkHttp's sockets and DNS both go over Wi-Fi and reach the
 * router that knows mm.fritz.box. Released on pause so we are not holding the
 * radio up in the user's pocket.
 */
class WifiNetworkManager(context: Context) {

    private val cm = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var callback: ConnectivityManager.NetworkCallback? = null

    fun acquire() {
        if (callback != null) return

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Pin sockets and DNS to Wi-Fi. Without this the process could
                // still resolve/route over the Bluetooth proxy and miss the LAN.
                val bound = cm.bindProcessToNetwork(network)
                Log.i(TAG, "Wi-Fi available, bound process: $bound")
            }

            override fun onLost(network: Network) {
                cm.bindProcessToNetwork(null)
                Log.i(TAG, "Wi-Fi lost, process unbound")
            }

            override fun onUnavailable() {
                Log.w(TAG, "Wi-Fi request unavailable")
            }
        }

        callback = cb
        // No timeout: the request stays active, holding Wi-Fi up, until release().
        cm.requestNetwork(request, cb)
    }

    fun release() {
        callback?.let { cb ->
            cm.bindProcessToNetwork(null)
            runCatching { cm.unregisterNetworkCallback(cb) }
        }
        callback = null
    }

    private companion object {
        const val TAG = "MmhueWifi"
    }
}
