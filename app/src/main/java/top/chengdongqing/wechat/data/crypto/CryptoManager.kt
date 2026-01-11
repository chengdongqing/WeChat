package top.chengdongqing.wechat.data.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH = 128
    private const val IV_LENGTH = 12

    // 实际项目中，这个 key 应该通过 ECDH 协商生成，这里模拟一个 32 字节密钥
    private val secretKey = SecretKeySpec("12345678901234567890123456789012".toByteArray(), "AES")

    fun encrypt(plainText: String): Pair<String, String> {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_LENGTH).apply { SecureRandom().nextBytes(this) }
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH, iv))

        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP) to
                Base64.encodeToString(iv, Base64.NO_WRAP)
    }

    fun decrypt(ciphertext: String, iv: String): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val ivBytes = Base64.decode(iv, Base64.NO_WRAP)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH, ivBytes))

            val decodedCiphertext = Base64.decode(ciphertext, Base64.NO_WRAP)
            String(cipher.doFinal(decodedCiphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            "【解密失败】"
        }
    }
}