package top.chengdongqing.wechat.features.call.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer
import top.chengdongqing.wechat.features.call.manager.CallManager
import top.chengdongqing.wechat.features.call.model.CallActions
import top.chengdongqing.wechat.features.call.model.CallType
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import javax.inject.Inject

/**
 * 通话 ViewModel
 *
 * 薄层桥接：将 [CallManager] 的状态和操作暴露给 UI，不含业务逻辑。
 * [actions] 在构造时一次性创建，避免每次重组重新分配 lambda。
 */
@HiltViewModel
class CallViewModel @Inject constructor(
    private val callManager: CallManager,
    private val contactRepository: ContactRepository
) : ViewModel() {

    val state = callManager.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = callManager.state.value
    )

    /** 共享 EGL 上下文，传给 [WebRtcVideoView] 初始化 SurfaceViewRenderer */
    val eglContext: EglBase.Context get() = callManager.eglBase.eglBaseContext

    /** 通话操作集合，构造时固定，UI 直接持有引用无需每帧重建 */
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
     *
     * 从数据库查询联系人显示名和头像，优先用备注名，均无时降级用 peerId。
     * 已有通话进行中时忽略（状态机保护）。
     */
    fun startCall(peerId: String, callType: CallType) {
        viewModelScope.launch {
            val contact = contactRepository.getContact(peerId)
            callManager.startCall(
                peerId = peerId,
                peerName = contact?.displayName ?: peerId,
                peerAvatar = contact?.avatarPath,
                callType = callType
            )
        }
    }

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