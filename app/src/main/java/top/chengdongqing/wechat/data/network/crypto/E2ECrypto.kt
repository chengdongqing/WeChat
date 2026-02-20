package top.chengdongqing.wechat.data.network.crypto

import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class E2ECrypto @Inject constructor() {

    companion object {
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }

    data class LocalKeyPair(
        val publicKeyEncoded: String,               // Base64，发给对方
        private val privateKey: java.security.PrivateKey
    ) {
        fun deriveSessionKey(peerPublicKeyEncoded: String): ByteArray {
            val peerKeyBytes = Base64.decode(peerPublicKeyEncoded, Base64.NO_WRAP)
            val peerKey: PublicKey = KeyFactory.getInstance("EC")
                .generatePublic(X509EncodedKeySpec(peerKeyBytes))
            val ka = KeyAgreement.getInstance("ECDH")
            ka.init(privateKey)
            ka.doPhase(peerKey, true)
            val sharedSecret = ka.generateSecret()
            return hkdf(sharedSecret, 32)
        }

        private fun hkdf(ikm: ByteArray, length: Int): ByteArray {
            val prk = hmacSha256(ByteArray(32), ikm)
            return hmacSha256(prk, byteArrayOf(0x01)).copyOf(length)
        }

        private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(key, "HmacSHA256"))
            return mac.doFinal(data)
        }
    }

    fun generateKeyPair(): LocalKeyPair {
        val gen = KeyPairGenerator.getInstance("EC")
        gen.initialize(ECGenParameterSpec("secp256r1"))
        val kp = gen.generateKeyPair()
        return LocalKeyPair(
            publicKeyEncoded = Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP),
            privateKey = kp.private
        )
    }

    /** 加密：输出 = [IV 12B | 密文 + GCM Tag] */
    fun encrypt(plaintext: ByteArray, sessionKey: ByteArray): ByteArray {
        val iv = Random.nextBytes(GCM_IV_LENGTH)
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(sessionKey, "AES"),
            GCMParameterSpec(GCM_TAG_LENGTH, iv)
        )
        return iv + cipher.doFinal(plaintext)
    }

    /** 解密：输入 = [IV 12B | 密文 + GCM Tag] */
    fun decrypt(data: ByteArray, sessionKey: ByteArray): ByteArray {
        require(data.size > GCM_IV_LENGTH) { "密文过短" }
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(sessionKey, "AES"),
            GCMParameterSpec(GCM_TAG_LENGTH, data.copyOf(GCM_IV_LENGTH))
        )
        return cipher.doFinal(data.copyOfRange(GCM_IV_LENGTH, data.size))
    }
}