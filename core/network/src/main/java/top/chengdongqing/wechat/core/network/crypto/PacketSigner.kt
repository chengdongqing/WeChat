package top.chengdongqing.wechat.core.network.crypto

import android.util.Base64
import android.util.LruCache
import top.chengdongqing.wechat.core.data.model.ChatProtocol
import top.chengdongqing.wechat.core.util.getOrPut
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息签名与验签工具
 */
@Singleton
class PacketSigner @Inject constructor() {

    private val publicKeyCache = LruCache<String, PublicKey>(100)

    private companion object {
        const val SIGN_ALGORITHM = "SHA256withECDSA"
        const val KEY_ALGORITHM = "EC"
    }

    /**
     * 发送消息前加签
     */
    fun sign(packet: ChatProtocol, privateKey: PrivateKey): String {
        val signatureBytes = Signature.getInstance(SIGN_ALGORITHM).run {
            initSign(privateKey)
            update(packet.signingPayload().toByteArray(Charsets.UTF_8))
            sign()
        }
        return Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
    }

    /**
     * 收到消息后验签
     */
    fun verify(packet: ChatProtocol, publicKeyBase64: String): Boolean {
        return runCatching {
            val publicKey = resolvePublicKey(packet.senderId, publicKeyBase64)
            val verifier = Signature.getInstance(SIGN_ALGORITHM).apply {
                initVerify(publicKey)
                update(packet.signingPayload().toByteArray(Charsets.UTF_8))
            }
            val signatureBytes = Base64.decode(packet.signature, Base64.NO_WRAP)
            verifier.verify(signatureBytes)
        }.getOrDefault(false)
    }

    /**
     * 使公钥缓存失效
     */
    fun invalidateCache(userId: String) {
        publicKeyCache.remove(userId)
    }

    /**
     * 将公钥 Base64 还原为 PublicKey
     */
    private fun resolvePublicKey(userId: String, publicKeyBase64: String): PublicKey {
        return publicKeyCache.getOrPut(userId) {
            val bytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP)
            val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
            keyFactory.generatePublic(X509EncodedKeySpec(bytes))
        }
    }
}
