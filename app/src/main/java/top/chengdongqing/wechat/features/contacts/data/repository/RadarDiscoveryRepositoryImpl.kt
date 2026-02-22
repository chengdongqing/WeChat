package top.chengdongqing.wechat.features.contacts.data.repository

import android.content.Context
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import top.chengdongqing.wechat.data.network.model.RadarBeacon
import top.chengdongqing.wechat.data.network.radar.RadarDiscoveryService
import top.chengdongqing.wechat.features.contacts.domain.repository.RadarDiscoveryRepository

@Singleton
class RadarDiscoveryRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val service: RadarDiscoveryService
) : RadarDiscoveryRepository {

    override val nearbyUsers: StateFlow<List<RadarBeacon>> =
        service.discoveredBeacons
            .map { it.values.toList() }
            .stateIn(
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                started = SharingStarted.WhileSubscribed(),
                initialValue = emptyList()
            )

    override fun startDiscovery() {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        service.start(wifiManager)
    }

    override fun stopDiscovery() = service.stop()
}