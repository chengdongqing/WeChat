package top.chengdongqing.wechat.data.network.service.modules

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.features.call.domain.model.CallState
import top.chengdongqing.wechat.features.call.manager.CallAudioManager
import top.chengdongqing.wechat.features.call.manager.CallManager
import top.chengdongqing.wechat.features.call.ui.CallActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通话模块
 *
 * 核心职责: 监听 CallManager 状态变化，自动执行副作用:
 * - Incoming → 启动 CallActivity + 播放铃声 + 显示来电通知
 * - Outgoing → 播放拨号音
 * - Connected → 切换到通话音频模式
 * - Ended → 停止一切，关闭通知
 */
@Singleton
class CallModule @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val callManager: CallManager,
    private val callAudioManager: CallAudioManager
) {
    private companion object {
        const val TAG = "CallModule"
        const val CALL_NOTIFICATION_ID = 2001
        const val CALL_CHANNEL_ID = "call_channel"
    }

    private var observerJob: Job? = null

    fun start(myUserId: String, scope: CoroutineScope) {
        callManager.init(myUserId)
        createNotificationChannel()

        observerJob = scope.launch {
            callManager.state
                .distinctUntilChangedBy { it.callState }
                .collect { state ->
                    Log.d(TAG, "通话状态变化: ${state.callState}")
                    when (state.callState) {
                        CallState.Incoming -> {
                            launchCallActivity()
                            callAudioManager.startRingtone(true)
                            showIncomingNotification(state.peerName, state.isVideoCall)
                        }

                        CallState.Outgoing -> {
                            callAudioManager.startRingtone(false)
                            showOngoingNotification("正在呼叫 ${state.peerName}...")
                        }

                        CallState.Connecting -> {
                            callAudioManager.stopRingtone()
                            showOngoingNotification("连接中...")
                        }

                        CallState.Connected -> {
                            callAudioManager.vibrateOnConnected()
                            callAudioManager.enterCallMode(state.isVideoCall)
                            showOngoingNotification("通话中 - ${state.peerName}")
                        }

                        CallState.Ended -> {
                            callAudioManager.exitCallMode()
                            callAudioManager.playHangupTone()
                            dismissNotification()
                        }

                        CallState.Idle -> {
                            dismissNotification()
                        }
                    }
                }
        }

        Log.d(TAG, "通话模块已启动")
    }

    fun stop() {
        observerJob?.cancel()
        callAudioManager.exitCallMode()
        dismissNotification()
    }

    // ==================== 启动 CallActivity ====================

    /**
     * 来电时自动启动通话界面
     *
     * FLAG_ACTIVITY_NEW_TASK: 从 Service 启动 Activity 必须
     * 不传 peerId/callType: CallActivity 从 CallManager.state 读取
     */
    private fun launchCallActivity() {
        val intent = Intent(context, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }

    // ==================== 通知 ====================

    private fun showIncomingNotification(peerName: String, isVideo: Boolean) {
        val fullScreenIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, CallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val typeText = if (isVideo) "视频通话" else "语音通话"
        val notification = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_filled)
            .setContentTitle(peerName)
            .setContentText("$typeText 来电")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenIntent, true)
            .setOngoing(true)
            .build()

        getNotificationManager().notify(CALL_NOTIFICATION_ID, notification)
    }

    private fun showOngoingNotification(text: String) {
        val intent = PendingIntent.getActivity(
            context, 0,
            Intent(context, CallActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_filled)
            .setContentTitle(text)
            .setContentIntent(intent)
            .setOngoing(true)
            .build()

        getNotificationManager().notify(CALL_NOTIFICATION_ID, notification)
    }

    private fun dismissNotification() {
        getNotificationManager().cancel(CALL_NOTIFICATION_ID)
    }

    private fun createNotificationChannel() {
        val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                CALL_CHANNEL_ID,
                "通话",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
            }
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        getNotificationManager().createNotificationChannel(channel)
    }

    private fun getNotificationManager() =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}