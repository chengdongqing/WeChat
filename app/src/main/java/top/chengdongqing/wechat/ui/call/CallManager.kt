package top.chengdongqing.wechat.ui.call

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.protocol.MessageDispatcher
import top.chengdongqing.wechat.data.model.ChatPayload
import top.chengdongqing.wechat.data.model.MessageEnvelope

class CallManager(
    private val context: Context,
    private val dispatcher: MessageDispatcher
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun observeIncomingCalls() {
        scope.launch {
            dispatcher.signalingFlow.collect { envelope ->
                val payload = envelope.payload
                if (payload is ChatPayload.CallAction && payload.action == "START_VIDEO") {
                    // 只有在当前没有通话时才跳转
                    if (!isAlreadyInCall()) {
                        launchCallActivity(envelope)
                    }
                }
            }
        }
    }

    private fun launchCallActivity(envelope: MessageEnvelope) {
        val intent = Intent(context, CallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // 传递必要信息
            putExtra("targetId", envelope.senderId)
            putExtra("targetName", envelope.senderName)
            putExtra("targetIp", envelope.senderIp)
            putExtra("isOfferer", false) // 接收方
        }
        context.startActivity(intent)
    }

    private fun isAlreadyInCall(): Boolean {
        // TODO
        return false
    }
}