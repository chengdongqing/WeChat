//package top.chengdongqing.wechat.ui.call
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.filter
//import kotlinx.coroutines.launch
//import top.chengdongqing.wechat.core.protocol.MessageDispatcher
//import top.chengdongqing.wechat.data.model.ChatPayload
//
//class CallViewModel(
//    private val dispatcher: MessageDispatcher,
//    private val webRtcManager: IWebRtcManager // 之前建议提取的 WebRTC 接口
//) : ViewModel() {
//
//    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
//    val callState = _callState.asStateFlow()
//
//    fun initCall(targetId: String, isOfferer: Boolean) {
//        viewModelScope.launch {
//            // 1. 启动本地预览
//            webRtcManager.init()
//
//            // 2. 订阅信令流，过滤出当前通话对象的消息
//            launch {
//                dispatcher.signalingFlow
//                    .filter { it.senderId == targetId }
//                    .collect { envelope ->
//                        handleSignaling(envelope.payload)
//                    }
//            }
//
//            // 3. 如果是发起者，创建 Offer
//            if (isOfferer) {
//                webRtcManager.createOffer()
//            }
//        }
//    }
//
//    private fun handleSignaling(payload: ChatPayload) {
//        when (payload) {
//            is ChatPayload.Sdp -> {
//                webRtcManager.handleRemoteSdp(payload.sdp, payload.type)
//            }
//            is ChatPayload.Ice -> {
//                webRtcManager.addRemoteIceCandidate(payload.toWebRtcIceCandidate())
//            }
//            is ChatPayload.CallAction -> {
//                if (payload.action == "HANGUP") {
//                    _callState.value = CallState.Finished
//                }
//            }
//        }
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        webRtcManager.release()
//    }
//}