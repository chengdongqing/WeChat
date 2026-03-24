package top.chengdongqing.wechat.core.network.connection

import jakarta.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import top.chengdongqing.wechat.core.data.model.ChatProtocol
import top.chengdongqing.wechat.core.data.repository.ChatSettingsRepository
import top.chengdongqing.wechat.core.network.crypto.E2ESessionManager
import top.chengdongqing.wechat.core.network.crypto.PacketSigner
import top.chengdongqing.wechat.core.network.model.Packet
import top.chengdongqing.wechat.core.network.model.PacketReader
import top.chengdongqing.wechat.core.network.model.PacketType.HANDSHAKE
import top.chengdongqing.wechat.core.network.model.PacketWriter
import top.chengdongqing.wechat.core.network.security.KeyStoreManager

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
    private val packetSigner: PacketSigner,
    private val keyStoreManager: KeyStoreManager
) {
    private suspend fun isE2eEnabled() = chatSettingsRepository.e2eEnabled.first()

    /**
     * 主动方：生成握手包，携带己方公钥（E2E 未开启时公钥为 null）
     */
    suspend fun buildHandshakePacket(peerId: String, myUserId: String): Packet {
        e2e.removeSession(peerId)
        val e2eKey = if (isE2eEnabled()) e2e.prepareHandshake(peerId) else null
        val hs = ChatProtocol.Handshake(
            senderId = myUserId,
            e2ePublicKey = e2eKey,
            signature = ""
        )
        val signature = packetSigner.sign(hs, keyStoreManager.getPrivateKey())

        return Packet(
            type = HANDSHAKE,
            body = json.encodeToString<ChatProtocol>(hs.copy(signature = signature))
                .toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * 被动方（Server）：解析握手包，完成密钥交换并返回 ACK 包和 senderId。
     * 失败返回 null。
     */
    fun acceptHandshake(reader: PacketReader, writer: PacketWriter): String? = runCatching {
        val packet = reader.read()
        if (packet.type != HANDSHAKE) return null

        val hs = json.decodeFromString<ChatProtocol.Handshake>(
            String(packet.body, Charsets.UTF_8)
        )

        hs.e2ePublicKey?.let { peerKey ->
            val myKey = e2e.acceptHandshake(hs.senderId, peerKey)
            val ack = ChatProtocol.Handshake(
                senderId = hs.senderId,
                e2ePublicKeyAck = myKey,
                signature = ""
            )
            val signature = packetSigner.sign(ack, keyStoreManager.getPrivateKey())

            writer.write(
                Packet(
                    type = HANDSHAKE,
                    body = json.encodeToString<ChatProtocol>(ack.copy(signature = signature))
                        .toByteArray(Charsets.UTF_8)
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
            val hs = json.decodeFromString<ChatProtocol.Handshake>(
                String(packet.body, Charsets.UTF_8)
            )

            hs.e2ePublicKey?.let { peerKey ->
                val myKey = e2e.acceptHandshake(conn.userId, peerKey)
                val ack = ChatProtocol.Handshake(
                    senderId = conn.userId,
                    e2ePublicKeyAck = myKey,
                    signature = ""
                )
                val signature = packetSigner.sign(ack, keyStoreManager.getPrivateKey())

                conn.writer.write(
                    Packet(
                        type = HANDSHAKE,
                        body = json.encodeToString<ChatProtocol>(ack.copy(signature = signature))
                            .toByteArray(Charsets.UTF_8)
                    )
                )
            }
            hs.e2ePublicKeyAck?.let { e2e.completeHandshake(conn.userId, it) }
        }
    }
}