package top.chengdongqing.wechat.core.data.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.core.data.model.RadarBeacon

interface RadarDiscoveryRepository {
    val nearbyUsers: Flow<List<RadarBeacon>>
    fun startDiscovery()
    fun stopDiscovery()
}
