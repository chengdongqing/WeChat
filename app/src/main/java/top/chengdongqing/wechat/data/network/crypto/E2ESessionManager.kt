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

/**
 * E2E 会话管理
 *
 * 每个 peerId 对应一个独立的加密 session，session key 仅存于内存（前向保密）。
 *
 * 握手流程（三步）：
 *   主动方 Step1: prepareHandshake()  → 发送 e2ePublicKey
 *   被动方 Step2: acceptHandshake()   → 发送 e2ePublicKeyAck
 *   主动方 Step3: completeHandshake() → session 建立完成
 */
@Singleton
class E2ESessionManager @Inject constructor(
    private val crypto: E2ECrypto
) {
    private companion object {
        const val TAG = "E2ESessionManager"
    }

    private data class Session(
        val sessionKey: ByteArray,
        val isTemporary: Boolean    // true = 因对方开启而被动激活
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Session) return false
            return isTemporary == other.isTemporary && sessionKey.contentEquals(other.sessionKey)
        }

        override fun hashCode() = 31 * isTemporary.hashCode() + sessionKey.contentHashCode()
    }

    private val sessions = mutableMapOf<String, Session>()
    private val pendingKeyPairs = mutableMapOf<String, E2ECrypto.LocalKeyPair>()

    /** 活跃加密连接，UI 可据此显示锁头图标 */
    private val _encryptedPeers = MutableStateFlow<Set<String>>(emptySet())
    val encryptedPeers: StateFlow<Set<String>> = _encryptedPeers.asStateFlow()

    /**
     * 当前是否与指定 peer 存在加密 session
     */
    fun hasSession(peerId: String) = peerId in sessions

    // ==================== 握手 ====================

    /**
     * 【主动方 Step1】生成本次握手的密钥对，返回公钥
     *
     * 公钥放入 Handshake.e2ePublicKey 发给对方，私钥暂存等待 ACK
     */
    fun prepareHandshake(peerId: String): String {
        val kp = crypto.generateKeyPair()
        pendingKeyPairs[peerId] = kp
        Log.d(TAG, "准备握手: $peerId")
        return kp.publicKeyEncoded
    }

    /**
     * 【被动方 Step2】收到对方公钥，派生 session key，返回我的公钥
     *
     * 公钥放入 Handshake.e2ePublicKeyAck 回传，session 立即生效（isTemporary=true）
     */
    fun acceptHandshake(peerId: String, peerPublicKey: String): String {
        val kp = crypto.generateKeyPair()
        saveSession(peerId, kp.deriveSessionKey(peerPublicKey), isTemporary = true)
        Log.d(TAG, "握手完成 (被动方): $peerId")
        return kp.publicKeyEncoded
    }

    /**
     * 【主动方 Step3】收到对方 ACK 公钥，完成握手
     *
     * 取出暂存的私钥派生 session key，握手结束（isTemporary=false）
     */
    fun completeHandshake(peerId: String, peerPublicKey: String) {
        val kp = pendingKeyPairs.remove(peerId) ?: run {
            Log.w(TAG, "completeHandshake: 无 pending 密钥对: $peerId")
            return
        }
        saveSession(peerId, kp.deriveSessionKey(peerPublicKey), isTemporary = false)
        Log.d(TAG, "握手完成 (主动方): $peerId")
    }

    // ==================== 加解密 ====================

    /**
     * 加密 Packet body
     *
     * 控制包（HANDSHAKE/PING/PONG）和无 session 的包透明放行
     */
    fun encryptPacket(peerId: String, packet: Packet): Packet {
        if (packet.type in PacketType.PLAINTEXT_TYPES) return packet
        if (!hasSession(peerId)) {
            Log.d(TAG, "⏭️ 跳过加密 (无 session): peerId=$peerId")
            return packet
        }
        return runCatching {
            val encryptedBody = crypto.encrypt(packet.body, requireSession(peerId).sessionKey)
            Log.d(TAG, buildEncryptLog(packet, encryptedBody, peerId))
            Packet(PacketType.encryptedType(packet.type), encryptedBody)
        }.getOrElse {
            Log.e(TAG, "❌ 加密失败: peerId=$peerId", it)
            packet
        }
    }

    /**
     * 解密 Packet body
     *
     * 无 ENCRYPTED_FLAG 的包透明放行；有 flag 但无 session 则丢弃（返回空 body）
     */
    fun decryptPacket(peerId: String, packet: Packet): Packet {
        if (!PacketType.isEncrypted(packet.type)) return packet
        val baseType = PacketType.realType(packet.type)
        if (!hasSession(peerId)) {
            Log.w(TAG, "⚠️ 收到加密包但无 session: peerId=$peerId")
            return Packet(baseType, ByteArray(0))
        }
        return runCatching {
            val decryptedBody = crypto.decrypt(packet.body, requireSession(peerId).sessionKey)
            Log.d(TAG, buildDecryptLog(packet, decryptedBody, peerId))
            Packet(baseType, decryptedBody)
        }.getOrElse {
            Log.e(TAG, "❌ 解密失败，包可能被篡改: peerId=$peerId", it)
            Packet(baseType, ByteArray(0))
        }
    }

    // ==================== 生命周期 ====================

    /**
     * 移除 session
     *
     * 用户主动关闭 E2E 时调用，isTemporary session 不受影响，
     * 仅在对方断开连接时由外部清除。
     */
    fun removeSession(peerId: String) {
        sessions.remove(peerId)
        pendingKeyPairs.remove(peerId)
        _encryptedPeers.update { it - peerId }
        Log.d(TAG, "session 已移除: $peerId")
    }

    // ==================== 私有 ====================

    private fun saveSession(peerId: String, key: ByteArray, isTemporary: Boolean) {
        sessions[peerId] = Session(key, isTemporary)
        _encryptedPeers.update { it + peerId }
        Log.d(TAG, "✅ session 建立: $peerId isTemporary=$isTemporary key[0..3]=${key.take(4)}")
    }

    private fun requireSession(peerId: String): Session =
        sessions[peerId] ?: throw IllegalStateException("无 E2E session: $peerId")

    private fun buildEncryptLog(plain: Packet, encryptedBody: ByteArray, peerId: String): String {
        val plainText = plain.body.toUtf8OrBinary()
        val cipherPreview = encryptedBody.hexPreview()
        return """
            🔒 加密:
               明文(${plain.body.size}B): ${plainText.take(100) + "..."}
               密文(${encryptedBody.size}B): $cipherPreview [IV=前12B, 密文+Tag=后${encryptedBody.size - 12}B]
               peerId=$peerId
        """.trimIndent()
    }

    private fun buildDecryptLog(cipher: Packet, decryptedBody: ByteArray, peerId: String): String {
        val cipherPreview = cipher.body.hexPreview()
        val plainText = decryptedBody.toUtf8OrBinary()
        return """
            🔓 解密:
               密文(${cipher.body.size}B): $cipherPreview [IV=前12B]
               明文(${decryptedBody.size}B): ${plainText.take(100) + "..."}
               peerId=$peerId
        """.trimIndent()
    }

    private fun ByteArray.hexPreview() =
        take(16).joinToString("") { "%02x".format(it) } + "..."

    private fun ByteArray.toUtf8OrBinary() =
        runCatching { String(this, Charsets.UTF_8) }.getOrElse { "[binary ${size}B]" }
}