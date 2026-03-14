package top.chengdongqing.wechat.data.network.connection

import jakarta.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.data.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.data.network.model.ChatProtocol
import top.chengdongqing.wechat.data.network.model.Packet
import top.chengdongqing.wechat.data.network.model.PacketReader
import top.chengdongqing.wechat.data.network.model.PacketType.HANDSHAKE
import top.chengdongqing.wechat.data.network.model.PacketWriter
import top.chengdongqing.wechat.features.settings.domain.repository.ChatSettingsRepository

/**
 * ECDH 握手处理器，服务端和客户端共用同一套密钥交换逻辑。
 *
 * 握手方向：
 * - 主动方（Client）：发送己方公钥 → 等待 ACK → 用 ACK 完成派生
 * - 被动方（Server）：收到公钥 → 派生 session key → 回传 ACK
 */
class PeerHandshakeHandler @Inject constructor(
    private val json: Json,
    private val e2e: E2ESessionManager,
    private val chatSettingsRepository: ChatSettingsRepository,
) {
    private suspend fun isE2eEnabled() = chatSettingsRepository.e2eEnabled.first()

    /** 主动方：生成握手包，携带己方公钥（E2E 未开启时公钥为 null） */
    suspend fun buildHandshakePacket(peerId: String, myUserId: String): Packet {
        e2e.removeSession(peerId) // 清除旧 session，防止密钥残留
        val e2eKey = if (isE2eEnabled()) e2e.prepareHandshake(peerId) else null
        val hs = ChatProtocol.Handshake(senderId = myUserId, e2ePublicKey = e2eKey)
        return Packet(HANDSHAKE, json.encodeToString<ChatProtocol>(hs).toByteArray(Charsets.UTF_8))
    }

    /**
     * 被动方（Server）：解析握手包，完成密钥交换并返回 ACK 包和 senderId。
     * 失败返回 null。
     */
    fun acceptHandshake(reader: PacketReader, writer: PacketWriter): String? = runCatching {
        val packet = reader.read()
        if (packet.type != HANDSHAKE) return null
        val hs = json.decodeFromString<ChatProtocol.Handshake>(String(packet.body, Charsets.UTF_8))
        hs.e2ePublicKey?.let { peerKey ->
            val myKey = e2e.acceptHandshake(hs.senderId, peerKey)
            val ack = ChatProtocol.Handshake(senderId = hs.senderId, e2ePublicKeyAck = myKey)
            writer.write(
                Packet(
                    HANDSHAKE,
                    json.encodeToString<ChatProtocol>(ack).toByteArray(Charsets.UTF_8)
                )
            )
        }
        hs.senderId
    }.getOrNull()

    /**
     * 主动方（Client）收到握手回包时的处理：
     * - e2ePublicKey 非空：被动侧在此回合发起，回传 ACK
     * - e2ePublicKeyAck 非空：收到 ACK，完成派生，握手结束
     */
    fun handleHandshakeReply(conn: PeerConnection, packet: Packet) {
        runCatching {
            val hs =
                json.decodeFromString<ChatProtocol.Handshake>(String(packet.body, Charsets.UTF_8))
            hs.e2ePublicKey?.let { peerKey ->
                val myKey = e2e.acceptHandshake(conn.userId, peerKey)
                val ack = ChatProtocol.Handshake(senderId = conn.userId, e2ePublicKeyAck = myKey)
                conn.writer.write(
                    Packet(
                        HANDSHAKE,
                        json.encodeToString<ChatProtocol>(ack).toByteArray(Charsets.UTF_8)
                    )
                )
            }
            hs.e2ePublicKeyAck?.let { e2e.completeHandshake(conn.userId, it) }
        }
    }
}