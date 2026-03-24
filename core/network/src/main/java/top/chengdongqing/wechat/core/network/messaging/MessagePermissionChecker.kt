package top.chengdongqing.wechat.core.network.messaging

import android.util.Log
import jakarta.inject.Inject
import jakarta.inject.Singleton
import top.chengdongqing.wechat.core.data.model.ChatProtocol
import top.chengdongqing.wechat.core.data.model.ReceiptType
import top.chengdongqing.wechat.core.data.repository.ContactRepository
import top.chengdongqing.wechat.core.model.PermissionResult
import top.chengdongqing.wechat.core.network.crypto.PacketSigner

/**
 * 消息权限检查
 */
@Singleton
class MessagePermissionChecker @Inject constructor(
    private val contactRepository: ContactRepository,
    private val packetSigner: PacketSigner,
    private val messageSender: MessageSender
) {

    private companion object {
        const val TAG = "MessagePermissionChecker"
    }

    /**
     * 检查权限并回复给对方
     */
    suspend fun checkAndReply(userId: String, protocol: ChatProtocol): Boolean {
        val messageId = protocol.messageId

        return when (check(userId, protocol)) {
            PermissionResult.Blocked -> {
                messageSender.sendReceipt(messageId, userId, ReceiptType.Blocked)
                false
            }

            PermissionResult.NotFriend -> {
                messageSender.sendReceipt(messageId, userId, ReceiptType.NotFriend)
                false
            }

            PermissionResult.InvalidSignature -> {
                messageSender.sendReceipt(messageId, userId, ReceiptType.InvalidSignature)
                false
            }

            else -> true
        }
    }

    private suspend fun check(
        userId: String,
        protocol: ChatProtocol
    ): PermissionResult {
        val contact = contactRepository.getContact(userId)

        return when {
            // 非好友
            contact == null -> PermissionResult.NotFriend
            // 被拉黑
            contact.isBlocked -> PermissionResult.Blocked
            // 验签失败
            contact.publicKey == null || !packetSigner.verify(protocol, contact.publicKey!!) -> {
                Log.w(TAG, "签名验证失败, userId: $userId")
                PermissionResult.InvalidSignature
            }

            else -> PermissionResult.Allowed
        }
    }
}