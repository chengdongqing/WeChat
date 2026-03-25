package top.chengdongqing.wechat.core.model

/**
 * 通话类型
 */
enum class CallType {
    Voice,
    Video;

    val isVideoCall: Boolean get() = this == Video
}
