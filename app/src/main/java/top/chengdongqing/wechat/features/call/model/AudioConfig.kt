package top.chengdongqing.wechat.features.call.model

/**
 * 音频配置
 */
data class AudioConfig(
    val isMicEnabled: Boolean = true,
    val isSpeakerEnabled: Boolean = true
) {
    /**
     * 切换麦克风状态
     */
    fun toggleMic() = copy(isMicEnabled = !isMicEnabled)

    /**
     * 切换扬声器状态
     */
    fun toggleSpeaker() = copy(isSpeakerEnabled = !isSpeakerEnabled)
}