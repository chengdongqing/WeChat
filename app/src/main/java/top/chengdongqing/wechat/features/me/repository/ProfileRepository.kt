package top.chengdongqing.wechat.features.me.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.data.model.UserProfile

interface ProfileRepository {
    suspend fun createProfile(profile: UserProfile): Result<Unit>
    suspend fun updateProfile(profile: UserProfile): Result<Unit>
    fun getProfile(): Flow<UserProfile?>
    suspend fun syncProfileWithPeers()  // P2P 同步
}