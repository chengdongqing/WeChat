package top.chengdongqing.wechat.features.call

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.call.model.CallDirection
import top.chengdongqing.wechat.features.call.screens.VideoCallScreen
import top.chengdongqing.wechat.features.call.screens.VoiceCallScreen
import top.chengdongqing.wechat.features.chat.domain.model.CallType

/**
 * 通话页面Activity
 *
 * 功能：
 * - 支持语音通话和视频通话
 * - 保持屏幕常亮
 * - 控制音频路由
 * - 响应ViewModel事件
 */
@AndroidEntryPoint
class CallActivity : ComponentActivity() {

    private val viewModel: CallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupWindow()
        observeViewModelEvents()

        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()

            WeTheme {
                when (uiState.callType) {
                    CallType.Voice -> VoiceCallScreen(
                        state = uiState,
                        onMinimize = ::minimizeToFloatingWindow,
                        onAcceptCall = viewModel::acceptCall,
                        onRejectCall = viewModel::rejectCall,
                        onToggleMic = viewModel::toggleMic,
                        onToggleSpeaker = viewModel::toggleSpeaker
                    )

                    CallType.Video -> VideoCallScreen(
                        state = uiState,
                        onMinimize = ::minimizeToFloatingWindow,
                        onAcceptCall = viewModel::acceptCall,
                        onRejectCall = viewModel::rejectCall,
                        onToggleMic = viewModel::toggleMic,
                        onToggleSpeaker = viewModel::toggleSpeaker,
                        onSwitchCamera = viewModel::switchCamera
                    )
                }
            }
        }
    }

    /**
     * 配置窗口属性
     */
    private fun setupWindow() {
        // 设置音频流类型为通话
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
        // 保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * 观察ViewModel事件
     */
    private fun observeViewModelEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    handleUiEvent(event)
                }
            }
        }
    }

    /**
     * 处理UI事件
     */
    private fun handleUiEvent(event: CallUiEvent) {
        when (event) {
            is CallUiEvent.FinishActivity -> finish()
            is CallUiEvent.ShowError -> showErrorAndFinish(event.message)
        }
    }

    /**
     * 显示错误并关闭页面
     */
    private fun showErrorAndFinish(message: String) {
        // TODO: 显示Toast或Snackbar
        finish()
    }

    /**
     * 最小化到悬浮窗
     */
    private fun minimizeToFloatingWindow() {
        // TODO: 实现悬浮窗功能
        moveTaskToBack(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理音频路由设置
        volumeControlStream = AudioManager.USE_DEFAULT_STREAM_TYPE
    }

    companion object {
        const val EXTRA_CALL_TYPE = "extra_call_type"
        const val EXTRA_CALL_DIRECTION = "extra_call_direction"
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_USER_NAME = "extra_user_name"
        const val EXTRA_USER_AVATAR = "extra_user_avatar"

        /**
         * 创建Intent
         */
        fun createIntent(
            context: Context,
            callType: CallType,
            callDirection: CallDirection,
            userId: String,
            userName: String,
            userAvatar: String? = null
        ): Intent {
            return Intent(context, CallActivity::class.java).apply {
                putExtra(EXTRA_CALL_TYPE, callType.name)
                putExtra(EXTRA_CALL_DIRECTION, callDirection.name)
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_USER_NAME, userName)
                putExtra(EXTRA_USER_AVATAR, userAvatar)

                // 添加FLAG确保新任务栈
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}

/**
 * Context扩展函数：发起通话
 */
fun Context.startCall(
    callType: CallType,
    userId: String,
    userName: String,
    userAvatar: String? = null
) {
    val intent = CallActivity.createIntent(
        context = this,
        callType = callType,
        callDirection = CallDirection.Outgoing,
        userId = userId,
        userName = userName,
        userAvatar = userAvatar
    )
    startActivity(intent)
}

/**
 * Context扩展函数：接收来电
 */
fun Context.receiveCall(
    callType: CallType,
    userId: String,
    userName: String,
    userAvatar: String? = null
) {
    val intent = CallActivity.createIntent(
        context = this,
        callType = callType,
        callDirection = CallDirection.Incoming,
        userId = userId,
        userName = userName,
        userAvatar = userAvatar
    )
    startActivity(intent)
}