package top.chengdongqing.wechat.data.network.service.modules

import android.app.Notification
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
 * 订阅 [CallManager.state]，根据状态变化自动执行对应副作用：
 * - Incoming   → 启动 CallActivity + 播放铃声 + 显示来电通知
 * - Outgoing   → 播放拨号音 + 显示呼出通知
 * - Connecting → 停止铃声 + 显示连接中通知
 * - Connected  → 震动提示 + 切换通话音频模式 + 显示通话中通知
 * - Ended      → 播放挂断音 + 退出音频模式 + 关闭通知
 * - Idle       → 关闭通知
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

    /** 初始化 CallManager 并开始订阅状态变化 */
    fun start(myUserId: String, scope: CoroutineScope) {
        callManager.init(myUserId)
        createNotificationChannel()
        observerJob = scope.launch { observeCallState() }
        Log.d(TAG, "通话模块已启动")
    }

    /** 取消状态订阅，退出音频模式，关闭通知 */
    fun stop() {
        observerJob?.cancel()
        callAudioManager.exitCallMode()
        dismissNotification()
        Log.d(TAG, "通话模块已停止")
    }

    // ==================== 状态订阅 ====================

    /**
     * 订阅通话状态，callState 变化时触发对应副作用
     *
     * 用 distinctUntilChangedBy 避免同状态重复触发（如 peerName 更新导致的重组）
     */
    private suspend fun observeCallState() {
        callManager.state
            .distinctUntilChangedBy { it.callState }
            .collect { state ->
                Log.d(TAG, "通话状态变化: ${state.callState}")
                when (state.callState) {
                    CallState.Incoming -> {
                        launchCallActivity()
                        callAudioManager.startRingtone(isIncoming = true)
                        showIncomingNotification(state.peerName, state.isVideoCall)
                    }

                    CallState.Outgoing -> {
                        callAudioManager.startRingtone(isIncoming = false)
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

                    CallState.Idle -> dismissNotification()
                }
            }
    }

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

    /**
     * 显示来电通知
     *
     * 使用 fullScreenIntent 在锁屏/息屏时直接唤起通话界面。
     * CATEGORY_CALL 使系统对来电通知给予最高优先级展示。
     */
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

        notificationManager.notify(CALL_NOTIFICATION_ID, notification)
    }

    /** 显示通话进行中通知（呼出中 / 连接中 / 通话中），点击可返回通话界面 */
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

        notificationManager.notify(CALL_NOTIFICATION_ID, notification)
    }

    /** 关闭通话通知 */
    private fun dismissNotification() {
        notificationManager.cancel(CALL_NOTIFICATION_ID)
    }

    /**
     * 创建通话通知渠道
     *
     * 渠道声音设为 null，铃声由 [CallAudioManager] 单独管理，避免系统通知音和铃声叠加。
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CALL_CHANNEL_ID,
                "通话",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC  // 锁屏也显示
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}