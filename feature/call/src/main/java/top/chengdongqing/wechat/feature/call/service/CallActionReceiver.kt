package top.chengdongqing.wechat.feature.call.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import top.chengdongqing.wechat.feature.call.manager.CallManager
import javax.inject.Inject

@AndroidEntryPoint
class CallActionReceiver : BroadcastReceiver() {
    @Inject lateinit var callManager: CallManager

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ACCEPT -> callManager.accept()
            ACTION_DECLINE -> callManager.decline()
            ACTION_HANGUP -> callManager.hangup()
            ACTION_TOGGLE_MUTE -> callManager.toggleMic()
        }
    }

    companion object {
        const val ACTION_ACCEPT = "top.chengdongqing.wechat.call.ACCEPT"
        const val ACTION_DECLINE = "top.chengdongqing.wechat.call.DECLINE"
        const val ACTION_HANGUP = "top.chengdongqing.wechat.call.HANGUP"
        const val ACTION_TOGGLE_MUTE = "top.chengdongqing.wechat.call.TOGGLE_MUTE"
    }
}
