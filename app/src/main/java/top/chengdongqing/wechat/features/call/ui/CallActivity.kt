package top.chengdongqing.wechat.features.call.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.call.domain.model.CallType

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
                CallScreen(
                    viewModel = viewModel,
                    onDismiss = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // singleTask 模式下，来电时已在前台会走这里
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val peerId = intent?.getStringExtra(EXTRA_PEER_ID)
        val callTypeName = intent?.getStringExtra(EXTRA_CALL_TYPE)

        if (peerId != null && callTypeName != null) {
            // 主动发起
            val callType = CallType.valueOf(callTypeName)
            viewModel.startCall(peerId, callType)
        }
        // 被动来电: 不做任何事，CallManager 已经是 Incoming 状态，UI 自动渲染
    }

    private fun setupWindowFlags() {
        // 保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        const val EXTRA_PEER_ID = "extra_peer_id"
        const val EXTRA_CALL_TYPE = "extra_call_type"
    }
}

fun Context.startCall(peerId: String, callType: CallType) {
    startActivity(
        Intent(this, CallActivity::class.java).apply {
            putExtra(CallActivity.EXTRA_PEER_ID, peerId)
            putExtra(CallActivity.EXTRA_CALL_TYPE, callType.name)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )
}