package top.chengdongqing.wechat.core.data.repository

import kotlinx.coroutines.flow.Flow
import top.chengdongqing.wechat.core.model.AppFontScale
import top.chengdongqing.wechat.core.model.AppLanguage
import top.chengdongqing.wechat.core.model.AppTheme
import top.chengdongqing.wechat.core.model.DisplaySettings

interface DisplaySettingsRepository {
    val settings: Flow<DisplaySettings>
    suspend fun saveFontScale(fontScale: AppFontScale)
    suspend fun saveTheme(theme: AppTheme)
    suspend fun saveLanguage(language: AppLanguage)
}
