package top.chengdongqing.wechat.data.network.crypto

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import top.chengdongqing.wechat.data.network.protocol.Packet
import top.chengdongqing.wechat.data.network.protocol.PacketType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class E2ESessionManager @Inject constructor(
    private val crypto: E2ECrypto
) {
    private companion object {
        const val TAG = "E2ESessionManager"
    }

    private data class Session(
        val sessionKey: ByteArray,
        val isTemporary: Boolean  // 因对方开启而被动激活
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Session

            if (isTemporary != other.isTemporary) return false
            if (!sessionKey.contentEquals(other.sessionKey)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = isTemporary.hashCode()
            result = 31 * result + sessionKey.contentHashCode()
            return result
        }
    }

    private val sessions = mutableMapOf<String, Session>()

    // 等待对方 ACK 的密钥对，peerId -> LocalKeyPair
    private val pendingKeyPairs = mutableMapOf<String, E2ECrypto.LocalKeyPair>()

    // 对外暴露活跃加密连接，UI 可显示锁头图标
    private val _encryptedPeers = MutableStateFlow<Set<String>>(emptySet())
    val encryptedPeers: StateFlow<Set<String>> = _encryptedPeers.asStateFlow()

    fun hasSession(peerId: String) = sessions.containsKey(peerId)

    // ==================== 握手流程 ====================

    /**
     * 【主动方】Step 1：生成密钥对，返回我的公钥，放入 Handshake.e2ePublicKey
     */
    fun prepareHandshake(peerId: String): String {
        val kp = crypto.generateKeyPair()
        pendingKeyPairs[peerId] = kp
        Log.d(TAG, "准备握手: $peerId")
        return kp.publicKeyEncoded
    }

    /**
     * 【主动方】Step 3：收到对方的 ACK 公钥，完成握手
     *
     * 调用时机：收到 Handshake.e2ePublicKeyAck 时
     */
    fun completeHandshake(peerId: String, peerPublicKey: String) {
        val kp = pendingKeyPairs.remove(peerId) ?: run {
            Log.w(TAG, "completeHandshake: 无 pending 密钥对 ($peerId)")
            return
        }
        val sessionKey = kp.deriveSessionKey(peerPublicKey)
        saveSession(peerId, sessionKey, isTemporary = false)
        Log.d(TAG, "握手完成 (主动方): $peerId")
    }

    /**
     * 【被动方】Step 2：收到对方公钥，派生 sessionKey，返回我的公钥
     *
     * 调用时机：收到 Handshake.e2ePublicKey 时
     * @return 要放入 Handshake.e2ePublicKeyAck 的公钥
     */
    fun acceptHandshake(peerId: String, peerPublicKey: String): String {
        val kp = crypto.generateKeyPair()
        val sessionKey = kp.deriveSessionKey(peerPublicKey)
        // isTemporary = 被动激活（本地设置未开启但对方开启了）
        saveSession(peerId, sessionKey, isTemporary = true)
        Log.d(TAG, "握手完成 (被动方，临时激活): $peerId")
        return kp.publicKeyEncoded
    }

    // ==================== 加解密 ====================

    fun encrypt(peerId: String, data: ByteArray): ByteArray =
        crypto.encrypt(data, requireSession(peerId).sessionKey)

    fun decrypt(peerId: String, data: ByteArray): ByteArray =
        crypto.decrypt(data, requireSession(peerId).sessionKey)

    fun encryptPacket(peerId: String, packet: Packet): Packet {
        if (packet.type in PacketType.PLAINTEXT_TYPES) {
            Log.d(TAG, "⏭️ 跳过加密 (控制包): type=0x${packet.type.toString(16)} peerId=$peerId")
            return packet
        }
        if (!hasSession(peerId)) {
            Log.d(
                TAG,
                "⏭️ 跳过加密 (无 session): type=0x${packet.type.toString(16)} peerId=$peerId"
            )
            return packet
        }
        return runCatching {
            val encrypted =
                Packet(PacketType.encryptedType(packet.type), encrypt(peerId, packet.body))
            Log.d(
                TAG,
                "🔒 加密成功: type=0x${packet.type.toString(16)}→0x${encrypted.type.toString(16)} plainSize=${packet.body.size} encSize=${encrypted.body.size} peerId=$peerId"
            )
            encrypted
        }.getOrElse {
            Log.e(TAG, "❌ 加密失败，降级明文: type=0x${packet.type.toString(16)} peerId=$peerId", it)
            packet
        }
    }

    fun decryptPacket(peerId: String, packet: Packet): Packet {
        if (!PacketType.isEncrypted(packet.type)) {
            Log.d(TAG, "⏭️ 跳过解密 (明文包): type=0x${packet.type.toString(16)} peerId=$peerId")
            return packet
        }
        val baseType = PacketType.realType(packet.type)
        if (!hasSession(peerId)) {
            Log.w(
                TAG,
                "⚠️ 收到加密包但无 session: type=0x${packet.type.toString(16)} peerId=$peerId"
            )
            return Packet(baseType, ByteArray(0))
        }
        return runCatching {
            val decrypted = Packet(baseType, decrypt(peerId, packet.body))
            Log.d(
                TAG,
                "🔓 解密成功: type=0x${packet.type.toString(16)}→0x${baseType.toString(16)} encSize=${packet.body.size} plainSize=${decrypted.body.size} peerId=$peerId",

                )
            decrypted
        }.getOrElse {
            Log.e(
                TAG,
                "❌ 解密失败，可能被篡改: type=0x${packet.type.toString(16)} peerId=$peerId",
                it
            )
            Packet(baseType, ByteArray(0))
        }
    }

    // ==================== 生命周期 ====================

    /**
     * 用户关闭 E2E 时调用；被动激活（temporary）的 session 不会被主动清除，
     * 只有在对方也关闭（收到无公钥握手包）或连接断开时才清除。
     */
    fun removeSession(peerId: String) {
        sessions.remove(peerId)
        pendingKeyPairs.remove(peerId)
        _encryptedPeers.update { it - peerId }
        Log.d(TAG, "E2E session 已移除: $peerId")
    }

    // ==================== 私有 ====================

    private fun saveSession(peerId: String, key: ByteArray, isTemporary: Boolean) {
        sessions[peerId] = Session(key, isTemporary)
        _encryptedPeers.update { it + peerId }
        Log.d(TAG, "✅ session 已建立: $peerId, key=${key.take(4)}")
    }

    private fun requireSession(peerId: String): Session =
        sessions[peerId] ?: throw IllegalStateException("无 E2E session: $peerId")
}