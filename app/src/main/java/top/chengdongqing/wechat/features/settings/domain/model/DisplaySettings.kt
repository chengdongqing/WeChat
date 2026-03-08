package top.chengdongqing.wechat.features.settings.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class DisplaySettings(
    val fontScale: AppFontScale = AppFontScale.Normal,
    val theme: AppTheme = AppTheme.FollowSystem,
    val language: AppLanguage = AppLanguage.FollowSystem
)