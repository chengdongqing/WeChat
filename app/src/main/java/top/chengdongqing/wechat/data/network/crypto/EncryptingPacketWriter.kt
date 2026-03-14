package top.chengdongqing.wechat.data.network.crypto

import top.chengdongqing.wechat.data.network.model.Packet
import top.chengdongqing.wechat.data.network.model.PacketWriter

/**
 * 透明加密代理
 *
 * 包裹 [PacketWriter]，发包前自动经过 [E2ESessionManager.encryptPacket]。
 * 无 session 时透明放行，上层无需感知加密状态。
 */
class EncryptingPacketWriter(
    private val writer: PacketWriter,
    private val peerId: String,
    private val e2e: E2ESessionManager
) {
    fun write(packet: Packet) = writer.write(e2e.encryptPacket(peerId, packet))

    fun writeNoFlush(packet: Packet) = writer.writeNoFlush(e2e.encryptPacket(peerId, packet))
}