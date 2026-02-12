package top.chengdongqing.wechat.features.call

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.features.call.audio.SoundPlayer
import top.chengdongqing.wechat.features.call.model.AudioConfig
import top.chengdongqing.wechat.features.call.model.CallDirection
import top.chengdongqing.wechat.features.call.model.CallDuration
import top.chengdongqing.wechat.features.call.model.CallState
import top.chengdongqing.wechat.features.call.model.CallUser
import top.chengdongqing.wechat.features.call.model.VideoConfig
import top.chengdongqing.wechat.features.chat.domain.model.CallType
import javax.inject.Inject

/**
 * 通话UI状态
 */
data class CallUiState(
    val callType: CallType = CallType.Voice,
    val callDirection: CallDirection = CallDirection.Outgoing,
    val callState: CallState = CallState.Idle,
    val remoteUser: CallUser = CallUser("", ""),
    val duration: CallDuration = CallDuration(),
    val audioConfig: AudioConfig = AudioConfig(),
    val videoConfig: VideoConfig = VideoConfig()
) {
    /**
     * 是否正在通话中
     */
    val isCallActive: Boolean
        get() = callState is CallState.Active

    /**
     * 是否显示本地视频预览
     */
    val shouldShowLocalPreview: Boolean
        get() = callType == CallType.Video &&
                (callState is CallState.Active || callState is CallState.Connecting)

    /**
     * 是否显示远程视频
     */
    val shouldShowRemoteVideo: Boolean
        get() = callType == CallType.Video &&
                callState is CallState.Active &&
                videoConfig.isRemoteVideoEnabled

    /**
     * 获取状态文本
     */
    fun getStatusText(): String = when (callState) {
        is CallState.Connecting -> "等待对方接听..."
        is CallState.Ringing -> when (callType) {
            CallType.Voice -> "邀请你语音通话"
            CallType.Video -> "邀请你视频通话"
        }

        is CallState.Active -> duration.format()
        is CallState.Ended -> "通话已结束"
        is CallState.Failed -> "连接失败"
        else -> ""
    }
}

/**
 * UI事件
 */
sealed class CallUiEvent {
    /** 关闭Activity */
    object FinishActivity : CallUiEvent()

    /** 显示错误 */
    data class ShowError(val message: String) : CallUiEvent()
}

/**
 * 通话ViewModel
 */
@HiltViewModel
class CallViewModel @Inject constructor(
    private val soundPlayer: SoundPlayer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    // UI事件
    private val _events = MutableSharedFlow<CallUiEvent>()
    val events: SharedFlow<CallUiEvent> = _events.asSharedFlow()

    // 计时器Job
    private var timerJob: Job? = null

    // 铃声Job
    private var ringtoneJob: Job? = null

    init {
        initializeCallState(savedStateHandle)
    }

    /**
     * 初始化通话状态
     */
    private fun initializeCallState(savedStateHandle: SavedStateHandle) {
        val callType = savedStateHandle.getCallType()
        val callDirection = savedStateHandle.getCallDirection()
        val remoteUser = savedStateHandle.getRemoteUser()

        _uiState.update {
            it.copy(
                callType = callType,
                callDirection = callDirection,
                remoteUser = remoteUser,
                callState = when (callDirection) {
                    CallDirection.Outgoing -> CallState.Connecting
                    CallDirection.Incoming -> CallState.Ringing
                }
            )
        }

        // 根据呼叫方向启动相应流程
        when (callDirection) {
            CallDirection.Outgoing -> startConnecting()
            CallDirection.Incoming -> startRinging()
        }
    }

    /**
     * 开始连接（呼出）
     */
    private fun startConnecting() {
        playConnectingSound()

        // 模拟对方接听（实际应由信令服务器通知）
        viewModelScope.launch {
            delay(3000)
            if (_uiState.value.callState is CallState.Connecting) {
                acceptCall()
            }
        }
    }

    /**
     * 开始响铃（来电）
     */
    private fun startRinging() {
        playRingtone()
    }

    /**
     * 接听电话
     */
    fun acceptCall() {
        stopAllSounds()

        _uiState.update {
            it.copy(callState = CallState.Active(System.currentTimeMillis()))
        }

        startCallTimer()
    }

    /**
     * 拒接/挂断电话
     */
    fun rejectCall() {
        stopAllSounds()
        stopCallTimer()

        _uiState.update { it.copy(callState = CallState.Ended) }

        viewModelScope.launch {
            playCallEndSound()
            delay(1000) // 给用户1秒时间看到"通话已结束"
            _events.emit(CallUiEvent.FinishActivity)
        }
    }

    /**
     * 切换麦克风
     */
    fun toggleMic() {
        _uiState.update {
            it.copy(audioConfig = it.audioConfig.toggleMic())
        }
        // TODO: 实际控制音频采集
    }

    /**
     * 切换扬声器
     */
    fun toggleSpeaker() {
        _uiState.update {
            it.copy(audioConfig = it.audioConfig.toggleSpeaker())
        }
        // TODO: 实际控制AudioManager
    }

    /**
     * 切换摄像头
     */
    fun switchCamera() {
        _uiState.update {
            it.copy(videoConfig = it.videoConfig.switchCamera())
        }
        // TODO: 实际控制摄像头切换
    }

    /**
     * 启动通话计时器
     */
    private fun startCallTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.update {
                    it.copy(duration = it.duration.increment())
                }
            }
        }
    }

    /**
     * 停止计时器
     */
    private fun stopCallTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * 播放连接音
     */
    private fun playConnectingSound() {
        ringtoneJob?.cancel()
        ringtoneJob = viewModelScope.launch {
            while (isActive) {
                soundPlayer.play(SoundPlayer.Sound.Connecting)
                delay(3000)
            }
        }
    }

    /**
     * 播放铃声
     */
    private fun playRingtone() {
        ringtoneJob?.cancel()
        ringtoneJob = viewModelScope.launch {
            while (isActive) {
                soundPlayer.play(SoundPlayer.Sound.Ringing)
                delay(3000)
            }
        }
    }

    /**
     * 播放通话结束音
     */
    private fun playCallEndSound() {
        viewModelScope.launch {
            soundPlayer.play(SoundPlayer.Sound.CallEnd)
        }
    }

    /**
     * 停止所有声音
     */
    private fun stopAllSounds() {
        ringtoneJob?.cancel()
        ringtoneJob = null
        soundPlayer.stop()
    }

    override fun onCleared() {
        super.onCleared()
        stopAllSounds()
        stopCallTimer()
    }
}

/**
 * SavedStateHandle扩展函数
 */
private fun SavedStateHandle.getCallType(): CallType {
    return get<String>(CallActivity.EXTRA_CALL_TYPE)?.let {
        runCatching { CallType.valueOf(it) }.getOrNull()
    } ?: CallType.Voice
}

private fun SavedStateHandle.getCallDirection(): CallDirection {
    return get<String>(CallActivity.EXTRA_CALL_DIRECTION)?.let {
        runCatching { CallDirection.valueOf(it) }.getOrNull()
    } ?: CallDirection.Outgoing
}

private fun SavedStateHandle.getRemoteUser(): CallUser {
    val userId = get<String>(CallActivity.EXTRA_USER_ID) ?: ""
    val userName = get<String>(CallActivity.EXTRA_USER_NAME) ?: "WeChat User"
    val userAvatar = get<String>(CallActivity.EXTRA_USER_AVATAR)
    return CallUser(userId, userName, userAvatar)
}