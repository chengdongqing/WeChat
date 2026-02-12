package top.chengdongqing.wechat.features.me.domain.model

import kotlinx.serialization.Serializable
import top.chengdongqing.wechat.core.util.randomUUID

@Serializable
data class UserProfile(
    val id: String,
    val nickname: String,
    val gender: Gender? = null,
    val signature: String? = null,
    val avatarPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {

    companion object {
        /**
         * 生成微信号
         */
        fun generateId(): String {
            return "wxid_${randomUUID().take(12)}"
        }

        /**
         * 验证用户名是否有效
         */
        fun isValidName(name: String): Boolean {
            return name.trim().length in 2..17
        }
    }

    /**
     * 创建更新后的资料副本
     */
    fun copyWithUpdate(
        userName: String? = null,
        gender: Gender? = null,
        signature: String? = null,
        avatarPath: String? = null
    ): UserProfile {
        return copy(
            nickname = userName ?: this.nickname,
            gender = gender ?: this.gender,
            signature = signature ?: this.signature,
            avatarPath = avatarPath ?: this.avatarPath,
            updatedAt = System.currentTimeMillis()
        )
    }
}

/**
 * 性别枚举
 */
@Serializable
enum class Gender(val label: String, val pronoun: String) {
    Male("男", "他"),
    Female("女", "她");

    companion object {
        fun Gender?.getIndex(): Int = this?.ordinal ?: -1

        fun fromIndex(index: Int): Gender? = entries.getOrNull(index)

        val Gender?.safePronoun: String
            get() = this?.pronoun ?: "他"
    }
}