package top.chengdongqing.wechat.feature.call.ui

import android.app.KeyguardManager
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.model.CallState
import top.chengdongqing.wechat.core.model.CallType
import top.chengdongqing.wechat.feature.call.service.CallActionReceiver

@AndroidEntryPoint
class CallActivity : ComponentActivity() {

    private val viewModel: CallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupWindowFlags()
        handleIntent(intent)
        observePictureInPictureState()

        setContent {
            WeTheme {
                CallScreen(
                    viewModel = viewModel,
                    onDismiss = ::finish,
                    onMinimize = ::minimizeCall
                )
            }
        }
    }

    private fun minimizeCall() {
        val state = viewModel.state.value
        if (state.callType == CallType.Video &&
            state.callState == CallState.Connected &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        ) {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(9, 16))
                    .setActions(createPipActions(state.isMicOn))
                    .build()
            )
        } else {
            // CallActivity 使用独立 taskAffinity，仅把通话任务退到后台。
            moveTaskToBack(true)
        }
    }

    private fun observePictureInPictureState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    val activeVideo = state.callType == CallType.Video &&
                        state.callState == CallState.Connected
                    val builder = PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(9, 16))
                        .setActions(createPipActions(state.isMicOn))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        builder.setAutoEnterEnabled(activeVideo)
                        builder.setSeamlessResizeEnabled(true)
                    }
                    setPictureInPictureParams(builder.build())
                    if (state.callState.isTerminal && isInPictureInPictureMode) {
                        finish()
                    }
                }
            }
        }
    }

    private fun createPipActions(isMicOn: Boolean): List<RemoteAction> {
        return listOf(
            RemoteAction(
                Icon.createWithResource(this, android.R.drawable.ic_lock_silent_mode),
                if (isMicOn) "静音" else "取消静音",
                if (isMicOn) "静音" else "取消静音",
                callAction(4101, CallActionReceiver.ACTION_TOGGLE_MUTE)
            ),
            RemoteAction(
                Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
                "挂断",
                "挂断",
                callAction(4102, CallActionReceiver.ACTION_HANGUP)
            )
        )
    }

    private fun callAction(requestCode: Int, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            this,
            requestCode,
            Intent(this, CallActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.O until Build.VERSION_CODES.S &&
            viewModel.state.value.callType == CallType.Video &&
            viewModel.state.value.callState == CallState.Connected
        ) {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(9, 16))
                    .setActions(createPipActions(viewModel.state.value.isMicOn))
                    .build()
            )
        }
    }

    /** singleTask 模式下，Activity 已在栈顶时来电走此回调 */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /**
     * 处理 Intent 参数
     *
     * 携带 peerId + callType → 主动发起通话
     * 不携带参数 → 被动来电，CallManager 已处于 Incoming 状态，UI 自动渲染
     */
    private fun handleIntent(intent: Intent?) {
        val peerId = intent?.getStringExtra(EXTRA_PEER_ID) ?: return
        val callType = intent.getStringExtra(EXTRA_CALL_TYPE)?.let {
            CallType.valueOf(it)
        } ?: return

        viewModel.startCall(peerId, callType)
    }

    /**
     * 配置窗口标志
     *
     * FLAG_KEEP_SCREEN_ON：通话期间屏幕常亮
     * showWhenLocked + turnScreenOn：来电时穿透锁屏并点亮屏幕（Android 8.1+ 新 API）
     */
    private fun setupWindowFlags() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // 屏幕常亮

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    companion object {
        const val EXTRA_PEER_ID = "extra_peer_id"
        const val EXTRA_CALL_TYPE = "extra_call_type"
    }
}

fun Context.startCall(peerId: String, callType: CallType) {
    val intent = Intent(this, CallActivity::class.java).apply {
        putExtra(CallActivity.EXTRA_PEER_ID, peerId)
        putExtra(CallActivity.EXTRA_CALL_TYPE, callType.name)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    startActivity(intent)
}
