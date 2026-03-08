package top.chengdongqing.wechat.features.me.domain.repository

import kotlinx.coroutines.flow.Flow

// TODO 删除此文件
interface SettingsRepository {

    /**
     * 是否开启扬声器播放
     */
    val speakerEnabled: Flow<Boolean>

    /**
     * 切换 扬声器/听筒
     */
    suspend fun toggleSpeaker(enabled: Boolean)
}