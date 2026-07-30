package top.chengdongqing.wechat.feature.call.manager

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
import top.chengdongqing.wechat.core.common.call.CallStatus
import top.chengdongqing.wechat.core.common.di.MainScope
import top.chengdongqing.wechat.core.common.media.RingtoneSound
import top.chengdongqing.wechat.core.data.model.ChatProtocol
import top.chengdongqing.wechat.core.data.model.ConnectionMode
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.data.repository.ContactRepository
import top.chengdongqing.wechat.core.data.repository.MessageRepository
import top.chengdongqing.wechat.core.data.repository.NotificationSettingsRepository
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.core.model.CallState
import top.chengdongqing.wechat.core.model.CallType
import top.chengdongqing.wechat.core.model.HangupReason
import top.chengdongqing.wechat.core.model.HangupResult
import top.chengdongqing.wechat.core.model.SendError
import top.chengdongqing.wechat.core.network.connection.ChatTransportManager
import top.chengdongqing.wechat.core.network.connection.ConnectionException
import top.chengdongqing.wechat.core.network.connection.wifi.TcpSocketClient
import top.chengdongqing.wechat.core.network.crypto.PacketSigner
import top.chengdongqing.wechat.core.network.messaging.MessageDispatcher
import top.chengdongqing.wechat.core.network.security.KeyStoreManager
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.feature.call.domain.model.CallUiState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通话超时场景
 */
private enum class TimeoutScenario {
    /** 等待对方接听 */
    Ringing,

    /** 等待 ICE 建立连接 */
    Connecting
}

/**
 * 通话管理器
 */
