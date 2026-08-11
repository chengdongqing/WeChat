package top.chengdongqing.wechat.feature.contacts.data.network

import android.content.Context
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.runtime.IoScope
import top.chengdongqing.wechat.core.data.model.RadarBeacon
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.network.http.AvatarServer
import top.chengdongqing.wechat.feature.contacts.data.network.RadarDiscoveryService.Companion.BEACON_TIMEOUT_MS
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.SocketTimeoutException
import javax.inject.Inject

/**
 * 雷达发现服务
 *
 * 基于UDP组播
 */
class RadarDiscoveryService @Inject constructor(
    private val json: Json,
    private val profileRepository: ProfileRepository,
    private val avatarServer: AvatarServer,
    @param:ApplicationContext private val context: Context,
    @param:IoScope private val scope: CoroutineScope
) {
    companion object {
        private const val MULTICAST_GROUP = "239.255.43.21"
        private const val MULTICAST_PORT = 52100
        private const val BEACON_INTERVAL_MS = 1000L // 每秒广播一次
        private const val BEACON_TIMEOUT_MS = BEACON_INTERVAL_MS * 3 // 3秒无心跳则移除
        private const val BUFFER_SIZE = 1024
    }

    private var multicastSocket: MulticastSocket? = null
    private var group: InetAddress? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    private val _discoveredBeacons = MutableStateFlow<Map<String, RadarBeacon>>(emptyMap())
    val discoveredBeacons = _discoveredBeacons.asStateFlow()

    private val wifiManager by lazy { context.getSystemService(Context.WIFI_SERVICE) as WifiManager }

    /**
     * 启动雷达发现服务。
     * 依次初始化头像服务、多播 Socket，并并发启动接收、广播、超时清理三个协程。
     */
    fun start() {
        stop()

        scope.launch {
            runCatching {
                acquireMulticastLock(wifiManager)
                setupSocket()

                launch(Dispatchers.IO) { receiveLoop() }
                launch(Dispatchers.IO) { beaconLoop() }
                launch { timeoutCleanerLoop() }
            }
        }
    }

    /**
     * 停止雷达发现服务，释放所有网络资源并清空已发现的用户列表。
     */
    fun stop() {
        multicastLock?.let {
            if (it.isHeld) it.release()
        }
        multicastLock = null

        multicastSocket?.leaveGroup(group)
        multicastSocket?.close()
        multicastSocket = null
        multicastLock?.release()
        multicastLock = null

        _discoveredBeacons.value = emptyMap()
    }

    /**
     * 初始化多播 Socket 并加入多播组。
     */
    private fun setupSocket() {
        group = InetAddress.getByName(MULTICAST_GROUP)
        multicastSocket = MulticastSocket(MULTICAST_PORT).apply {
            joinGroup(group)
            timeToLive = 4
            soTimeout = 3000
        }
    }

    /**
     * 持续监听多播包，将收到的 Beacon 更新到已发现用户列表。
     * 使用本地时间覆盖 Beacon 中的 timestamp，避免设备间时钟不同步导致超时误判。
     */
    private fun receiveLoop() {
        val buffer = ByteArray(BUFFER_SIZE)
        while (multicastSocket?.isClosed == false) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                multicastSocket?.receive(packet)

                val payload = String(packet.data, 0, packet.length, Charsets.UTF_8)
                val beacon = json.decodeFromString<RadarBeacon>(payload)
                val myUserId = profileRepository.requireUserId()

                // 忽略自己广播的包
                if (beacon.userId == myUserId) continue

                _discoveredBeacons.update {
                    it + (beacon.userId to beacon.copy(
                        timestamp = System.currentTimeMillis()
                    ))
                }
            } catch (_: SocketTimeoutException) {
                // soTimeout 到期属于正常现象，继续下一轮接收
            } catch (_: Exception) {
                if (multicastSocket?.isClosed == true) break
            }
        }
    }

    /**
     * 定期向多播组广播自己的 Beacon，让局域网内其他设备感知到本机的存在。
     */
    private suspend fun beaconLoop() = withContext(Dispatchers.IO) {
        val avatarUrl = avatarServer.avatarUrl ?: return@withContext
        val myProfile = profileRepository.requireProfile()

        val payload = json.encodeToString(
            RadarBeacon(
                userId = myProfile.id,
                nickname = myProfile.nickname,
                avatarUrl = avatarUrl
            )
        ).toByteArray(Charsets.UTF_8)

        while (multicastSocket?.isClosed == false) {
            try {
                multicastSocket?.send(DatagramPacket(payload, payload.size, group, MULTICAST_PORT))
            } catch (_: Exception) {
                if (multicastSocket?.isClosed == true) break
            }
            delay(BEACON_INTERVAL_MS)
        }
    }

    /**
     * 定期扫描已发现用户列表，移除超过 [BEACON_TIMEOUT_MS] 未收到心跳的用户。
     */
    private suspend fun timeoutCleanerLoop() {
        while (true) {
            delay(BEACON_INTERVAL_MS)
            val now = System.currentTimeMillis()
            _discoveredBeacons.update { current ->
                current.filter { (_, beacon) -> now - beacon.timestamp < BEACON_TIMEOUT_MS }
            }
        }
    }

    /**
     * 申请 Wi-Fi 多播锁。
     * Android 默认会过滤多播数据包，持有此锁后才能正常收到多播消息。
     */
    private fun acquireMulticastLock(wifiManager: WifiManager) {
        multicastLock = wifiManager.createMulticastLock("radar_discovery").apply {
            setReferenceCounted(true)
            acquire()
        }
    }
}