package top.chengdongqing.wechat.features.settings.domain.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.features.settings.domain.model.AppFontScale
import top.chengdongqing.wechat.features.settings.domain.model.AppLanguage
import top.chengdongqing.wechat.features.settings.domain.model.AppTheme
import top.chengdongqing.wechat.features.settings.domain.model.DisplaySettings

interface DisplaySettingsRepository {

    /** 所有显示设置 */
    val settings: Flow<DisplaySettings>

    suspend fun saveFontScale(fontScale: AppFontScale)

    suspend fun saveTheme(theme: AppTheme)

    suspend fun saveLanguage(language: AppLanguage)
}