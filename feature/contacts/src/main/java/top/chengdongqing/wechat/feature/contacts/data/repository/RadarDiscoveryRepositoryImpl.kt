package top.chengdongqing.wechat.feature.contacts.data.repository

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import top.chengdongqing.wechat.core.data.model.RadarBeacon
import top.chengdongqing.wechat.core.data.repository.RadarDiscoveryRepository
import top.chengdongqing.wechat.feature.contacts.data.network.RadarDiscoveryService

@Singleton
class RadarDiscoveryRepositoryImpl @Inject constructor(
    private val service: RadarDiscoveryService
) : RadarDiscoveryRepository {

    override val nearbyUsers: Flow<List<RadarBeacon>> =
        service.discoveredBeacons.map { it.values.toList() }

    override fun startDiscovery() = service.start()

    override fun stopDiscovery() = service.stop()
}