package top.chengdongqing.wechat.features.call.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.call.model.CallType

@AndroidEntryPoint
class CallActivity : ComponentActivity() {

    private val viewModel: CallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupWindowFlags()
        handleIntent(intent)

        setContent {
            WeTheme {
                CallScreen(viewModel = viewModel, onDismiss = ::finish)
            }
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
        val callType =
            intent.getStringExtra(EXTRA_CALL_TYPE)?.let { CallType.valueOf(it) } ?: return
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