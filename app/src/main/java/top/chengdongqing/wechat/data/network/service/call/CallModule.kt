package top.chengdongqing.wechat.data.network.service.call

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
import top.chengdongqing.wechat.core.di.IoScope
import top.chengdongqing.wechat.data.network.model.NotificationId
import top.chengdongqing.wechat.data.network.service.ServiceModule
import top.chengdongqing.wechat.data.network.service.notification.NotificationHelper
import top.chengdongqing.wechat.features.call.manager.CallAudioManager
import top.chengdongqing.wechat.features.call.manager.CallManager
import top.chengdongqing.wechat.features.call.model.CallState
import top.chengdongqing.wechat.features.call.ui.CallActivity
import top.chengdongqing.wechat.features.settings.domain.repository.NotificationSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通话模块
 */
@Singleton
class CallModule @Inject constructor(
    private val callManager: CallManager,
    private val callAudioManager: CallAudioManager,
    private val notificationHelper: NotificationHelper,
    private val notificationRepository: NotificationSettingsRepository,
    @param:ApplicationContext private val context: Context,
    @param:IoScope private val scope: CoroutineScope
) : ServiceModule {
    private companion object {
        const val TAG = "CallModule"
    }

    private var observerJob: Job? = null

    override fun start() {
        runCatching {
            // 初始化通话管理器
            callManager.init()
            // 开始监听通话状态
            observerJob = scope.launch {
                observeCallState()
            }
        }.onSuccess {
            Log.d(TAG, "通话模块已启动")
        }.onFailure {
            Log.e(TAG, "通话模块已启动", it)
        }
    }

    override fun stop() {
        runCatching {
            // 取消订阅状态
            observerJob?.cancel()
            // 退出音频模式
            callAudioManager.exitCallMode()
            // 清除通话通知
            dismissNotification()
        }.onSuccess {
            Log.d(TAG, "通话模块已停止")
        }
    }

    /**
     * 订阅通话状态
     */
    private suspend fun observeCallState() {
        callManager.state
            .distinctUntilChangedBy { it.callState }
            .collect { state ->
                when (state.callState) {
                    /**
                     * 来电
                     */
                    CallState.Incoming -> {
                        if (callNotificationEnabled()) {
                            // 弹出通话界面
                            launchCallActivity()
                            // 响铃
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

                    /**
                     * 呼叫
                     */
                    CallState.Outgoing -> {
                        notificationHelper.showOngoingNotification(
                            context.getString(R.string.call_notification_outgoing, state.peerName)
                        )
                    }

                    /**
                     * 连接中
                     */
                    CallState.Connecting -> {
                        // 停止响铃
                        callAudioManager.stopRingtone()

                        notificationHelper.showOngoingNotification(
                            context.getString(R.string.call_notification_connecting)
                        )
                    }

                    /**
                     * 已连接
                     */
                    CallState.Connected -> {
                        // 触发振动
                        callAudioManager.vibrateOnConnected()
                        // 进入通话音频
                        callAudioManager.enterCallMode(state.isVideoCall)

                        notificationHelper.showOngoingNotification(
                            context.getString(R.string.call_notification_ongoing, state.peerName)
                        )
                    }

                    /**
                     * 通话结束
                     */
                    CallState.Ended -> {
                        // 播放挂断提示音
                        callAudioManager.playHangupTone {
                            // 退出通话音频
                            callAudioManager.exitCallMode()
                        }
                        // 清除通知
                        dismissNotification()
                    }

                    CallState.Idle -> dismissNotification()
                }
            }
    }

    /**
     * 调起通话界面
     */
    private fun launchCallActivity() {
        val intent = Intent(context, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }

    /**
     * 清除通知
     */
    private fun dismissNotification() {
        notificationHelper.cancelNotification(NotificationId.Call.id)
    }

    /**
     * 是否开启通话通知
     */
    private suspend fun callNotificationEnabled(): Boolean =
        notificationRepository.callNotificationEnabled.first()
}