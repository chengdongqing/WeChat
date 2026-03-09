package top.chengdongqing.wechat.features.home.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import top.chengdongqing.wechat.R

/**
 * 快捷操作枚举
 */
enum class QuickAction(
    @get:DrawableRes val icon: Int,
    @get:StringRes val label: Int,
) {
    GroupChat(R.drawable.ic_chats_filled, R.string.home_action_new_group),
    AddFriend(R.drawable.ic_add_friends_filled, R.string.home_action_add_friend),
    Scan(R.drawable.ic_scan_filled, R.string.home_action_scan),
    Payment(R.drawable.ic_pay_vendor_filled, R.string.home_action_payment),
}