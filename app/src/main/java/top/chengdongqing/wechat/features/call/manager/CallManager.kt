package top.chengdongqing.wechat.features.call.manager

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.data.database.dao.ContactDao
import top.chengdongqing.wechat.data.database.entity.toDomain
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.features.call.domain.model.CallState
import top.chengdongqing.wechat.features.call.domain.model.CallType
import top.chengdongqing.wechat.features.call.domain.model.CallUiState
import top.chengdongqing.wechat.features.call.domain.model.HangupReason
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通话管理器
 *
 * 完整链路:
 *
 * 【发起通话】
 * 用户点击 → Context.startCall() → CallActivity → CallViewModel.startCall()
 *   → CallManager.startCall() → WebRTC createOffer → SignalingManager.send(Offer)
 *
 * 【收到来电】
 * TCP Packet(SIGNALING) → MessageReceiver → MessageDispatcher.handleSignaling()
 *   → SignalingManager.onSignalingReceived() → CallManager.handleOffer()
 *   → 查联系人信息 → 更新状态 Incoming → CallModule 检测到状态变化
 *   → 启动 CallActivity(来电模式) + 播放铃声 + 显示通知
 */
@Singleton
class CallManager @Inject constructor(
    private val signalingManager: SignalingManager,
    private val webRTCManager: WebRTCManager,
    private val callAudioManager: CallAudioManager,
    private val contactDao: ContactDao
) {
    private companion object {
        const val TAG = "CallManager"
        const val RING_TIMEOUT_MS = 30_000L
    }

    private val _state = MutableStateFlow(CallUiState())
    val state: StateFlow<CallUiState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timeoutJob: Job? = null
    private var timerJob: Job? = null
    private var myUserId: String = ""
    private var isInitialized = false

    // 记录是否是发起方（写通话记录用）
    private var isOutgoing = false

    // ==================== 初始化 ====================

    fun init(myUserId: String) {
        if (isInitialized) return
        isInitialized = true
        this.myUserId = myUserId

        scope.launch {
            signalingManager.incomingSignaling.collect { handleIncomingSignaling(it) }
        }
        scope.launch {
            webRTCManager.iceConnectionState.collect { handleIceStateChange(it) }
        }
        scope.launch {
            webRTCManager.localIceCandidates.collect { sendIceCandidate(it) }
        }

        Log.d(TAG, "CallManager 已初始化, myUserId=$myUserId")
    }

    // ==================== 发起通话 ====================

    fun startCall(peerId: String, peerName: String, peerAvatar: String?, callType: CallType) {
        if (_state.value.callState != CallState.Idle) {
            Log.w(TAG, "当前有通话进行中")
            return
        }

        isOutgoing = true
        val callId = randomUUID()

        _state.value = CallUiState(
            callState = CallState.Outgoing,
            callType = callType,
            callId = callId,
            peerId = peerId,
            peerName = peerName,
            peerAvatar = peerAvatar,
            isOutgoing = isOutgoing
        )

        scope.launch {
            try {
                webRTCManager.initialize()
                webRTCManager.createPeerConnection()
                webRTCManager.startLocalMedia(callType)

                val offer = webRTCManager.createOffer()
                signalingManager.send(
                    targetUserId = peerId,
                    message = ChatProtocol.Signaling.Offer(
                        messageId = callId,
                        senderId = myUserId,
                        callType = callType,
                        sdp = offer.description
                    )
                )

                startTimeout()
            } catch (e: Exception) {
                Log.e(TAG, "发起通话失败", e)
                endCall(HangupReason.Error)
            }
        }
    }

    // ==================== 接听 / 拒接 / 挂断 ====================

    fun accept() {
        if (_state.value.callState != CallState.Incoming) return
        cancelTimeout()
        _state.update {
            it.copy(callState = CallState.Connecting)
        }

        scope.launch {
            try {
                val answer = webRTCManager.createAnswer()
                signalingManager.send(
                    targetUserId = _state.value.peerId,
                    message = ChatProtocol.Signaling.Answer(
                        messageId = _state.value.callId,
                        senderId = myUserId,
                        callType = _state.value.callType,
                        sdp = answer.description
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "接听失败", e)
                endCall(HangupReason.Error)
            }
        }
    }

    fun reject() {
        if (_state.value.callState != CallState.Incoming) return
        scope.launch {
            signalingManager.send(
                targetUserId = _state.value.peerId,
                message = ChatProtocol.Signaling.Hangup(
                    messageId = _state.value.callId,
                    senderId = myUserId,
                    reason = HangupReason.Declined
                )
            )
        }
        endCall(HangupReason.Declined)
    }

    fun hangup() {
        val current = _state.value
        if (current.callState == CallState.Idle || current.callState == CallState.Ended) return
        scope.launch {
            signalingManager.send(
                targetUserId = current.peerId,
                message = ChatProtocol.Signaling.Hangup(
                    messageId = current.callId,
                    senderId = myUserId,
                    reason = HangupReason.Normal
                )
            )
        }
        endCall(HangupReason.Normal)
    }

    // ==================== 通话中控制 ====================

    fun toggleMic() {
        webRTCManager.toggleMute()
        _state.update { it.copy(isMicOn = !it.isMicOn) }
    }

    fun toggleSpeaker() {
        callAudioManager.toggleSpeaker()
        _state.update { it.copy(isSpeakerOn = !it.isSpeakerOn) }
    }

    fun switchCamera() {
        webRTCManager.switchCamera()
        _state.update { it.copy(isFrontCamera = !it.isFrontCamera) }
    }

    fun toggleVideo() {
        webRTCManager.toggleVideo()
        _state.update { it.copy(isVideoOn = !it.isVideoOn) }
    }

    fun setLocalRenderer(renderer: SurfaceViewRenderer) = webRTCManager.setLocalRenderer(renderer)
    fun setRemoteRenderer(renderer: SurfaceViewRenderer) = webRTCManager.setRemoteRenderer(renderer)
    val eglBase get() = webRTCManager.eglBase

    // ==================== 信令处理 ====================

    private suspend fun handleIncomingSignaling(message: ChatProtocol.Signaling) {
        when (message) {
            is ChatProtocol.Signaling.Offer -> handleOffer(message)
            is ChatProtocol.Signaling.Answer -> handleAnswer(message)
            is ChatProtocol.Signaling.IceCandidate -> handleRemoteIce(message)
            is ChatProtocol.Signaling.Hangup -> handleHangup(message)
            is ChatProtocol.Signaling.Busy -> handleBusy(message)
        }
    }

    private suspend fun handleOffer(offer: ChatProtocol.Signaling.Offer) {
        if (_state.value.callState != CallState.Idle) {
            signalingManager.send(
                targetUserId = offer.senderId,
                message = ChatProtocol.Signaling.Busy(
                    messageId = offer.messageId,
                    senderId = myUserId
                )
            )
            return
        }

        isOutgoing = false

        // 查联系人信息
        val contact = withContext(Dispatchers.IO) {
            contactDao.getById(offer.senderId)?.toDomain()
        }
        val peerName = contact?.displayName ?: offer.senderId
        val peerAvatar = contact?.avatarPath

        // 初始化 WebRTC
        webRTCManager.initialize()
        webRTCManager.createPeerConnection()
        webRTCManager.setRemoteDescription(
            SessionDescription(SessionDescription.Type.OFFER, offer.sdp)
        )
        webRTCManager.startLocalMedia(offer.callType)

        // 更新状态为 Incoming（CallModule 监听到后会启动 CallActivity + 播放铃声）
        _state.value = CallUiState(
            callState = CallState.Incoming,
            callType = offer.callType,
            callId = offer.messageId,
            peerId = offer.senderId,
            peerName = peerName,
            peerAvatar = peerAvatar,
            isOutgoing = isOutgoing
        )

        startTimeout()
        Log.d(TAG, "来电: $peerName (${offer.senderId}), type=${offer.callType}")
    }

    private suspend fun handleAnswer(answer: ChatProtocol.Signaling.Answer) {
        if (_state.value.callId != answer.messageId || _state.value.callState != CallState.Outgoing) return
        cancelTimeout()
        _state.update { it.copy(callState = CallState.Connecting) }
        webRTCManager.setRemoteDescription(
            SessionDescription(SessionDescription.Type.ANSWER, answer.sdp)
        )
    }

    private fun handleRemoteIce(ice: ChatProtocol.Signaling.IceCandidate) {
        if (_state.value.callId != ice.messageId) return
        webRTCManager.addIceCandidate(IceCandidate(ice.sdpMid, ice.sdpMLineIndex, ice.candidate))
    }

    private fun handleHangup(hangup: ChatProtocol.Signaling.Hangup) {
        if (_state.value.callId != hangup.messageId) return
        endCall(hangup.reason)
    }

    private fun handleBusy(busy: ChatProtocol.Signaling.Busy) {
        if (_state.value.callId != busy.messageId) return
        endCall(HangupReason.Busy)
    }

    // ==================== ICE ====================

    private fun handleIceStateChange(iceState: PeerConnection.IceConnectionState) {
        when (iceState) {
            PeerConnection.IceConnectionState.CONNECTED,
            PeerConnection.IceConnectionState.COMPLETED -> {
                if (_state.value.callState == CallState.Connecting) {
                    _state.update { it.copy(callState = CallState.Connected) }
                    startTimer()
                }
            }

            PeerConnection.IceConnectionState.DISCONNECTED,
            PeerConnection.IceConnectionState.FAILED -> {
                if (_state.value.callState == CallState.Connected) endCall(HangupReason.Error)
            }

            else -> {}
        }
    }

    private fun sendIceCandidate(candidate: IceCandidate) {
        val current = _state.value
        if (current.callState == CallState.Idle || current.callState == CallState.Ended) return
        scope.launch {
            signalingManager.send(
                current.peerId, ChatProtocol.Signaling.IceCandidate(
                    messageId = current.callId, senderId = myUserId,
                    candidate = candidate.sdp, sdpMid = candidate.sdpMid,
                    sdpMLineIndex = candidate.sdpMLineIndex
                )
            )
        }
    }

    // ==================== 超时 & 计时 ====================

    private fun startTimeout() {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(RING_TIMEOUT_MS)
            signalingManager.send(
                targetUserId = _state.value.peerId,
                message = ChatProtocol.Signaling.Hangup(
                    messageId = _state.value.callId,
                    senderId = myUserId,
                    reason = HangupReason.Timeout
                )
            )
            endCall(HangupReason.Timeout)
        }
    }

    private fun cancelTimeout() {
        timeoutJob?.cancel(); timeoutJob = null
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var seconds = 0
            while (true) {
                delay(1000); seconds++; _state.update { it.copy(duration = seconds) }
            }
        }
    }

    // ==================== 结束通话 ====================

    private fun endCall(reason: HangupReason) {
        cancelTimeout()
        timerJob?.cancel(); timerJob = null
        _state.update { it.copy(callState = CallState.Ended, endReason = reason) }
        webRTCManager.release()
        Log.d(TAG, "通话结束: $reason, 时长=${_state.value.duration}s")

        // TODO: 写通话记录消息 messageRepository.insertCallRecord(...)

        scope.launch { delay(2000); _state.value = CallUiState() }
    }
}