package top.chengdongqing.wechat.core.data.repository

import kotlinx.coroutines.flow.Flow

interface PrivacySettingsRepository {
    val friendVerifyEnabled: Flow<Boolean>
    suspend fun toggleFriendVerify(enabled: Boolean)
}
