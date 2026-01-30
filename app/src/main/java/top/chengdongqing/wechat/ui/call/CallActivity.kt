package top.chengdongqing.wechat.ui.call

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import top.chengdongqing.wechat.data.call.model.CallType

@AndroidEntryPoint
class CallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val viewModel: CallViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()

            when (state.callType) {
                CallType.VOICE -> VoiceCallScreen(
                    state = state,
                    onAcceptCall = { viewModel.acceptCall() },
                    onHangup = {
                        viewModel.hangup()
                        finish()
                    },
                    onToggleMic = { viewModel.toggleMic() },
                    onToggleSpeaker = { viewModel.toggleSpeaker() }
                )

                CallType.VIDEO -> {} //VideoCallScreen(viewModel)
            }
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, CallActivity::class.java)
    }
}

fun Context.startCall() {
    val intent = CallActivity.newIntent(this).apply {
//        putExtra()
    }
    startActivity(intent)
}