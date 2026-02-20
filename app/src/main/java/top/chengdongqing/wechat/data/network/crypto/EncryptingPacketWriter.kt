package top.chengdongqing.wechat.data.network.crypto

import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.data.network.protocol.PacketType
import top.chengdongqing.wechat.data.network.protocol.PacketWriter

/**
 * 透明加密代理，委托给真实 PacketWriter
 */
class EncryptingPacketWriter(
    private val delegate: PacketWriter,
    private val peerId: String,
    private val e2e: E2ESessionManager
) {
    fun write(packet: Packet) {
        delegate.write(encrypt(packet))
    }

    fun writeNoFlush(packet: Packet) {
        delegate.writeNoFlush(encrypt(packet))
    }

    fun flush() {
        delegate.flush()
    }

    private fun encrypt(packet: Packet): Packet {
        if (packet.type in PacketType.PLAINTEXT_TYPES) return packet
        if (!e2e.hasSession(peerId)) return packet
        return runCatching {
            Packet(PacketType.encryptedType(packet.type), e2e.encrypt(peerId, packet.body))
        }.getOrElse { packet }
    }
}