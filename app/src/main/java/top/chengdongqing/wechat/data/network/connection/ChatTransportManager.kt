package top.chengdongqing.wechat.data.network.connection

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.network.connection.bluetooth.BluetoothChatTransport
import top.chengdongqing.wechat.data.network.connection.wifi.WiFiDirectChatTransport
import top.chengdongqing.wechat.data.network.connection.wifi.WiFiLanChatTransport
import top.chengdongqing.wechat.data.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.data.network.protocol.Packet

/**
 * 根据当前 ConnectionMode 路由到对应的 ChatTransport
 */
@Singleton
class ChatTransportManager @Inject constructor(
    private val wifiLan: WiFiLanChatTransport,
    private val wifiDirect: WiFiDirectChatTransport,
    private val bluetooth: BluetoothChatTransport,
    @param:IoScope private val scope: CoroutineScope
) : ChatTransport {

    private val _mode = MutableStateFlow(ConnectionMode.WiFiLan)
    val mode = _mode.asStateFlow()

    /**
     * 订阅连接模式变化
     */
    fun observeMode(connectionMode: Flow<ConnectionMode>) {
        connectionMode
            .onEach { _mode.value = it }
            .launchIn(scope)
    }

    private val active: ChatTransport
        get() = when (_mode.value) {
            ConnectionMode.WiFiLan -> wifiLan
            ConnectionMode.WiFiDirect -> wifiDirect
            ConnectionMode.Bluetooth -> bluetooth
        }

    override val connectionEvents: Flow<ConnectionEvent> = merge(
        wifiLan.connectionEvents,
        wifiDirect.connectionEvents,
        bluetooth.connectionEvents
    )

    /** 汇总所有模式的连接请求事件，UI 层订阅这一个即可 */
    private val _connectionRequired =
        MutableSharedFlow<ConnectionRequiredEvent>(extraBufferCapacity = 8)
    override val connectionRequired: SharedFlow<ConnectionRequiredEvent> =
        _connectionRequired.asSharedFlow()

    init {
        // 汇总蓝牙和 WiFi Direct 的 connectionRequired 事件
        merge(
            bluetooth.connectionRequired,
            wifiDirect.connectionRequired
        ).onEach {
            _connectionRequired.emit(it)
        }.launchIn(scope)
    }

    override suspend fun send(userId: String, packet: Packet) =
        active.send(userId, packet)

    override suspend fun sendAtomicTransfer(
        userId: String,
        block: suspend (EncryptingPacketWriter) -> Unit
    ) = active.sendAtomicTransfer(userId, block)

    override fun isConnected(userId: String) = active.isConnected(userId)

    override suspend fun disconnect(userId: String) = active.disconnect(userId)

    override suspend fun disconnectAll() = active.disconnectAll()
}