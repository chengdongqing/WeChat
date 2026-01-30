package top.chengdongqing.wechat.core.call.engine

interface CallEngine {
    fun initialize()

    //    fun startCall(config: CallConfig)
    fun endCall()
    fun setAudioEnabled(enabled: Boolean)
    fun setVideoEnabled(enabled: Boolean)
    fun switchCamera()
    fun isAudioEnabled(): Boolean
    fun isVideoEnabled(): Boolean
}