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
import top.chengdongqing.wechat.features.call.domain.model.CallActions
import top.chengdongqing.wechat.features.call.domain.model.CallState
import top.chengdongqing.wechat.features.call.domain.model.CallType
import top.chengdongqing.wechat.features.call.manager.CallManager
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val callManager: CallManager,
    private val contactDao: ContactDao,
    private val socketManager: SocketManager
) : ViewModel() {

    val state = callManager.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = callManager.state.value
    )

    val eglContext: EglBase.Context get() = callManager.eglBase.eglBaseContext

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

    // ==================== 渲染器绑定 ====================

    /**
     * 绑定本地预览渲染器到 WebRTC 本地视频轨道
     */
    fun bindLocalRenderer(renderer: SurfaceViewRenderer) {
        callManager.setLocalRenderer(renderer)
    }

    fun bindRemoteRenderer(renderer: SurfaceViewRenderer) {
        callManager.setRemoteRenderer(renderer)
    }
}