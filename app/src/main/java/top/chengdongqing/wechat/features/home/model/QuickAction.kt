package top.chengdongqing.wechat.features.home.model

import androidx.annotation.DrawableRes
import top.chengdongqing.wechat.R

/**
 * 快捷操作枚举
 */
enum class QuickAction(
    @param:DrawableRes val icon: Int,
    val label: String,
) {
    GroupChat(R.drawable.ic_chats_filled, "发起群聊"),
    AddFriend(R.drawable.ic_add_friends_filled, "添加朋友"),
    Scan(R.drawable.ic_scan_filled, "扫一扫"),
    Payment(R.drawable.ic_pay_vendor_filled, "收付款"),
}