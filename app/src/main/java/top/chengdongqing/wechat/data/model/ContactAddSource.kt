package top.chengdongqing.wechat.data.model

enum class ContactAddSource(val label: String) {
    Search("搜索账号"),
    QRCode("扫一扫"),
    Tap("碰一碰"),
    Radar("雷达扫描"),
    Group("群聊"),
    Card("名片分享");

    fun getDescription(isFromMe: Boolean): String = when (isFromMe) {
        true -> "通过${label}添加"
        false -> "对方通过${label}添加"
    }
}