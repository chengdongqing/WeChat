package top.chengdongqing.wechat.features.call.model

/**
 * 通话用户信息
 */
data class CallUser(
    val id: String,
    val name: String,
    val avatar: String? = null
)