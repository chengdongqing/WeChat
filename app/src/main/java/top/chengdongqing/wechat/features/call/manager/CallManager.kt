package top.chengdongqing.wechat.features.call.manager

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.data.network.messaging.MessageDispatcher
import top.chengdongqing.wechat.data.network.messaging.MessageSender
import top.chengdongqing.wechat.data.network.protocol.ChatProtocol
import top.chengdongqing.wechat.data.network.service.modules.CallModule
import top.chengdongqing.wechat.features.call.model.CallState
import top.chengdongqing.wechat.features.call.model.CallStatus
import top.chengdongqing.wechat.features.call.model.CallType
import top.chengdongqing.wechat.features.call.model.CallUiState
import top.chengdongqing.wechat.features.call.model.HangupReason
import top.chengdongqing.wechat.features.call.model.HangupResult
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.domain.repository.MessageRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactRepository
import top.chengdongqing.wechat.features.settings.domain.model.RingtoneSound
import top.chengdongqing.wechat.features.settings.domain.repository.NotificationSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通话管理器
 *
 * 完整调用链路：
 *
 * 【发起通话】
 * 用户点击 → CallViewModel.startCall()
 *   → ensureConnected → WebRTC createOffer → SignalingManager.send(Offer)
 *   → 等待对方 Answer → ICE 连接建立 → CallState.Connected
 *
 * 【收到来电】
 * TCP Packet(SIGNALING) → MessageReceiver → MessageDispatcher
 *   → SignalingManager.onSignalingReceived() → handleOffer()
 *   → CallState.Incoming → CallModule 启动 CallActivity + 铃声 + 通知
 *   → 用户接听 → accept() → createAnswer → ICE 连接建立 → CallState.Connected
 *
 * 【通话结束】
 * hangup/decline/cancel/timeout/ICE 失败 → endCall()
 *   → 写通话记录 → delay(3s) → 重置状态为 Idle
 */
