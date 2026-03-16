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
     * * 调用此方法后将执行以下操作：
     * 1. 启动本地头像 HTTP 服务器并生成个人名片 URL。
     * 2. 加入多播组，开始监听局域网内其他设备发出的广播。
     * 3. 启动定时任务，周期性地向外发送自身的 [RadarBeacon] 信息。
     * 4. 开启超时检测逻辑，自动移除长时间未收到心跳的用户。
     */
    fun startDiscovery()

    /**
     * 停止雷达发现服务
     * * 用于释放系统资源，包括：
     * 1. 关闭 MulticastSocket 并退出多播组。
     * 2. 停止本地头像服务器。
     * 3. 释放 Wi-Fi 多播锁（MulticastLock）。
     * 4. 清空当前的 [nearbyUsers] 列表。
     */
    fun stopDiscovery()
}