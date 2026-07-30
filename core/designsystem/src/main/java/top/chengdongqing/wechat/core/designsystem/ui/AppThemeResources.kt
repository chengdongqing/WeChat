package top.chengdongqing.wechat.core.designsystem.ui

import androidx.annotation.StringRes
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.model.AppTheme

@get:StringRes
val AppTheme.labelRes: Int
    get() = when (this) {
        AppTheme.FollowSystem -> R.string.settings_follow_system
        AppTheme.Light -> R.string.display_theme_light
        AppTheme.Dark -> R.string.display_theme_dark
    }