@Singleton
class CallManager @Inject constructor(
    private val signalingManager: SignalingManager,
    private val messageSender: MessageSender,
    private val webRTCManager: WebRTCManager,
    private val callAudioManager: CallAudioManager,
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
    private val messageDispatcher: MessageDispatcher,
    private val notificationRepository: NotificationSettingsRepository
) {
    private companion object {
        const val TAG = "CallManager"
        const val RING_TIMEOUT_MS = 30_000L  // 无应答超时，30s 后自动挂断
        const val CONNECT_TIMEOUT_MS = 15_000L // 连接中持续15s自动挂断
    }

    private val _state = MutableStateFlow(CallUiState())
    val state = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timeoutJob: Job? = null
    private var timerJob: Job? = null
    private var myUserId = ""
    private var isInitialized = false

    /** 是否为发起方，影响通话记录的写入方式 */
    private var isOutgoing = false

    // 有序信令队列
    private val signalingChannel = Channel<ChatProtocol.Signaling>(capacity = 64)

    // ==================== 初始化 ====================

    /**
     * 初始化 CallManager，幂等（重复调用无效）
     *
     * 订阅信令流、ICE 状态流和本端候选流，三个订阅贯穿整个通话生命周期。
     */
    fun init(myUserId: String) {
        if (isInitialized) return
        isInitialized = true
        this.myUserId = myUserId

        scope.launch {
            signalingManager.incomingSignaling.collect { msg ->
                when (msg) {
                    is ChatProtocol.Signaling.IceCandidate -> launch { handleRemoteIce(msg) }
                    else -> signalingChannel.send(msg)
                }
            }
        }

        // 顺序消费有序信令
        scope.launch {
            for (msg in signalingChannel) {
                handleIncomingSignaling(msg)
            }
        }

        scope.launch { webRTCManager.iceConnectionState.collect { handleIceStateChange(it) } }
        scope.launch { webRTCManager.localIceCandidates.collect { sendIceCandidate(it) } }
    }

    // ==================== 发起通话 ====================

    /**
     * 发起通话
     *
     * 当前有通话进行中时忽略。
     * 流程：ensureConnected → WebRTC 初始化 → createOffer → 发送 Offer → 启动超时计时
     */
    fun startCall(peerId: String, peerName: String, peerAvatar: String?, callType: CallType) {
        if (!_state.value.callState.isTerminal) return

        isOutgoing = true
        val callId = randomUUID()

        _state.value = CallUiState(
            callState = CallState.Outgoing,
            callType = callType,
            callId = callId,
            peerId = peerId,
            peerName = peerName,
            peerAvatar = peerAvatar,
            isOutgoing = true,
            isSpeakerOn = callType.isVideoCall
        )

        scope.launch {
            runCatching { messageSender.ensureConnected(peerId, myUserId) }
                .onFailure { endCall(HangupReason.Offline); return@launch }

            runCatching {
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
            }.onFailure {
                Log.e(TAG, "发起通话失败", it)
                endCall(HangupReason.Error)
            }
        }
    }

    // ==================== 接听 / 拒接 / 取消 / 挂断 ====================

    /**
     * 接听来电
     *
     * 取消超时计时，切换为 Connecting，发送 Answer。
     */
    fun accept() {
        if (_state.value.callState != CallState.Incoming) return
        cancelTimeout()
        _state.update { it.copy(callState = CallState.Connecting) }

        scope.launch {
            runCatching {
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
            }.onFailure {
                Log.e(TAG, "接听失败", it)
                endCall(HangupReason.Error)
            }
        }
    }

    /** 拒接来电，发送 Hangup(Declined) 给对方 */
    fun decline() {
        if (_state.value.callState != CallState.Incoming) return
        sendHangup(HangupReason.Declined)
        endCall(HangupReason.Declined)
    }

    /** 取消去电（对方未接听时），发送 Hangup(Cancelled) 给对方 */
    fun cancel() {
        val s = _state.value.callState
        if (s != CallState.Outgoing && s != CallState.Connecting) return
        sendHangup(HangupReason.Cancelled)
        endCall(HangupReason.Cancelled)
    }

    /** 主动挂断通话，发送 Hangup(Normal) 给对方 */
    fun hangup() {
        val state = _state.value
        if (state.callState == CallState.Idle || state.callState == CallState.Ended) return
        sendHangup(HangupReason.Normal)
        endCall(HangupReason.Normal, isFromMe = true)
    }

    /** 发送挂断信令给对方 */
    private fun sendHangup(reason: HangupReason) {
        val state = _state.value
        scope.launch {
            signalingManager.send(
                targetUserId = state.peerId,
                message = ChatProtocol.Signaling.Hangup(
                    messageId = state.callId,
                    senderId = myUserId,
                    reason = reason,
                    duration = state.duration
                )
            )
        }
    }

    // ==================== 通话中控制 ====================

    /** 切换麦克风静音并同步媒体状态给对方 */
    fun toggleMic() {
        webRTCManager.toggleMute()
        _state.update { it.copy(isMicOn = !it.isMicOn) }
        sendMediaState()
    }

    /** 切换免提并同步媒体状态给对方 */
    fun toggleSpeaker() {
        callAudioManager.toggleSpeaker()
        _state.update { it.copy(isSpeakerOn = !it.isSpeakerOn) }
        sendMediaState()
    }

    /**
     * 切换摄像头开关并同步媒体状态
     *
     * 视频轨道未创建时（如权限延迟）先启动采集再开启；否则直接切换轨道 enabled 状态。
     */
    fun toggleVideo() {
        val current = _state.value
        if (current.callType == CallType.Video && webRTCManager.isLocalVideoTrackNull()) {
            scope.launch {
                webRTCManager.startLocalMedia(current.callType)
                _state.update { it.copy(isVideoOn = true) }
                sendMediaState()
            }
        } else {
            _state.update { it.copy(isVideoOn = webRTCManager.toggleVideo()) }
            sendMediaState()
        }
    }

    /** 将本端媒体状态（视频/麦克风/免提）同步给对方 */
    private fun sendMediaState() {
        val current = _state.value
        if (current.callState == CallState.Idle || current.callState == CallState.Ended) return
        scope.launch {
            signalingManager.send(
                targetUserId = current.peerId,
                message = ChatProtocol.Signaling.MediaState(
                    messageId = current.callId,
                    senderId = myUserId,
                    isVideoOn = current.isVideoOn,
                    isMicOn = current.isMicOn,
                    isSpeakerOn = current.isSpeakerOn
                )
            )
        }
    }

    fun switchCamera() {
        webRTCManager.switchCamera()
        _state.update { it.copy(isFrontCamera = !it.isFrontCamera) }
    }

    fun swapVideo() {
        webRTCManager.swapRenderers()
        _state.update { it.copy(isVideoSwapped = !it.isVideoSwapped) }
    }

    fun toggleControlsVisibility() {
        _state.update { it.copy(isControlsVisible = !it.isControlsVisible) }
    }

    fun setLocalRenderer(renderer: SurfaceViewRenderer) = webRTCManager.setLocalRenderer(renderer)
    fun setRemoteRenderer(renderer: SurfaceViewRenderer) = webRTCManager.setRemoteRenderer(renderer)
    fun restartVideoCapture() = webRTCManager.restartVideoCapture()
    val eglBase get() = webRTCManager.eglBase

    // ==================== 信令处理 ====================

    private suspend fun handleIncomingSignaling(message: ChatProtocol.Signaling) {
        when (message) {
            is ChatProtocol.Signaling.Offer -> handleOffer(message)
            is ChatProtocol.Signaling.Answer -> handleAnswer(message)
            is ChatProtocol.Signaling.IceCandidate -> handleRemoteIce(message)
            is ChatProtocol.Signaling.Hangup -> handleHangup(message)
            is ChatProtocol.Signaling.Busy -> handleBusy(message)
            is ChatProtocol.Signaling.MediaState -> handleMediaState(message)
            is ChatProtocol.Signaling.RingtoneInfo -> handleRingtoneInfo(message)
        }
    }

    /**
     * 处理来电 Offer
     *
     * 忙线时直接回复 Busy。
     * 否则：查联系人信息 → 初始化 WebRTC → 设置远端 SDP → 更新状态为 Incoming。
     * [CallModule] 监听到 Incoming 后自动启动 CallActivity + 铃声 + 通知。
     */
    private suspend fun handleOffer(offer: ChatProtocol.Signaling.Offer) {
        if (!_state.value.callState.isTerminal) {
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

        val contact = contactRepository.getContact(offer.senderId)

        webRTCManager.initialize()
        webRTCManager.createPeerConnection()
        webRTCManager.setRemoteDescription(
            SessionDescription(
                SessionDescription.Type.OFFER,
                offer.sdp
            )
        )
        webRTCManager.startLocalMedia(offer.callType)

        _state.value = CallUiState(
            callState = CallState.Incoming,
            callType = offer.callType,
            callId = offer.messageId,
            peerId = offer.senderId,
            peerName = contact?.displayName ?: offer.senderId,
            peerAvatar = contact?.avatarPath,
            isOutgoing = false,
            isSpeakerOn = offer.callType.isVideoCall
        )

        startTimeout()

        // 告知对方我的铃声设置
        signalingManager.send(
            targetUserId = offer.senderId,
            message = ChatProtocol.Signaling.RingtoneInfo(
                messageId = offer.messageId,
                senderId = myUserId,
                ringtone = if (ringtoneAudibleEnabled()) {
                    myRingtone()
                } else {
                    RingtoneSound.Default
                }
            )
        )
    }

    /** 收到 Answer，设置远端 SDP，切换为 Connecting 等待 ICE 建立 */
    private suspend fun handleAnswer(answer: ChatProtocol.Signaling.Answer) {
        if (_state.value.callId != answer.messageId || _state.value.callState != CallState.Outgoing) return

        cancelTimeout()
        _state.update { it.copy(callState = CallState.Connecting) }
        startTimeout(CONNECT_TIMEOUT_MS)   // 15s ICE 连接超时

        webRTCManager.setRemoteDescription(
            SessionDescription(
                SessionDescription.Type.ANSWER,
                answer.sdp
            )
        )
    }

    /** 添加对方的 ICE 候选 */
    private fun handleRemoteIce(ice: ChatProtocol.Signaling.IceCandidate) {
        if (_state.value.callId != ice.messageId) return
        webRTCManager.addIceCandidate(IceCandidate(ice.sdpMid, ice.sdpMLineIndex, ice.candidate))
    }

    private fun handleHangup(hangup: ChatProtocol.Signaling.Hangup) {
        if (_state.value.callId != hangup.messageId) return
        endCall(hangup.reason, duration = hangup.duration)
    }

    private fun handleBusy(busy: ChatProtocol.Signaling.Busy) {
        if (_state.value.callId != busy.messageId) return
        endCall(HangupReason.Busy)
    }

    /** 更新对方的媒体状态（视频/麦克风/免提） */
    private fun handleMediaState(state: ChatProtocol.Signaling.MediaState) {
        if (_state.value.callId != state.messageId) return
        _state.update {
            it.copy(
                isPeerVideoOn = state.isVideoOn,
                isPeerMicOn = state.isMicOn,
                isPeerSpeakerOn = state.isSpeakerOn
            )
        }
    }

    private fun handleRingtoneInfo(info: ChatProtocol.Signaling.RingtoneInfo) {
        if (_state.value.callId != info.messageId) return
        if (_state.value.callState != CallState.Outgoing) return

        scope.launch {
            callAudioManager.startRingtone(
                isIncoming = false,
                ringtone = info.ringtone
            )
        }
    }

    // ==================== ICE ====================

    /**
     * 处理 ICE 连接状态变化
     *
     * CONNECTED/COMPLETED → 切换为 Connected，启动通话计时
     * DISCONNECTED/FAILED → 通话中断，结束通话
     */
    private fun handleIceStateChange(iceState: PeerConnection.IceConnectionState) {
        when (iceState) {
            PeerConnection.IceConnectionState.CONNECTED,
            PeerConnection.IceConnectionState.COMPLETED -> {
                if (_state.value.callState == CallState.Connecting) {
                    _state.update { it.copy(callState = CallState.Connected) }
                    startTimer()
                }
            }

            PeerConnection.IceConnectionState.DISCONNECTED -> {
                Log.d(TAG, "ICE 断开，等待信令处理...")
            }

            PeerConnection.IceConnectionState.FAILED -> {
                val current = _state.value.callState
                if (current == CallState.Connected || current == CallState.Connecting) {
                    endCall(HangupReason.Error)
                }
            }

            else -> {}
        }
    }

    /** 将本端 ICE 候选发送给对方 */
    private fun sendIceCandidate(candidate: IceCandidate) {
        val current = _state.value
        if (current.callState == CallState.Idle || current.callState == CallState.Ended) return
        scope.launch {
            signalingManager.send(
                current.peerId,
                ChatProtocol.Signaling.IceCandidate(
                    messageId = current.callId,
                    senderId = myUserId,
                    candidate = candidate.sdp,
                    sdpMid = candidate.sdpMid,
                    sdpMLineIndex = candidate.sdpMLineIndex
                )
            )
        }
    }

    // ==================== 超时 & 计时 ====================

    /** 启动无应答超时，30s 后自动发 Hangup(Timeout) 并结束通话 */
    private fun startTimeout(timeout: Long = RING_TIMEOUT_MS) {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(timeout)

            val reason = if (timeout == RING_TIMEOUT_MS) {
                HangupReason.Timeout
            } else {
                HangupReason.Error
            }
            sendHangup(reason)
            endCall(reason)
        }
    }

    private fun cancelTimeout() {
        timeoutJob?.cancel()
        timeoutJob = null
    }

    /** 通话接通后启动秒级计时，每秒更新 duration */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var seconds = 0L
            while (true) {
                delay(1000)
                _state.update { it.copy(duration = ++seconds) }
            }
        }
    }

    // ==================== 结束通话 ====================

    /**
     * 结束通话
     *
     * 1. 取消超时和计时
     * 2. 更新状态为 Ended，释放 WebRTC 资源
     * 3. 写通话记录（发起方走 MessageRepository，接收方走 MessageDispatcher）
     * 4. 延迟 3s 重置状态为 Idle（给 UI 展示结果的时间）
     */
    private fun endCall(reason: HangupReason, isFromMe: Boolean = false, duration: Long? = null) {
        if (_state.value.callState.isTerminal) {
            return
        }

        // 取消计时器
        cancelTimeout()
        // 更新通话状态
        val snapshot = _state.value
        _state.update {
            it.copy(
                callState = CallState.Ended,
                hangupResult = HangupResult(reason, isFromMe)
            )
        }
        // 释放 WebRTC 相关资源
        webRTCManager.release()

        scope.launch {
            // 保存通话记录
            saveCallRecord(snapshot, reason, duration)
        }
    }

    /**
     * 写通话记录
     *
     * 发起方：通过 [MessageRepository] 发送通话消息（走正常发消息流程）
     * 接收方：通过 [MessageDispatcher] 直接入库（对方的通话记录由对方发，我方本地生成）
     */
    private suspend fun saveCallRecord(
        snapshot: CallUiState,
        reason: HangupReason,
        duration: Long? = null
    ) {
        val status = reason.toCallStatus()
        if (isOutgoing) {
            messageRepository.sendMessage(
                sessionId = snapshot.peerId,
                receiverId = snapshot.peerId,
                messageId = snapshot.callId,
                content = MessageContent.Call(
                    type = snapshot.callType,
                    status = status,
                    duration = duration ?: snapshot.duration
                )
            )
        } else {
            messageDispatcher.dispatch(
                ChatProtocol.CallMessage(
                    messageId = snapshot.callId,
                    senderId = snapshot.peerId,
                    receiverId = myUserId,
                    status = status.name,
                    duration = duration ?: snapshot.duration,
                    callType = snapshot.callType,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun reset() {
        if (_state.value.callState.isTerminal) {
            _state.value = CallUiState()
        }
    }

    private suspend fun myRingtone(): RingtoneSound =
        notificationRepository.ringtone.first()

    private suspend fun ringtoneAudibleEnabled(): Boolean =
        notificationRepository.ringtoneAudibleEnabled.first()
}

/** 将挂断原因映射为通话记录状态 */
fun HangupReason.toCallStatus() = when (this) {
    HangupReason.Normal -> CallStatus.Finished
    HangupReason.Declined -> CallStatus.Declined
    HangupReason.Cancelled -> CallStatus.Cancelled
    HangupReason.Timeout -> CallStatus.Missed
    HangupReason.Busy,
    HangupReason.Offline,
    HangupReason.Error -> CallStatus.Failed
}