package top.chengdongqing.wechat.core.designsystem.ui

import androidx.annotation.StringRes
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.model.AppLanguage

@get:StringRes
val AppLanguage.labelRes: Int
    get() = when (this) {
        AppLanguage.FollowSystem -> R.string.settings_follow_system
        AppLanguage.Chinese -> R.string.display_language_chinese
        AppLanguage.English -> R.string.display_language_english
    }