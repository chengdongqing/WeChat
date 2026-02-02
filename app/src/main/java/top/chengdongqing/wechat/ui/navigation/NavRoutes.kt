package top.chengdongqing.wechat.ui.navigation

import androidx.annotation.DrawableRes
import top.chengdongqing.wechat.R

sealed class Screen(
    val route: String,
    val label: String,
    @get:DrawableRes val iconResId: Int? = null,
    @get:DrawableRes val selectedIconResId: Int? = null
) {
    object Welcome : Screen("welcome", "欢迎页")
    object ProfileSetup : Screen("profile_setup", "个人资料初始化页")

    object Home : Screen("home", "首页")

    // 主Tab页面
    object Chats :
        Screen(
            "chats",
            "微信",
            R.drawable.ic_tab_chats_outlined,
            R.drawable.ic_tab_chats_filled
        )

    object Contacts : Screen(
        "contacts",
        "通讯录",
        R.drawable.ic_tab_contacts_outlined,
        R.drawable.ic_tab_contacts_filled
    )

    object Discovery : Screen(
        "discovery",
        "发现",
        R.drawable.ic_tab_discover_outlined,
        R.drawable.ic_tab_discover_filled
    )

    object Me : Screen(
        "me",
        "我",
        R.drawable.ic_tab_me_outlined,
        R.drawable.ic_tab_me_filled
    )

    // 二级页面
    object AddFriend : Screen("add_friend", "添加朋友")
    object RadarScan : Screen("radar_scan", "雷达扫描")
    object PinCodeGroup : Screen("pin_code_group", "面对面建群")
    object ChatSession : Screen("chats/{chatId}", "聊天详情") {
        fun createRoute(chatId: String) = "chats/${chatId}"
    }

    object ChatInfo : Screen("chats/{chatId}/info", "聊天信息") {
        fun createRoute(chatId: String) = "chats/${chatId}/info"
    }

    object ContactDetail : Screen("contacts/{contactId}", "联系人详情") {
        fun createRoute(contactId: String) = "contacts/${contactId}"
    }

    object ContactSetting : Screen("contacts/{contactId}/setting", "朋友设置") {
        fun createRoute(contactId: String) = "contacts/${contactId}/setting"
    }

    object Profile : Screen("profile", "个人资料")
    object Avatar : Screen("profile/avatar", "头像")
    object QRCode : Screen("profile/qrcode", "我的二维码")
    object ID : Screen("profile/id", "微信号")
    object Name : Screen("profile/name", "名字")
    object Signature : Screen("profile/signature", "个性签名")
    object Gender : Screen("profile/gender", "性别")
}