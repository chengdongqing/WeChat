package top.chengdongqing.wechat.features.settings.domain.repository

import kotlinx.coroutines.flow.Flow

interface PrivacySettingsRepository {

    /**
     * 开启好友验证开关
     */
    val friendVerifyEnabled: Flow<Boolean>

    suspend fun toggleFriendVerify(enabled: Boolean)

    /**
     * 退出登录时清除所有设置
     */
    suspend fun clearAll()
}