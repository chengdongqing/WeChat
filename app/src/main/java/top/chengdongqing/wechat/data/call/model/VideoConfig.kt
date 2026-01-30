package top.chengdongqing.wechat.data.call.model


/**
 * 视频配置
 */
data class VideoConfig(
    val isFrontCamera: Boolean = true,
    val isLocalVideoEnabled: Boolean = true,
    val isRemoteVideoEnabled: Boolean = false
) {
    /**
     * 切换摄像头
     */
    fun switchCamera() = copy(isFrontCamera = !isFrontCamera)
}