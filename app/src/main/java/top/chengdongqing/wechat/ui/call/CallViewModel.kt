package top.chengdongqing.wechat.ui.call

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.media.SoundTipPlayer
import top.chengdongqing.wechat.data.call.model.CallState
import top.chengdongqing.wechat.data.call.model.CallType

data class CallUiState(
    val callType: CallType = CallType.VOICE,
    val callState: CallState = CallState.Idle,
    val durationText: String = "00:00",
    val remoteUserName: String = "WeChat User",
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true
)

// UI 一次性事件（用于关闭 Activity 或弹 Toast）
sealed class CallUiEvent {
    object FinishActivity : CallUiEvent()
    data class ShowToast(val message: String) : CallUiEvent()
}

@HiltViewModel
class CallViewModel @Inject constructor(
    private val soundPlayer: SoundTipPlayer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 内部合并所有状态
    private val _uiState = MutableStateFlow(CallUiState())
    val uiState = _uiState.asStateFlow()

    // 发送一次性指令给 Activity
    private val _event = MutableSharedFlow<CallUiEvent>()
    val event = _event.asSharedFlow()

    private var timerJob: Job? = null
    private var ringtoneJob: Job? = null

    init {
        // 1. 从 Intent/SavedStateHandle 初始化数据
        val type = savedStateHandle.get<String>("arg_call_type")?.let {
            try {
                CallType.valueOf(it)
            } catch (e: Exception) {
                CallType.VOICE
            }
        } ?: CallType.VOICE

        val name = savedStateHandle.get<String>("arg_user_name") ?: "对方姓名"

        _uiState.update {
            it.copy(
                callType = type,
                remoteUserName = name,
                callState = CallState.Connecting // 进入页面即开始连接
            )
        }

        // 2. 开始呼叫逻辑
        startRingtoneLoop()
    }

    /**
     * 循环播放拨号音
     */
    private fun startRingtoneLoop() {
        ringtoneJob?.cancel()
        ringtoneJob = viewModelScope.launch {
            while (isActive) {
                soundPlayer.play(R.raw.phonering)
                delay(3000) // 每 3 秒播放一次
            }
        }
    }

    private fun stopRingtone() {
        ringtoneJob?.cancel()
        ringtoneJob = null
    }

    /**
     * 接听电话
     */
    fun acceptCall() {
        stopRingtone()
        _uiState.update { it.copy(callState = CallState.Active(System.currentTimeMillis())) }
        startTimer()
    }

    /**
     * 挂断电话
     */
    fun hangup() {
        stopRingtone()
        timerJob?.cancel()

        _uiState.update { it.copy(callState = CallState.Ended) }

        viewModelScope.launch {
            soundPlayer.play(R.raw.playend)
            delay(1000) // 给用户 1 秒感受“通话已结束”的 UI
            _event.emit(CallUiEvent.FinishActivity)
        }
    }

    /**
     * 通话计时
     */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val beginTime = System.currentTimeMillis()
            while (isActive) {
                val seconds = (System.currentTimeMillis() - beginTime) / 1000
                _uiState.update { it.copy(durationText = formatDuration(seconds)) }
                delay(1000)
            }
        }
    }

    /**
     * 开关麦克风
     */
    fun toggleMic() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    /**
     * 开关扬声器
     */
    fun toggleSpeaker() {
        _uiState.update { it.copy(isSpeakerOn = !it.isSpeakerOn) }
        // 实际开发中此处需要调用 AudioManager 切换物理声道
    }

    private fun formatDuration(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%02d:%02d".format(m, s)
    }

    override fun onCleared() {
        super.onCleared()
        stopRingtone()
        timerJob?.cancel()
    }
}