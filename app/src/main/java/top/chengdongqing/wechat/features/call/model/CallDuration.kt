package top.chengdongqing.wechat.features.call.model

/**
 * 通话持续时间
 */
data class CallDuration(
    val seconds: Long = 0
) {
    /**
     * 格式化为 MM:SS
     */
    fun format(): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(minutes, secs)
    }

    /**
     * 增加一秒
     */
    fun increment() = copy(seconds = seconds + 1)
}