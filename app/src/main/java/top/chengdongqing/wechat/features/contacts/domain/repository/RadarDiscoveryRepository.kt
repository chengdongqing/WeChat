package top.chengdongqing.wechat.features.contacts.domain.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.data.network.model.RadarBeacon

interface RadarDiscoveryRepository {

    /**
     * 当前发现的附近用户列表
     */
    val nearbyUsers: Flow<List<RadarBeacon>>

    /**
     * 开启雷达发现服务
     */
    fun startDiscovery()

    /**
     * 停止雷达发现服务
     */
    fun stopDiscovery()
}