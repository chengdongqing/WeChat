package top.chengdongqing.wechat.core.network.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.spec.ECGenParameterSpec

/**
 * 密钥管理器
 */
@Singleton
class KeyStoreManager @Inject constructor() {

    private companion object {
        const val KEY_ALIAS = "identity_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val STD_NAME = "secp256r1"
    }

    /**
     * 注册时调用，私钥由 KeyStore 管理，永不离开硬件
     * 返回公钥 Base64
     */
    fun generateKeyPair(): String {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        // 如果旧密钥已存在，建议先删除，防止重复创建异常
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec(STD_NAME))
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .build()

        val kpGen = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE
        )
        kpGen.initialize(spec)
        val kp = kpGen.generateKeyPair()

        // 返回 X.509 编码的公钥 Base64
        return Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP)
    }

    /**
     * 发消息、签名时调用
     * 私钥从 KeyStore 取：这个对象内部不包含私钥字节，只包含一个指向硬件安全模块（TEE/SE）的索引（或句柄）
     */
    fun getPrivateKey(): PrivateKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
            ?: error("尚未注册身份密钥")
        return entry.privateKey
    }

    /**
     * 从硬件安全模块中永久物理删除密钥
     */
    fun clearIdentity() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }
}