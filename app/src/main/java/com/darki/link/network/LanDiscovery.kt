package com.darki.link.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import com.darki.link.DeviceIdentity
import com.darki.link.PairingStore
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Discovery optimized for DARKI Link's primary topology:
 * iQOO hotspot -> Lenovo Tab 6 Wi-Fi client.
 *
 * NSD/mDNS lets the app find the peer without asking the user to type an IP.
 * Authentication still happens after discovery; discovery never establishes trust.
 */
class LanDiscovery(context: Context) {
    private val appContext = context.applicationContext
    private val nsd = appContext.getSystemService(NsdManager::class.java)
    private val active = AtomicBoolean(false)
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    companion object {
        const val SERVICE_TYPE = "_darkilink._tcp."
        const val SERVICE_NAME_PREFIX = "DARKI-Link-"
    }

    fun advertise(port: Int = PairingStore.DEFAULT_PORT, onEvent: (String) -> Unit = {}) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "$SERVICE_NAME_PREFIX${DeviceIdentity.get(appContext).take(8)}"
            serviceType = SERVICE_TYPE
            this.port = port
            setAttribute("device", Build.MODEL.take(60))
            setAttribute("protocol", "1")
        }

        stopAdvertise()
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) = onEvent("Advertising ${info.serviceName}")
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) = onEvent("Advertise failed: $errorCode")
            override fun onServiceUnregistered(info: NsdServiceInfo) = onEvent("Advertisement stopped")
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) = onEvent("Advertise stop failed: $errorCode")
        }
        nsd.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun discover(
        onPeer: (host: String, port: Int, serviceName: String) -> Unit,
        onEvent: (String) -> Unit = {}
    ) {
        stopDiscover()
        active.set(true)
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = onEvent("Searching for DARKI Link peers…")
            override fun onServiceFound(info: NsdServiceInfo) {
                if (!info.serviceType.equals(SERVICE_TYPE, ignoreCase = true)) return
                nsd.resolveService(info, object : NsdManager.ResolveListener {
                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        val host = resolved.host?.hostAddress ?: return
                        onPeer(host, resolved.port, resolved.serviceName)
                    }
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) =
                        onEvent("Peer resolve failed: $errorCode")
                })
            }
            override fun onServiceLost(info: NsdServiceInfo) = onEvent("Peer lost: ${info.serviceName}")
            override fun onDiscoveryStopped(serviceType: String) = onEvent("Peer discovery stopped")
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                active.set(false)
                onEvent("Discovery failed: $errorCode")
                nsd.stopServiceDiscovery(this)
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = onEvent("Discovery stop failed: $errorCode")
        }
        nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopAdvertise() {
        registrationListener?.let { runCatching { nsd.unregisterService(it) } }
        registrationListener = null
    }

    fun stopDiscover() {
        discoveryListener?.let { runCatching { nsd.stopServiceDiscovery(it) } }
        discoveryListener = null
        active.set(false)
    }

    fun close() {
        stopDiscover()
        stopAdvertise()
    }
}
