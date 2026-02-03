package top.chengdongqing.wechat2.core.protocol

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import top.chengdongqing.wechat2.data.crypto.CryptoManager
import top.chengdongqing.wechat2.data.local_1.MessageDao
import top.chengdongqing.wechat2.data.local_1.MessageEntity
import top.chengdongqing.wechat2.data.model_1.ChatPayload
import top.chengdongqing.wechat2.data.model_1.MessageEnvelope
import top.chengdongqing.wechat2.data.model_1.isSignal

/**
 * 消息调度中心：如果把 WifiLanManager 比作码头（负责搬运数据包），那 MessageDispatcher 就是海关和物流调度中心。
 *
 * 核心功能包括：
 * 1.协议分拣（Routing）：识别消息是聊天文字、图片，还是 WebRTC 的信令（SDP/ICE）。
 * 2.安全脱壳（Decryption）：如果收到的是 EncryptedText，它负责在进入 UI 层前解密。UI 层拿到的永远是“干净”的明文。
 * 3.持久化（Persistence）：负责将需要保存的消息（文字、媒体路径）写入数据库，而不需要保存的消息（如 ICE 候选者）则直接跳过。
 * 4.业务触发（Action Handling）：比如收到 START_VIDEO 时，它负责调用 context.startActivity 弹出通话界面。
 * 5.状态同步（State Management）：它通过 SharedFlow 让多个 Activity 同时观察到消息的变化。
 */
interface MessageDispatcher {
    // 供聊天界面订阅
    val chatFlow: SharedFlow<MessageEnvelope>

    // 供通话界面订阅
    val signalingFlow: SharedFlow<MessageEnvelope>

    /**
     * 分发消息
     */
    fun dispatch(envelope: MessageEnvelope)
}

class MessageDispatcherImpl(private val messageDao: MessageDao) : MessageDispatcher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val chatFlow = MutableSharedFlow<MessageEnvelope>(replay = 1)

    override val signalingFlow = MutableSharedFlow<MessageEnvelope>(extraBufferCapacity = 64)

    override fun dispatch(envelope: MessageEnvelope) {
        scope.launch {
            when (val payload = envelope.payload) {
                is ChatPayload.EncryptedText -> {
                    // 解密
                    val plainText = CryptoManager.decrypt(payload.ciphertext, payload.iv)
                    // 转换成普通文本 Payload
                    val decryptedPayload = ChatPayload.Text(plainText)
                    val newEnvelope = envelope.copy(payload = decryptedPayload)

                    saveAndEmit(newEnvelope)
                }

                is ChatPayload.Text,
                is ChatPayload.Media -> {
                    saveAndEmit(envelope)
                }

                else -> if (payload.isSignal) {
                    signalingFlow.emit(envelope)
                }
            }
        }
    }

    private suspend fun saveAndEmit(envelope: MessageEnvelope) {
        saveToDatabase(envelope)
        chatFlow.emit(envelope)
    }

    private suspend fun saveToDatabase(envelope: MessageEnvelope) {
        val entity = MessageEntity.fromEnvelope(envelope)
        messageDao.insertMessage(entity)
    }
}