package top.chengdongqing.wechat.features.contacts.data.repository

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import top.chengdongqing.wechat.data.network.model.RadarBeacon
import top.chengdongqing.wechat.features.contacts.data.network.RadarDiscoveryService
import top.chengdongqing.wechat.features.contacts.domain.repository.RadarDiscoveryRepository

@Singleton
class RadarDiscoveryRepositoryImpl @Inject constructor(
    private val service: RadarDiscoveryService
) : RadarDiscoveryRepository {

    override val nearbyUsers: Flow<List<RadarBeacon>> =
        service.discoveredBeacons.map { it.values.toList() }

    override fun startDiscovery() = service.start()

    override fun stopDiscovery() = service.stop()
}