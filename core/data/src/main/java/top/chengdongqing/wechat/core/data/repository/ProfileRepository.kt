package top.chengdongqing.wechat.core.data.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.core.model.Gender
import top.chengdongqing.wechat.core.model.UserProfile

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile?>
    fun getProfile(): UserProfile?
    fun requireProfile(): UserProfile
    fun requireUserId(): String
    suspend fun saveProfile(profile: UserProfile)
    suspend fun updateProfile(
        nickname: String? = null,
        gender: Gender? = null,
        signature: String? = null,
        avatarPath: String? = null
    )
    suspend fun hasProfile(): Boolean
}
