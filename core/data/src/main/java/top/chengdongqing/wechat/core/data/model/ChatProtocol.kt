package top.chengdongqing.wechat.core.data.model

import kotlinx.serialization.Serializable
import top.chengdongqing.wechat.core.common.media.RingtoneSound
import top.chengdongqing.wechat.core.model.CallType
import top.chengdongqing.wechat.core.model.HangupReason
import top.chengdongqing.wechat.core.model.MessageType
import top.chengdongqing.wechat.core.model.ProfileBeacon

@Serializable
sealed class ChatProtocol {

    abstract val messageId: String
    abstract val senderId: String
    abstract val signature: String
    abstract val timestamp: Long

    abstract fun signingPayload(): String

    @Serializable
    data class TextMessage(
        override val messageId: String,
        override val senderId: String,
        override val signature: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val receiverId: String,
        val messageType: MessageType,
        val content: String
    ) : ChatProtocol() {
        override fun signingPayload() =
            "$messageId|$senderId|$receiverId|$messageType|$content|$timestamp"
    }

    /**
     * 群文本消息。route/ttl 是可变的 Mesh 传输元数据，不进入原作者签名；
     * 其余字段由原发送者签名，任何中继节点都无法篡改正文或目标群。
     */
    @Serializable
    data class GroupTextMessage(
        override val messageId: String,
        override val senderId: String,
        override val signature: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val groupId: String,
        val memberVersion: Long,
        val messageType: MessageType,
        val content: String,
        val ttl: Int = 6,
        val route: List<String> = emptyList()
    ) : ChatProtocol() {
        override fun signingPayload() =
            "$messageId|$senderId|$groupId|$memberVersion|$messageType|$content|$timestamp"
    }

    /** 群资料与成员快照。新建群、成员变化和修改群名均通过该协议同步。 */
    @Serializable
    data class GroupSnapshot(
        override val messageId: String,
        override val senderId: String,
        override val signature: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val groupId: String,
        val name: String,
        val announcement: String? = null,
        val ownerId: String,
        val memberVersion: Long,
        val members: List<GroupMemberSnapshot>
    ) : ChatProtocol() {
        override fun signingPayload() =
            "$messageId|$senderId|$groupId|$name|$announcement|$ownerId|$memberVersion|$members|$timestamp"
    }

    /** 群直播房间事件与 WebRTC 信令，不作为聊天消息持久化。 */
    @Serializable
    data class GroupLiveEvent(
        override val messageId: String,
        override val senderId: String,
        override val signature: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val groupId: String,
        val liveId: String,
        val status: String,
        val displayName: String,
        val targetId: String? = null,
        val payload: String? = null
    ) : ChatProtocol() {
        override fun signingPayload() =
            "$messageId|$senderId|$groupId|$liveId|$status|$displayName|$targetId|$payload|$timestamp"
    }

    data class CallMessage(
        override val messageId: String,
        override val senderId: String,
        override val signature: String = "",
        override val timestamp: Long = System.currentTimeMillis(),
        val receiverId: String,
        val status: String,
        val duration: Long,
        val callType: CallType
    ) : ChatProtocol() {
        override fun signingPayload() =
            "$messageId|$senderId|$receiverId|$status|$duration|$callType|$timestamp"
    }

    @Serializable
    data class MediaMessage(
        override val messageId: String,
        override val senderId: String,
        override val signature: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val receiverId: String,
        val messageType: MessageType,
        val content: String,
        val extension: String?,
        val fileSize: Long,
        val checksum: String,
        val mediaDuration: Long? = null
    ) : ChatProtocol() {
        override fun signingPayload() =
            "$messageId|$senderId|$receiverId|$messageType|$content|$fileSize|$checksum|$mediaDuration|$timestamp"
    }

    @Serializable
    data class MessageReceipt(
        override val messageId: String,
        override val senderId: String,
        override val signature: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val receiverId: String,
        val receiptType: ReceiptType
    ) : ChatProtocol() {
        override fun signingPayload() =
            "$messageId|$senderId|$receiverId|$receiptType|$timestamp"
    }

    @Serializable
    data class ProfileResponse(
        override val messageId: String = "",
        override val senderId: String,
        override val signature: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val profile: ProfileBeacon
    ) : ChatProtocol() {
        override fun signingPayload(): String =
            "$messageId|$senderId|$profile|$timestamp"
    }

    @Serializable
    sealed class Signaling : ChatProtocol() {

        @Serializable
        data class Offer(
            override val messageId: String,
            override val senderId: String,
            override val signature: String,
            override val timestamp: Long = System.currentTimeMillis(),
            val callType: CallType,
            val sdp: String
        ) : Signaling() {
            override fun signingPayload() =
                "$messageId|$senderId|$callType|$sdp|$timestamp"
        }

        @Serializable
        data class Answer(
            override val messageId: String,
            override val senderId: String,
            override val signature: String,
            override val timestamp: Long = System.currentTimeMillis(),
            val callType: CallType,
            val sdp: String
        ) : Signaling() {
            override fun signingPayload() =
                "$messageId|$senderId|$callType|$sdp|$timestamp"
        }

        @Serializable
        data class IceCandidate(
            override val messageId: String,
            override val senderId: String,
            override val signature: String,
            override val timestamp: Long = System.currentTimeMillis(),
            val candidate: String,
            val sdpMid: String?,
            val sdpMLineIndex: Int
        ) : Signaling() {
            override fun signingPayload() =
                "$messageId|$senderId|$candidate|$sdpMid|$sdpMLineIndex|$timestamp"
        }

        @Serializable
        data class Hangup(
            override val messageId: String,
            override val senderId: String,
            override val signature: String,
            override val timestamp: Long = System.currentTimeMillis(),
            val reason: HangupReason,
            val duration: Long
        ) : Signaling() {
            override fun signingPayload() =
                "$messageId|$senderId|$reason|$duration|$timestamp"
        }

        @Serializable
        data class Busy(
            override val messageId: String,
            override val senderId: String,
            override val signature: String,
            override val timestamp: Long = System.currentTimeMillis(),
        ) : Signaling() {
            override fun signingPayload() =
                "$messageId|$senderId|$timestamp"
        }

        @Serializable
        data class MediaState(
            override val messageId: String,
            override val senderId: String,
            override val signature: String,
            override val timestamp: Long = System.currentTimeMillis(),
            val isVideoOn: Boolean = true,
            val isMicOn: Boolean = true,
            val isSpeakerOn: Boolean = true
        ) : Signaling() {
            override fun signingPayload() =
                "$messageId|$senderId|$isVideoOn|$isMicOn|$isSpeakerOn|$timestamp"
        }

        @Serializable
        data class RingtoneInfo(
            override val messageId: String,
            override val senderId: String,
            override val signature: String,
            override val timestamp: Long = System.currentTimeMillis(),
            val ringtone: RingtoneSound
        ) : Signaling() {
            override fun signingPayload() =
                "$messageId|$senderId|$ringtone|$timestamp"
        }
    }

    @Serializable
    data class Handshake(
        override val messageId: String = "",
        override val senderId: String,
        override val signature: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val e2ePublicKey: String? = null,
        val e2ePublicKeyAck: String? = null
    ) : ChatProtocol() {
        override fun signingPayload() =
            "$messageId|$senderId|$e2ePublicKey|$e2ePublicKeyAck|$timestamp"
    }
}

@Serializable
data class GroupMemberSnapshot(
    val userId: String,
    val nickname: String,
    val avatarPath: String? = null,
    val role: String = "Member"
)
