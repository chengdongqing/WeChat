package top.chengdongqing.wechat.data.network.connection.bluetooth

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.network.connection.ChatTransport
import top.chengdongqing.wechat.data.network.connection.ConnectionEvent
import top.chengdongqing.wechat.data.network.connection.ConnectionRequiredEvent
import top.chengdongqing.wechat.data.network.crypto.EncryptingPacketWriter
import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository

/**
 * 蓝牙聊天传输层
 * 对外实现 ChatTransport，内部委托给 BtConnectionManager
 */
@Singleton
class BluetoothChatTransport @Inject constructor(
    private val connectionManager: BtConnectionManager,
    private val rfcommClient: RfcommClient,
    private val connectionInfoDao: ConnectionInfoDao,
    private val profileRepository: ProfileRepository,
) : ChatTransport {

    private val myUserId get() = profileRepository.getProfile()?.id ?: ""

    private val _connectionRequired =
        MutableSharedFlow<ConnectionRequiredEvent>(extraBufferCapacity = 8)
    override val connectionRequired: SharedFlow<ConnectionRequiredEvent> =
        _connectionRequired.asSharedFlow()

    override val connectionEvents: Flow<ConnectionEvent> =
        connectionManager.connectionEvents

    override suspend fun send(userId: String, packet: Packet): Result<Unit> {
        // 已连接直接发
        if (connectionManager.isConnected(userId)) {
            return connectionManager.send(userId, packet)
        }

        // 有 MAC 地址，直接建立连接
        val info = connectionInfoDao.getById(userId)
        info?.bluetoothAddress?.let {
            rfcommClient.connect(
                userId = userId,
                macAddress = it,
                myUserId
            ).getOrElse { e ->
                return Result.failure(e)
            }
            return connectionManager.send(userId, packet)
        }

        // 没有 MAC 地址，通知 UI 弹出设备选择弹窗
        // 消息会有 UI 层触发 retrySend 重新发送
        _connectionRequired.emit(ConnectionRequiredEvent.Bluetooth(userId, packet))
        return Result.failure(Exception("需要选择蓝牙设备"))
    }

    override suspend fun sendAtomicTransfer(
        userId: String,
        block: suspend (EncryptingPacketWriter) -> Unit
    ) = connectionManager.sendAtomicTransfer(userId, block)

    override fun isConnected(userId: String) =
        connectionManager.isConnected(userId)

    override suspend fun disconnect(userId: String) =
        connectionManager.disconnect(userId)

    override suspend fun disconnectAll() =
        connectionManager.closeAll()
}