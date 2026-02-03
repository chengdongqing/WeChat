package top.chengdongqing.wechat.data.model

import top.chengdongqing.wechat.core.util.randomUUID
import java.io.Serializable

data class User(
    val id: String,
    val name: String,
    val gender: Gender,
    val signature: String? = null,
    val avatarPath: String? = null,
    val version: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Serializable {

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
    ): User {
        return copy(
            name = userName ?: this.name,
            gender = gender ?: this.gender,
            signature = signature ?: this.signature,
            avatarPath = avatarPath ?: this.avatarPath,
            version = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}

/**
 * 性别枚举
 */
enum class Gender(val label: String) {
    Male("男"),
    Female("女"),
    Unknown("未知")
}