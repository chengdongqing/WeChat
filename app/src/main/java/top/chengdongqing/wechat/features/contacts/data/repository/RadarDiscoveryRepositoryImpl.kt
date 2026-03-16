package top.chengdongqing.wechat.features.contacts.data.repository

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import top.chengdongqing.wechat.data.network.model.RadarBeacon
import top.chengdongqing.wechat.features.contacts.data.network.RadarDiscoveryService
import top.chengdongqing.wechat.features.contacts.domain.repository.RadarDiscoveryRepository

@Singleton
class RadarDiscoveryRepositoryImpl @Inject constructor(
    private val service: RadarDiscoveryService
) : RadarDiscoveryRepository {

    override val nearbyUsers: Flow<List<RadarBeacon>> =
        service.discoveredBeacons
            .map { it.values.toList() }
            .stateIn(
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                started = SharingStarted.WhileSubscribed(),
                initialValue = emptyList()
            )

    override fun startDiscovery() = service.start()

    override fun stopDiscovery() = service.stop()
}