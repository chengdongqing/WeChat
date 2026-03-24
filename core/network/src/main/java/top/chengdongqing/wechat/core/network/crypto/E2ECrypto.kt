package top.chengdongqing.wechat.core.network.crypto

import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 端到端加密核心
 *
 * 密钥交换：ECDH (secp256r1)
 * 密钥派生：HKDF-SHA256
 * 对称加密：AES-256-GCM（认证加密，防篡改）
 *
 * 加密格式：[IV 12B][密文 + GCM Tag 16B]
 */
@Singleton
class E2ECrypto @Inject constructor() {

    companion object {
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val SESSION_KEY_LENGTH = 32

        private val secureRandom = SecureRandom()
    }

    /**
     * 本地密钥对
     *
     * 每次握手生成一个新实例，实现前向保密。
     * 私钥仅存于内存，不持久化。
     */
    data class LocalKeyPair(
        val publicKeyEncoded: String,       // Base64 编码的公钥，发给对方
        private val privateKey: java.security.PrivateKey
    ) {
        /** 与对方公钥做 ECDH，派生共享 session key */
        fun deriveSessionKey(peerPublicKeyEncoded: String): ByteArray {
            val peerKey: PublicKey = Base64.decode(peerPublicKeyEncoded, Base64.NO_WRAP)
                .let { KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(it)) }

            val sharedSecret = KeyAgreement.getInstance("ECDH").run {
                init(privateKey)
                doPhase(peerKey, true)
                generateSecret()
            }

            return hkdf(sharedSecret)
        }

        // HKDF-SHA256 简化实现（单轮 expand，输出 ≤ 32B 时足够）
        private fun hkdf(ikm: ByteArray, length: Int = SESSION_KEY_LENGTH): ByteArray {
            val prk = hmacSha256(key = ByteArray(32), data = ikm)       // Extract
            return hmacSha256(key = prk, data = byteArrayOf(0x01))      // Expand
                .copyOf(length)
        }

        private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray =
            Mac.getInstance("HmacSHA256").run {
                init(SecretKeySpec(key, "HmacSHA256"))
                doFinal(data)
            }
    }


    /**
     * 生成密钥对
     */
    fun generateKeyPair(): LocalKeyPair {
        val kp = KeyPairGenerator.getInstance("EC").run {
            initialize(ECGenParameterSpec("secp256r1"))
            generateKeyPair()
        }
        return LocalKeyPair(
            publicKeyEncoded = Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP),
            privateKey = kp.private
        )
    }

    /**
     * 加密，输出格式：[IV 12B][密文 + GCM Tag]
     */
    fun encrypt(plaintext: ByteArray, sessionKey: ByteArray): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(sessionKey, "AES"),
            GCMParameterSpec(GCM_TAG_LENGTH, iv)
        )
        return iv + cipher.doFinal(plaintext)
    }

    /**
     * 解密，输入格式：[IV 12B][密文 + GCM Tag]
     */
    fun decrypt(data: ByteArray, sessionKey: ByteArray): ByteArray {
        require(data.size > GCM_IV_LENGTH) { "数据过短，不是合法的加密包" }
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)

        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(sessionKey, "AES"),
            GCMParameterSpec(GCM_TAG_LENGTH, data.copyOf(GCM_IV_LENGTH))
        )
        return cipher.doFinal(data.copyOfRange(GCM_IV_LENGTH, data.size))
    }
}