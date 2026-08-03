package top.chengdongqing.wechat.core.network.messaging

import android.util.Log
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.first
import top.chengdongqing.wechat.core.data.model.ChatProtocol
import top.chengdongqing.wechat.core.data.model.ReceiptType
import top.chengdongqing.wechat.core.data.repository.ChatSessionRepository
import top.chengdongqing.wechat.core.data.repository.ChatSettingsRepository
import top.chengdongqing.wechat.core.data.repository.ContactRepository
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.model.PermissionResult
import top.chengdongqing.wechat.core.network.crypto.PacketSigner

/**
 * 消息权限检查
 */
@Singleton
class MessagePermissionChecker @Inject constructor(
    private val contactRepository: ContactRepository,
    private val chatSessionRepository: ChatSessionRepository,
    private val chatSettingsRepository: ChatSettingsRepository,
    private val profileRepository: ProfileRepository,
    private val packetSigner: PacketSigner,
    private val messageSender: MessageSender
) {

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
            protocol.senderId != userId -> PermissionResult.InvalidSignature
            contact?.isBlocked == true -> PermissionResult.Blocked
            protocol is ChatProtocol.TemporaryChatInvite -> checkTemporaryInvite(protocol)
            contact == null -> checkTemporarySession(userId, protocol)
            // 验签失败
            contact.publicKey == null || !packetSigner.verify(protocol, contact.publicKey!!) -> {
                Log.w(TAG, "签名验证失败, userId: $userId")
                PermissionResult.InvalidSignature
            }

            else -> PermissionResult.Allowed
        }
    }

    private suspend fun checkTemporaryInvite(
        invite: ChatProtocol.TemporaryChatInvite
    ): PermissionResult {
        if (!chatSettingsRepository.temporaryChatEnabled.first()) {
            return PermissionResult.NotFriend
        }
        val now = System.currentTimeMillis()
        val valid = invite.receiverId == profileRepository.requireUserId() &&
                invite.timestamp in (now - INVITE_CLOCK_SKEW_MS)..(now + INVITE_CLOCK_SKEW_MS) &&
                invite.expiresAt > now &&
                invite.expiresAt <= now + TemporaryChatCoordinator.MAX_DURATION_MS &&
                packetSigner.verifyPresentedKey(invite, invite.publicKey)
        return if (valid) PermissionResult.Allowed else PermissionResult.InvalidSignature
    }

    private suspend fun checkTemporarySession(
        userId: String,
        protocol: ChatProtocol
    ): PermissionResult {
        // 总开关不仅控制邀请，也必须控制已有临时会话的后续消息。
        if (!chatSettingsRepository.temporaryChatEnabled.first()) {
            return PermissionResult.NotFriend
        }
        // 邀请处理直接更新数据库，这里读取实时 Flow，避免旧会话缓存导致误判“非好友”。
        val session = chatSessionRepository.observeSession(userId).first()
        val publicKey = session?.temporaryPeerPublicKey
        val valid = session?.isTemporary == true &&
                session.expiresAt?.let { it > System.currentTimeMillis() } == true &&
                publicKey != null && packetSigner.verify(protocol, publicKey)
        return if (valid) PermissionResult.Allowed else PermissionResult.NotFriend
    }

    private companion object {
        const val TAG = "MessagePermissionChecker"
        const val INVITE_CLOCK_SKEW_MS = 2 * 60 * 1000L
    }
}
