package top.chengdongqing.wechat.home

data class HomeActions(
    val onSearchClick: () -> Unit = {},
    val onQuickActionClick: (QuickActionType) -> Unit = {},
    val onTabSelected: (HomeTab) -> Unit = {},
    val onQrCodeScanned: (String) -> Unit = {},
    val onChatWithAI: () -> Unit = {}
)