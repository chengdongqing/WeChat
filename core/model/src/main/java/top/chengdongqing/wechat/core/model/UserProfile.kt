package top.chengdongqing.wechat.core.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import java.util.UUID

@Immutable
@Serializable
data class UserProfile(
    val id: String,
    val nickname: String,
    val gender: Gender? = null,
    val signature: String? = null,
    val avatarPath: String? = null,
    val publicKey: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun generateId(): String = "wxid_${randomUUID().take(12)}"
        fun isValidName(name: String): Boolean = name.trim().length in 2..17
    }

    fun copyWithUpdate(
        nickname: String? = null,
        gender: Gender? = null,
        signature: String? = null,
        avatarPath: String? = null
    ): UserProfile = copy(
        nickname = nickname ?: this.nickname,
        gender = gender ?: this.gender,
        signature = signature ?: this.signature,
        avatarPath = avatarPath ?: this.avatarPath,
        updatedAt = System.currentTimeMillis()
    )
}

private fun randomUUID() = UUID.randomUUID().toString().replace("-", "")