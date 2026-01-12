package top.chengdongqing.wechat.data.webrtc

//interface IWebRtcManager {
//    // --- 状态监听 ---
//    fun setEventListener(listener: WebRtcEventListener)
//
//    // --- 核心控制 ---
//    fun init(eglContext: EglBase.Context)
//    fun startLocalPreview(localSink: VideoSink)
//    fun stopCall()
//    fun release()
//
//    // --- 信令对接 ---
//    fun createOffer()
//    fun handleRemoteSdp(sdp: String, type: String)
//    fun addRemoteIceCandidate(candidate: IceCandidate)
//
//    // --- 媒体设置 ---
//    fun toggleMic(mute: Boolean)
//    fun toggleCamera(pause: Boolean)
//    fun switchCamera()
//
//    // --- 属性暴露 ---
//    val isConnected: Boolean
//    val localStream: MediaStream?
//}