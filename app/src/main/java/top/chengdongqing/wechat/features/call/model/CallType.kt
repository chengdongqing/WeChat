package top.chengdongqing.wechat.features.call.model

/**
 * 通话类型
 */
enum class CallType(val label: String) {
    Voice("语音通话"),
    Video("视频通话");

    val isVideoCall: Boolean get() = this == Video
}