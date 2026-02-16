package top.chengdongqing.wechat.features.call.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer
import top.chengdongqing.wechat.data.database.dao.ContactDao
import top.chengdongqing.wechat.data.network.socket.SocketManager
import top.chengdongqing.wechat.features.call.data.CallActions
import top.chengdongqing.wechat.features.call.data.CallState
import top.chengdongqing.wechat.features.call.manager.CallManager
import top.chengdongqing.wechat.features.chat.domain.model.CallType
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val callManager: CallManager,
    private val contactDao: ContactDao,
    private val socketManager: SocketManager
) : ViewModel() {

    val state = callManager.state.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), callManager.state.value
    )

    val eglContext: EglBase.Context get() = callManager.eglBase.eglBaseContext

    /**
     * ★ 修复: onReject 和 onHangup 区分
     * - Incoming 状态 → onReject 调 reject()
     * - 其他状态 → onHangup 调 hangup()
     *
     * CallControlBar 里的挂断按钮统一调 onHangup，
     * 来电界面的拒绝按钮调 onReject。
     */
    val actions = CallActions(
        onAccept = { callManager.accept() },
        onReject = { callManager.reject() },
        onHangup = { callManager.hangup() },
        onToggleMic = { callManager.toggleMic() },
        onToggleSpeaker = { callManager.toggleSpeaker() },
        onSwitchCamera = { callManager.switchCamera() },
        onToggleVideo = { callManager.toggleVideo() },
        onMinimize = {}  // TODO: 画中画
    )

    // ==================== 发起通话 ====================

    fun startCall(peerId: String, callType: CallType) {
        if (state.value.callState != CallState.Idle) return

        viewModelScope.launch(Dispatchers.IO) {
            if (!socketManager.isConnected(peerId)) {
                // TODO: 提示对方不在线
                return@launch
            }

            val contact = contactDao.getById(peerId)
            callManager.startCall(
                peerId = peerId,
                peerName = contact?.remarkName ?: contact?.nickname ?: peerId,
                peerAvatar = contact?.avatarPath,
                callType = callType
            )
        }
    }

    // ==================== ★ 渲染器绑定 ====================

    /**
     * 绑定本地预览渲染器到 WebRTC 本地视频轨道
     *
     * 参考代码里的关键步骤:
     * webRtcManager.initVideoViews(localRenderer, remoteRenderer)
     *
     * 我们拆成两个方法，因为 Compose 里本地和远端是独立的组件。
     */
    fun bindLocalRenderer(renderer: SurfaceViewRenderer) {
        callManager.setLocalRenderer(renderer)
    }

    fun bindRemoteRenderer(renderer: SurfaceViewRenderer) {
        callManager.setRemoteRenderer(renderer)
    }
}