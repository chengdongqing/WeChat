package top.chengdongqing.wechat.data.network.service.modules

import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.notification.NotificationHelper
import top.chengdongqing.wechat.features.call.manager.CallAudioManager
import top.chengdongqing.wechat.features.call.manager.CallManager
import top.chengdongqing.wechat.features.call.model.CallState
import top.chengdongqing.wechat.features.call.ui.CallActivity
import top.chengdongqing.wechat.features.settings.domain.repository.NotificationSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通话模块
 *
 * 订阅 [CallManager.state]，根据状态变化自动执行对应副作用：
 * - Incoming   → 启动 CallActivity + 播放铃声 + 显示来电通知
 * - Outgoing   → 播放拨号音 + 显示呼出通知
 * - Connecting → 停止铃声 + 显示连接中通知
 * - Connected  → 振动提示 + 切换通话音频模式 + 显示通话中通知
 * - Ended      → 播放挂断音 + 退出音频模式 + 关闭通知
 * - Idle       → 关闭通知
 */
@Singleton
class CallModule @Inject constructor(
    private val callManager: CallManager,
    private val callAudioManager: CallAudioManager,
    private val notificationHelper: NotificationHelper,
    private val notificationRepository: NotificationSettingsRepository,
    @param:ApplicationContext private val context: Context
) {
    private companion object {
        const val TAG = "CallModule"
    }

    private var observerJob: Job? = null

    /**
     * 初始化 CallManager 并开始订阅状态变化
     */
    fun start(myUserId: String, scope: CoroutineScope) {
        callManager.init(myUserId)
        observerJob = scope.launch {
            observeCallState()
        }

        Log.d(TAG, "通话模块已启动")
    }

    /**
     * 取消状态订阅，退出音频模式，关闭通知
     */
    fun stop() {
        observerJob?.cancel()
        callAudioManager.exitCallMode()
        dismissNotification()

        Log.d(TAG, "通话模块已停止")
    }

    /**
     * 订阅通话状态，callState 变化时触发对应副作用
     *
     * 用 distinctUntilChangedBy 避免同状态重复触发（如 peerName 更新导致的重组）
     */
    private suspend fun observeCallState() {
        callManager.state
            .distinctUntilChangedBy { it.callState }
            .collect { state ->
                when (state.callState) {
                    CallState.Incoming -> {
                        if (callNotificationEnabled()) {
                            launchCallActivity()
                            callAudioManager.startRingtone(isIncoming = true)
                        }
                        notificationHelper.showIncomingNotification(
                            title = state.peerName,
                            text = context.getString(
                                if (state.isVideoCall) R.string.call_notification_incoming_video
                                else R.string.call_notification_incoming_voice
                            )
                        )
                    }

                    CallState.Outgoing -> {
                        notificationHelper.showOngoingNotification(
                            context.getString(R.string.call_notification_outgoing, state.peerName)
                        )
                    }

                    CallState.Connecting -> {
                        callAudioManager.stopRingtone()
                        notificationHelper.showOngoingNotification(
                            context.getString(R.string.call_notification_connecting)
                        )
                    }

                    CallState.Connected -> {
                        callAudioManager.vibrateOnConnected()
                        callAudioManager.enterCallMode(state.isVideoCall)
                        notificationHelper.showOngoingNotification(
                            context.getString(R.string.call_notification_ongoing, state.peerName)
                        )
                    }

                    CallState.Ended -> {
                        callAudioManager.playHangupTone {
                            callAudioManager.exitCallMode()
                        }
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

    /** 关闭通话通知 */
    private fun dismissNotification() {
        notificationHelper.cancelNotification(NotificationHelper.CALL_NOTIFICATION_ID)
    }

    private suspend fun callNotificationEnabled(): Boolean =
        notificationRepository.callNotificationEnabled.first()
}