@Singleton
class CallManager @Inject constructor(
    private val signalingManager: SignalingManager,
    private val webRTCManager: WebRTCManager,
    private val callAudioManager: CallAudioManager,
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
    private val messageDispatcher: MessageDispatcher,
    private val transport: ChatTransportManager,
    private val connectionInfoDao: ConnectionInfoDao,
    private val socketClient: TcpSocketClient,
    private val packetSigner: PacketSigner,
    private val keyStoreManager: KeyStoreManager,
    private val profileRepository: ProfileRepository,
    private val notificationRepository: NotificationSettingsRepository,
    @param:MainScope private val scope: CoroutineScope
) {
    private companion object {
        const val TAG = "CallManager"

        const val RING_TIMEOUT_MS = 30_000L // 无应答超时：30s 后自动挂断
        const val CONNECT_TIMEOUT_MS = 15_000L // ICE 连接超时：15s 后自动挂断
    }

    private val _state = MutableStateFlow(CallUiState())
    val state = _state.asStateFlow()

    private var isOutgoing = false
    private var isInitialized = false
    private var timeoutJob: Job? = null
    private var durationTimerJob: Job? = null
    private val myUserId: String get() = profileRepository.requireUserId()
    private val signalingChannel = Channel<ChatProtocol.Signaling>(capacity = 64)

    /**
     * 初始化，只执行一次
     */
    fun init() {
        if (isInitialized) return
        isInitialized = true

        // 监听信令
        scope.launch {
            signalingManager.incomingSignaling.collect { msg ->
                when (msg) {
                    // 除了ICE，其他信令都通过队列消费
                    is ChatProtocol.Signaling.IceCandidate -> launch { handleRemoteIce(msg) }
                    else -> signalingChannel.send(msg)
                }
            }
        }
        // 有序消息信令
        scope.launch {
            for (msg in signalingChannel) {
                handleIncomingSignaling(msg)
            }
        }
        scope.launch { webRTCManager.iceConnectionState.collect { handleIceStateChange(it) } }
        scope.launch { webRTCManager.localIceCandidates.collect { sendIceCandidate(it) } }
    }

    /**
     * 发起通话
     */
    fun startCall(peerId: String, callType: CallType) {
        if (!_state.value.callState.isTerminal) return

        isOutgoing = true

        scope.launch {
            val callId = randomUUID()
            val contact = contactRepository.getContact(peerId)
            _state.value = CallUiState(
                callState = CallState.Outgoing,
                callType = callType,
                callId = callId,
                peerId = peerId,
                peerName = contact?.displayName ?: "",
                peerAvatar = contact?.avatarPath,
                isOutgoing = true,
                isSpeakerOn = callType.isVideoCall
            )

            runCatching {
                ensureConnected(peerId, myUserId)
            }.onFailure {
                endCall(HangupReason.Offline)
                return@launch
            }

            runCatching {
                webRTCManager.initialize()
                webRTCManager.createPeerConnection()
                webRTCManager.startLocalMedia(callType)

                val offer = webRTCManager.createOffer()
                sendSignaling(
                    targetUserId = peerId,
                    packet = ChatProtocol.Signaling.Offer(
                        messageId = callId,
                        senderId = myUserId,
                        callType = callType,
                        sdp = offer.description,
                        signature = ""
                    )
                )

                startTimeout(TimeoutScenario.Ringing)
            }.onFailure {
                Log.e(TAG, "发起通话失败", it)
                endCall(HangupReason.Error)
            }
        }
    }

    /**
     * 确保与目标用户有可用连接
     */
    private suspend fun ensureConnected(targetUserId: String, myUserId: String) {
        if (transport.isConnected(targetUserId)) return
        if (transport.mode.value != ConnectionMode.WiFiLan) return

        val info = connectionInfoDao.getById(targetUserId)
        val ip = info?.lanIpAddress ?: return
        val port = info.lanPort ?: return

        socketClient.connect(
            userId = targetUserId,
            host = ip,
            port = port,
            myUserId = myUserId
        ).getOrElse {
            throw ConnectionException("连接失败: $targetUserId", SendError.ConnectionFailed)
        }
    }

    /**
     * 接听来电
     */
    fun accept() {
        if (_state.value.callState != CallState.Incoming) return
        cancelTimeout()
        _state.update { it.copy(callState = CallState.Connecting) }

        scope.launch {
            runCatching {
                val answer = webRTCManager.createAnswer()
                sendSignaling(
                    targetUserId = _state.value.peerId,
                    packet = ChatProtocol.Signaling.Answer(
                        messageId = _state.value.callId,
                        senderId = myUserId,
                        callType = _state.value.callType,
                        sdp = answer.description,
                        signature = ""
                    )
                )
            }.onFailure {
                Log.e(TAG, "接听失败", it)
                endCall(HangupReason.Error)
            }
        }
    }

    /**
     * 拒接来电
     */
    fun decline() {
        if (_state.value.callState != CallState.Incoming) return
        sendHangup(HangupReason.Declined)
        endCall(HangupReason.Declined)
    }

    /**
     * 取消去电（对方未接听时）
     */
    fun cancel() {
        val s = _state.value.callState
        if (s != CallState.Outgoing && s != CallState.Connecting) return
        sendHangup(HangupReason.Cancelled)
        endCall(HangupReason.Cancelled)
    }

    /**
     * 挂断通话
     */
    fun hangup() {
        val state = _state.value
        if (state.callState == CallState.Idle || state.callState == CallState.Ended) return
        sendHangup(HangupReason.Normal)
        endCall(HangupReason.Normal, isFromMe = true)
    }

    /**
     * 签名并发送信令，统一处理签名+发送逻辑
     */
    private suspend fun sendSignaling(
        targetUserId: String,
        packet: ChatProtocol.Signaling
    ) {
        val signature = packetSigner.sign(packet, keyStoreManager.getPrivateKey())
        signalingManager.send(
            targetUserId = targetUserId,
            message = packet.withSignature(signature)
        )
    }

    /**
     * 发送挂断信令给对方
     */
    private fun sendHangup(reason: HangupReason) {
        val state = _state.value
        scope.launch {
            runCatching {
                sendSignaling(
                    targetUserId = state.peerId,
                    packet = ChatProtocol.Signaling.Hangup(
                        messageId = state.callId,
                        senderId = myUserId,
                        reason = reason,
                        duration = state.duration,
                        signature = ""
                    )
                )
            }
        }
    }

    /**
     * 开关麦克风
     */
    fun toggleMic() {
        webRTCManager.toggleMute()
        _state.update { it.copy(isMicOn = !it.isMicOn) }
        sendMediaState()
    }

    /**
     * 开关免提
     */
    fun toggleSpeaker() {
        callAudioManager.toggleSpeaker()
        _state.update { it.copy(isSpeakerOn = !it.isSpeakerOn) }
        sendMediaState()
    }

    /**
     * 切换摄像头开关并同步媒体状态
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

    /**
     * 将本端媒体状态（视频/麦克风/免提）同步给对方
     */
    private fun sendMediaState() {
        val current = _state.value
        if (current.callState == CallState.Idle || current.callState == CallState.Ended) return

        scope.launch {
            runCatching {
                sendSignaling(
                    targetUserId = current.peerId,
                    packet = ChatProtocol.Signaling.MediaState(
                        messageId = current.callId,
                        senderId = myUserId,
                        isVideoOn = current.isVideoOn,
                        isMicOn = current.isMicOn,
                        isSpeakerOn = current.isSpeakerOn,
                        signature = ""
                    )
                )
            }
        }
    }

    /**
     * 切换前/后摄像头
     */
    fun switchCamera() {
        webRTCManager.switchCamera()
        _state.update { it.copy(isFrontCamera = !it.isFrontCamera) }
    }

    /**
     * 切换主视频为对方/我方
     */
    fun swapVideo() {
        webRTCManager.swapRenderers()
        _state.update { it.copy(isVideoSwapped = !it.isVideoSwapped) }
    }

    /**
     * 进入/退出沉浸式
     */
    fun toggleControlsVisibility() {
        _state.update { it.copy(isControlsVisible = !it.isControlsVisible) }
    }

    fun setLocalRenderer(renderer: SurfaceViewRenderer) = webRTCManager.setLocalRenderer(renderer)
    fun setRemoteRenderer(renderer: SurfaceViewRenderer) = webRTCManager.setRemoteRenderer(renderer)
    fun restartVideoCapture() = webRTCManager.restartVideoCapture()
    val eglBase get() = webRTCManager.eglBase

    /**
     * 分发收到的信令消息
     */
    private suspend fun handleIncomingSignaling(message: ChatProtocol.Signaling) {
        when (message) {
            is ChatProtocol.Signaling.Offer -> handleOffer(message)
            is ChatProtocol.Signaling.Answer -> handleAnswer(message)
            is ChatProtocol.Signaling.Hangup -> handleHangup(message)
            is ChatProtocol.Signaling.Busy -> handleBusy(message)
            is ChatProtocol.Signaling.MediaState -> handleMediaState(message)
            is ChatProtocol.Signaling.RingtoneInfo -> handleRingtoneInfo(message)
            else -> {}
        }
    }

    /**
     * 处理来电 Offer：
     * - 若当前正在通话则回复 Busy；
     * - 否则初始化 WebRTC、更新 UI、启动振铃倒计时
     */
    private suspend fun handleOffer(offer: ChatProtocol.Signaling.Offer) {
        if (!_state.value.callState.isTerminal) {
            runCatching {
                sendSignaling(
                    targetUserId = offer.senderId,
                    packet = ChatProtocol.Signaling.Busy(
                        messageId = offer.messageId,
                        senderId = myUserId,
                        signature = ""
                    )
                )
            }
            return
        }

        isOutgoing = false

        webRTCManager.initialize()
        webRTCManager.createPeerConnection()
        webRTCManager.setRemoteDescription(
            SessionDescription(SessionDescription.Type.OFFER, offer.sdp)
        )
        webRTCManager.startLocalMedia(offer.callType)

        val contact = contactRepository.getContact(offer.senderId)
        _state.value = CallUiState(
            callState = CallState.Incoming,
            callType = offer.callType,
            callId = offer.messageId,
            peerId = offer.senderId,
            peerName = contact?.displayName ?: "",
            peerAvatar = contact?.avatarPath,
            isOutgoing = false,
            isSpeakerOn = offer.callType.isVideoCall
        )

        startTimeout(TimeoutScenario.Ringing)

        // 告知对方我的铃声设置
        runCatching {
            sendSignaling(
                targetUserId = offer.senderId,
                packet = ChatProtocol.Signaling.RingtoneInfo(
                    messageId = offer.messageId,
                    senderId = myUserId,
                    ringtone = if (ringtoneAudibleEnabled()) myRingtone() else RingtoneSound.Default,
                    signature = ""
                )
            )
        }
    }

    /**
     * 收到 Answer：设置远端 SDP，切换为 Connecting 并启动 ICE 连接超时
     */
    private suspend fun handleAnswer(answer: ChatProtocol.Signaling.Answer) {
        if (_state.value.callId != answer.messageId || _state.value.callState != CallState.Outgoing) return

        cancelTimeout()
        _state.update { it.copy(callState = CallState.Connecting) }
        startTimeout(TimeoutScenario.Connecting)

        webRTCManager.setRemoteDescription(
            SessionDescription(SessionDescription.Type.ANSWER, answer.sdp)
        )
    }

    /**
     * 添加对方的 ICE 候选
     */
    private fun handleRemoteIce(ice: ChatProtocol.Signaling.IceCandidate) {
        if (_state.value.callId != ice.messageId) return
        webRTCManager.addIceCandidate(IceCandidate(ice.sdpMid, ice.sdpMLineIndex, ice.candidate))
    }

    /**
     * 处理对方挂断
     */
    private fun handleHangup(hangup: ChatProtocol.Signaling.Hangup) {
        if (_state.value.callId != hangup.messageId) return
        endCall(reason = hangup.reason, duration = hangup.duration)
    }

    /**
     * 处理通话繁忙
     */
    private fun handleBusy(busy: ChatProtocol.Signaling.Busy) {
        if (_state.value.callId != busy.messageId) return
        endCall(reason = HangupReason.Busy)
    }

    /**
     * 更新对方的媒体状态（视频/麦克风/免提）
     */
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

    /**
     * 获取对方的铃声设置，发起方据此播放对应铃声
     */
    private fun handleRingtoneInfo(info: ChatProtocol.Signaling.RingtoneInfo) {
        if (_state.value.callId != info.messageId) return
        if (_state.value.callState != CallState.Outgoing) return

        scope.launch {
            callAudioManager.startRingtone(isIncoming = false, ringtone = info.ringtone)
        }
    }

    /**
     * 处理 ICE 连接状态变化：
     * - CONNECTED/COMPLETED：通话正式建立，启动计时
     * - DISCONNECTED：等待信令层处理，不主动挂断
     * - FAILED：直接结束通话
     */
    private fun handleIceStateChange(iceState: PeerConnection.IceConnectionState) {
        when (iceState) {
            PeerConnection.IceConnectionState.CONNECTED,
            PeerConnection.IceConnectionState.COMPLETED -> {
                if (_state.value.callState == CallState.Connecting) {
                    cancelTimeout()
                    _state.update { it.copy(callState = CallState.Connected) }
                    startDurationTimer()
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

    /**
     * 将 ICE 候选发送给对方
     */
    private fun sendIceCandidate(candidate: IceCandidate) {
        val current = _state.value
        if (current.callState == CallState.Idle || current.callState == CallState.Ended) return

        scope.launch {
            runCatching {
                sendSignaling(
                    targetUserId = current.peerId,
                    packet = ChatProtocol.Signaling.IceCandidate(
                        messageId = current.callId,
                        senderId = myUserId,
                        candidate = candidate.sdp,
                        sdpMid = candidate.sdpMid,
                        sdpMLineIndex = candidate.sdpMLineIndex,
                        signature = ""
                    )
                )
            }
        }
    }

    /**
     * 启动超时自动挂断：
     * - Ringing：等待对方接听，超时视为无应答
     * - Connecting：等待 ICE 建立，超时视为连接错误
     */
    private fun startTimeout(scenario: TimeoutScenario) {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            val (timeout, reason) = when (scenario) {
                TimeoutScenario.Ringing -> RING_TIMEOUT_MS to HangupReason.Timeout
                TimeoutScenario.Connecting -> CONNECT_TIMEOUT_MS to HangupReason.Error
            }
            delay(timeout)
            sendHangup(reason)
            endCall(reason)
        }
    }

    /**
     * 取消超时计时器
     */
    private fun cancelTimeout() {
        timeoutJob?.cancel()
        timeoutJob = null
    }

    /**
     * 开始通话计时，每秒更新 duration
     */
    private fun startDurationTimer() {
        durationTimerJob?.cancel()
        durationTimerJob = scope.launch {
            var seconds = 0L
            while (true) {
                delay(1000)
                _state.update { it.copy(duration = ++seconds) }
            }
        }
    }

    /**
    * 结束时长计时器
    */
    private fun stopDurationTimer() {
        durationTimerJob?.cancel()
        durationTimerJob = null
    }

    /**
     * 结束通话：取消计时、更新状态、释放 WebRTC 资源、保存通话记录
     */
    private fun endCall(reason: HangupReason, isFromMe: Boolean = false, duration: Long? = null) {
        if (_state.value.callState.isTerminal) return

        cancelTimeout()
        stopDurationTimer()

        val snapshot = _state.value
        _state.update {
            it.copy(
                callState = CallState.Ended,
                hangupResult = HangupResult(reason, isFromMe)
            )
        }
        webRTCManager.release()

        scope.launch {
            saveCallRecord(snapshot, reason, duration)
        }
    }

    /**
     * 写通话记录
     */
    private suspend fun saveCallRecord(
        snapshot: CallUiState,
        reason: HangupReason,
        duration: Long? = null
    ) {
        val status = reason.toCallStatus()
        val actualDuration = duration ?: snapshot.duration

        if (isOutgoing) {
            messageRepository.sendMessage(
                sessionId = snapshot.peerId,
                receiverId = snapshot.peerId,
                messageId = snapshot.callId,
                content = MessageContent.Call(
                    type = snapshot.callType,
                    status = status,
                    duration = actualDuration
                )
            )
        } else {
            messageDispatcher.dispatch(
                ChatProtocol.CallMessage(
                    messageId = snapshot.callId,
                    senderId = snapshot.peerId,
                    receiverId = myUserId,
                    status = status.name,
                    duration = actualDuration,
                    callType = snapshot.callType,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * 重置状态
     */
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

private fun HangupReason.toCallStatus() = when (this) {
    HangupReason.Normal -> CallStatus.Finished
    HangupReason.Declined -> CallStatus.Declined
    HangupReason.Cancelled -> CallStatus.Cancelled
    HangupReason.Timeout -> CallStatus.Missed
    HangupReason.Busy,
    HangupReason.Offline,
    HangupReason.Error -> CallStatus.Failed
}

@Suppress("UNCHECKED_CAST")
private fun <T : ChatProtocol.Signaling> T.withSignature(signature: String): T = when (this) {
    is ChatProtocol.Signaling.Offer -> copy(signature = signature)
    is ChatProtocol.Signaling.Answer -> copy(signature = signature)
    is ChatProtocol.Signaling.IceCandidate -> copy(signature = signature)
    is ChatProtocol.Signaling.Hangup -> copy(signature = signature)
    is ChatProtocol.Signaling.Busy -> copy(signature = signature)
    is ChatProtocol.Signaling.MediaState -> copy(signature = signature)
    is ChatProtocol.Signaling.RingtoneInfo -> copy(signature = signature)
} as T