package top.chengdongqing.wechat.features.call.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer
import top.chengdongqing.wechat.features.call.manager.CallManager
import top.chengdongqing.wechat.features.call.model.CallActions
import top.chengdongqing.wechat.features.call.model.CallType
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val callManager: CallManager
) : ViewModel() {

    val state = callManager.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = callManager.state.value
    )

    /**
     * 共享 EGL 上下文，用于初始化 SurfaceViewRenderer
     */
    val eglContext: EglBase.Context get() = callManager.eglBase.eglBaseContext

    /**
     * 通话操作集合
     */
    val actions = CallActions(
        onAccept = { callManager.accept() },
        onDecline = { callManager.decline() },
        onCancel = { callManager.cancel() },
        onHangup = { callManager.hangup() },
        onToggleMic = { callManager.toggleMic() },
        onToggleSpeaker = { callManager.toggleSpeaker() },
        onToggleVideo = { callManager.toggleVideo() },
        onSwitchCamera = { callManager.switchCamera() },
        onSwapVideo = { callManager.swapVideo() },
        onToggleControlsVisibility = { callManager.toggleControlsVisibility() },
        onMinimize = {}  // TODO: 画中画模式
    )

    /**
     * 发起通话
     */
    fun startCall(peerId: String, callType: CallType) = callManager.startCall(peerId, callType)

    /** 将本端渲染器绑定到 WebRTC 本地视频轨道 */
    fun bindLocalRenderer(renderer: SurfaceViewRenderer) = callManager.setLocalRenderer(renderer)

    /** 将远端渲染器绑定到 WebRTC 远端视频轨道 */
    fun bindRemoteRenderer(renderer: SurfaceViewRenderer) = callManager.setRemoteRenderer(renderer)

    /** 重新采集本地视频 */
    fun restartVideoCapture() = callManager.restartVideoCapture()

    override fun onCleared() {
        super.onCleared()

        // 重置状态
        callManager.reset()
    }
}