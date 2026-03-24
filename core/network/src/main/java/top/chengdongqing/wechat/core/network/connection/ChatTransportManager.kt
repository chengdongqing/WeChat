package top.chengdongqing.wechat.core.network.connection

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import top.chengdongqing.wechat.core.common.di.IoScope
import top.chengdongqing.wechat.core.data.model.ConnectionMode
import top.chengdongqing.wechat.core.data.repository.ChatSettingsRepository
import top.chengdongqing.wechat.core.network.connection.bluetooth.BluetoothChatTransport
import top.chengdongqing.wechat.core.network.connection.wifi.WiFiDirectChatTransport
import top.chengdongqing.wechat.core.network.connection.wifi.WiFiLanChatTransport
import top.chengdongqing.wechat.core.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.core.network.model.Packet

/**
 * 传输层路由器，根据当前 [ConnectionMode] 将收发请求分发给对应的 [ChatTransport]。
 *
 * - send / sendAtomicTransfer / isConnected / disconnect → 路由到 [active]
 * - disconnectAll → 广播给所有传输层
 * - connectionEvents → 三路合并，订阅方无需关心来源
 * - connectionRequired → 汇总蓝牙和 WiFi Direct 的事件（LAN 不发此事件）
 */
@Singleton
class ChatTransportManager @Inject constructor(
    private val wifiLan: WiFiLanChatTransport,
    private val wifiDirect: WiFiDirectChatTransport,
    private val bluetooth: BluetoothChatTransport,
    chatSettingsRepository: ChatSettingsRepository,
    @param:IoScope private val scope: CoroutineScope,
) : ChatTransport {

    private val _mode = MutableStateFlow(ConnectionMode.WiFiLan)
    val mode = _mode.asStateFlow()

    fun setMode(mode: ConnectionMode) {
        _mode.value = mode
    }

    private val active: ChatTransport
        get() = when (_mode.value) {
            ConnectionMode.WiFiLan -> wifiLan
            ConnectionMode.WiFiDirect -> wifiDirect
            ConnectionMode.Bluetooth -> bluetooth
        }

    override val connectionEvents = merge(
        wifiLan.connectionEvents,
        wifiDirect.connectionEvents,
        bluetooth.connectionEvents,
    )

    private val _connectionRequired =
        MutableSharedFlow<ConnectionRequiredEvent>(extraBufferCapacity = 8)
    override val connectionRequired = _connectionRequired.asSharedFlow()

    init {
        // 汇总需要用户介入的传输层事件
        merge(bluetooth.connectionRequired, wifiDirect.connectionRequired)
            .onEach { _connectionRequired.emit(it) }
            .launchIn(scope)

        // E2E 开关变化时断开所有连接
        chatSettingsRepository.e2eEnabled
            .onEach { disconnectAll() }
            .launchIn(scope)
    }

    override suspend fun send(userId: String, packet: Packet) = active.send(userId, packet)

    override suspend fun sendAtomicTransfer(
        userId: String,
        block: suspend (EncryptingPacketWriter) -> Unit
    ) = active.sendAtomicTransfer(userId, block)

    override fun isConnected(userId: String) = active.isConnected(userId)

    override suspend fun disconnect(userId: String) = active.disconnect(userId)

    /**
     * 切换模式或销毁时调用，确保三路传输层全部释放
     */
    override suspend fun disconnectAll() {
        wifiLan.disconnectAll()
        wifiDirect.disconnectAll()
        bluetooth.disconnectAll()
    }
}