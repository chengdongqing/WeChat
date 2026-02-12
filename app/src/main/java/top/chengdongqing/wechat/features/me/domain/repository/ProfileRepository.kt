package top.chengdongqing.wechat.features.me.domain.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.features.me.domain.model.Gender
import top.chengdongqing.wechat.features.me.domain.model.UserProfile

interface ProfileRepository {

    /**
     * 获取当前用户资料（Flow）
     */
    fun getCurrentProfile(): Flow<UserProfile?>

    /**
     * 获取当前用户资料（挂起）
     */
    suspend fun getCurrentProfileOnce(): UserProfile?

    /**
     * 保存用户资料
     */
    suspend fun saveProfile(profile: UserProfile): Result<Unit>

    /**
     * 更新用户资料
     */
    suspend fun updateProfile(
        nickname: String? = null,
        gender: Gender? = null,
        signature: String? = null,
        avatarPath: String? = null
    ): Result<Unit>

    /**
     * 检查是否已设置过资料
     */
    suspend fun hasProfile(): Boolean

    /**
     * 清除用户资料（用于测试或重置）
     */
    suspend fun clearProfile(): Result<Unit>
}