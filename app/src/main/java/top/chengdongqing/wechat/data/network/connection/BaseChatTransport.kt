package top.chengdongqing.wechat.data.network.connection

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import top.chengdongqing.wechat.data.network.model.Packet

/**
 * ChatTransport 基类
 */
abstract class BaseChatTransport(
    protected open val connectionManager: ConnectionManager,
) : ChatTransport {

    override val connectionEvents
        get() = connectionManager.connectionEvents

    private val _connectionRequired =
        MutableSharedFlow<ConnectionRequiredEvent>(extraBufferCapacity = 8)
    override val connectionRequired = _connectionRequired.asSharedFlow()

    /**
     * 连接守卫：已连接则直接执行 [action]，否则尝试自动重连。
     * 自动重连由子类实现 [tryAutoReconnect]；重连失败后由子类决定
     * 是发出 [ConnectionRequiredEvent] 还是直接返回 failure。
     */
    protected suspend fun <T> ensureConnected(
        userId: String,
        packet: Packet? = null,
        action: suspend () -> Result<T>,
    ): Result<T> {
        if (connectionManager.isConnected(userId)) return action()

        val reconnected = tryAutoReconnect(userId)
        if (reconnected) return action()

        return onConnectionUnavailable(userId, packet)
    }

    /**
     * 尝试利用历史信息静默重连，成功返回 true。
     * 无历史信息或不支持自动重连的子类直接返回 false。
     */
    protected open suspend fun tryAutoReconnect(userId: String): Boolean = false

    /**
     * 自动重连失败后的处理：
     * - 需要用户介入（蓝牙/WiFi Direct）：发出 [ConnectionRequiredEvent] 并返回 failure
     * - 无法自动处理（LAN）：直接返回 failure
     */
    protected abstract suspend fun <T> onConnectionUnavailable(
        userId: String,
        packet: Packet?,
    ): Result<T>

    /** 向 UI 层发出连接请求事件，引导用户手动选择设备 */
    protected suspend fun requireConnectionFromUi(event: ConnectionRequiredEvent): Result<Nothing> {
        _connectionRequired.emit(event)
        return Result.failure(Exception(event.reason))
    }

    override fun isConnected(userId: String) = connectionManager.isConnected(userId)

    override suspend fun disconnect(userId: String) = connectionManager.disconnect(userId)

    override suspend fun disconnectAll() = connectionManager.closeAll()
}

val ConnectionRequiredEvent.reason: String
    get() = when (this) {
        is ConnectionRequiredEvent.Bluetooth -> "需要选择蓝牙设备"
        is ConnectionRequiredEvent.WiFiDirect -> "需要选择 WiFi Direct 设备"
    }