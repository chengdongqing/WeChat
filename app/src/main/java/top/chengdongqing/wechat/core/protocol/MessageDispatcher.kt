package top.chengdongqing.wechat.core.protocol

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.data.crypto.CryptoManager
import top.chengdongqing.wechat.data.local.MessageDao
import top.chengdongqing.wechat.data.local.MessageEntity
import top.chengdongqing.wechat.data.model.ChatPayload
import top.chengdongqing.wechat.data.model.MessageEnvelope
import top.chengdongqing.wechat.ui.call.CallActivity

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
class MessageDispatcher(
    private val context: Context,
    private val messageDao: MessageDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 供聊天界面订阅
    val chatFlow = MutableSharedFlow<MessageEnvelope>(replay = 1)

    // 供通话界面订阅
    val signalingFlow = MutableSharedFlow<ChatPayload>(extraBufferCapacity = 64)

    fun dispatch(envelope: MessageEnvelope) {
        scope.launch {
            when (val payload = envelope.payload) {
                is ChatPayload.EncryptedText -> {
                    // 1. 自动解密
                    val plainText = CryptoManager.decrypt(payload.ciphertext, payload.iv)

                    // 2. 转换成普通文本 Payload 供 UI 显示
                    val decryptedPayload = ChatPayload.Text(plainText)
                    val newEnvelope = envelope.copy(payload = decryptedPayload)

                    // 3. 存入数据库（保存的是明文，方便搜索）
                    saveToDatabase(newEnvelope)

                    // 4. 推送到 UI
                    chatFlow.emit(newEnvelope)
                }

                is ChatPayload.Sdp, is ChatPayload.Ice -> {
                    // 信令消息：不存库，直接推送到通话线
                    signalingFlow.emit(payload)
                }

                is ChatPayload.CallAction -> {
                    handleCallAction(envelope)
                }

                is ChatPayload.Text,
                is ChatPayload.Media -> {
                    saveToDatabase(envelope)
                    chatFlow.emit(envelope)
                }

                else -> {}
            }
        }
    }

    private suspend fun saveToDatabase(envelope: MessageEnvelope) {
        val entity = MessageEntity(
            id = envelope.id,
            chatId = envelope.senderId,
            senderId = envelope.senderId,
            senderName = envelope.senderName,
            payload = envelope.payload,
            msgType = "TEXT",
            isFromMe = false,
            status = 1,
            timestamp = envelope.timestamp,
        )
        messageDao.insertMessage(entity)
    }

    private suspend fun handleCallAction(envelope: MessageEnvelope) {
        val action = (envelope.payload as ChatPayload.CallAction).action
        if (action == "START_VIDEO") {
            // 弹出通话 Activity
            val intent = Intent(context, CallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("targetIp", envelope.senderIp)
                putExtra("isOfferer", false)
            }
            context.startActivity(intent)
        }

        // 信令流也要推送
        signalingFlow.emit(envelope.payload)
    }
}