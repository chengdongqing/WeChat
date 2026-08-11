package top.chengdongqing.wechat.service.call

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.repository.NotificationSettingsRepository
import top.chengdongqing.wechat.core.model.CallState
import top.chengdongqing.wechat.core.network.model.NotificationId
import top.chengdongqing.wechat.core.network.service.call.CallServiceModule
import top.chengdongqing.wechat.core.runtime.IoScope
import top.chengdongqing.wechat.feature.call.manager.CallAudioManager
import top.chengdongqing.wechat.feature.call.manager.CallManager
import top.chengdongqing.wechat.feature.call.manager.SystemTelecomController
import top.chengdongqing.wechat.feature.call.ui.CallActivity
import top.chengdongqing.wechat.service.notification.NotificationHelper
import javax.inject.Inject
import javax.inject.Singleton
import top.chengdongqing.wechat.R as AppR

/**
 * 通话模块
 */
@Singleton
class CallProtocolHandler @Inject constructor(
    private val callManager: CallManager,
    private val callAudioManager: CallAudioManager,
    private val systemTelecomController: SystemTelecomController,
    private val notificationHelper: NotificationHelper,
    private val notificationRepository: NotificationSettingsRepository,
    @param:ApplicationContext private val context: Context,
    @param:IoScope private val scope: CoroutineScope
) : CallServiceModule {
    private companion object {
        const val TAG = "CallProtocolHandler"
    }

    private var observerJob: Job? = null

    override fun start() {
        runCatching {
            // 初始化通话管理器
            callManager.init()
            systemTelecomController.start()
            // 开始监听通话状态
            observerJob = scope.launch {
                observeCallState()
            }
        }.onSuccess {
            Log.d(TAG, "通话模块已启动")
        }.onFailure {
            Log.e(TAG, "通话模块启动失败", it)
        }
    }

    override fun stop() {
        runCatching {
            // 取消订阅状态
            observerJob?.cancel()
            systemTelecomController.stop()
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
            .map { (it.callState to it.isMicOn) to it }
            .distinctUntilChanged { old, new -> old.first == new.first }
            .map { it.second }
            .collect { state ->
                when (state.callState) {
                    /**
                     * 来电
                     */
                    CallState.Incoming -> {
                        // 必须先发布 FullScreenIntent。锁屏或应用在后台时，Android
                        // 只允许通过来电通知点亮屏幕并启动接听界面。
                        notificationHelper.showIncomingNotification(
                            title = state.peerName,
                            text = context.getString(
                                if (state.isVideoCall) AppR.string.call_notification_incoming_video
                                else AppR.string.call_notification_incoming_voice
                            ),
                            isVideo = state.isVideoCall
                        )

                        if (callNotificationEnabled()) {
                            // 前台，或用户已允许在其他应用上层显示时，可直接打开。
                            // 锁屏仍交给 FullScreenIntent，以获得系统来电语义。
                            if (canLaunchActivityDirectly()) launchCallActivity()
                            // 响铃
                            callAudioManager.startRingtone(isIncoming = true)
                        }
                    }

                    /**
                     * 呼叫
                     */
                    CallState.Outgoing -> {
                        notificationHelper.showOngoingNotification(
                            context.getString(
                                AppR.string.call_notification_outgoing,
                                state.peerName
                            ),
                            peerName = state.peerName,
                            isMuted = !state.isMicOn,
                            isVideo = state.isVideoCall
                        )
                    }

                    /**
                     * 连接中
                     */
                    CallState.Connecting -> {
                        // 停止响铃
                        callAudioManager.stopRingtone()

                        notificationHelper.showOngoingNotification(
                            context.getString(AppR.string.call_notification_connecting),
                            peerName = state.peerName,
                            isMuted = !state.isMicOn,
                            isVideo = state.isVideoCall
                        )
                    }

                    /**
                     * 已连接
                     */
                    CallState.Connected -> {
                        // 某些设备在 Connecting 状态合并/丢帧时仍保留循环来电振动。
                        callAudioManager.stopRingtone()
                        // 触发振动
                        callAudioManager.vibrateOnConnected()
                        // 进入通话音频
                        callAudioManager.enterCallMode(state.isVideoCall)

                        notificationHelper.showOngoingNotification(
                            context.getString(
                                AppR.string.call_notification_ongoing,
                                state.peerName
                            ),
                            peerName = state.peerName,
                            durationSeconds = state.duration,
                            showChronometer = true,
                            isMuted = !state.isMicOn,
                            isVideo = state.isVideoCall
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

    private fun canLaunchActivityDirectly(): Boolean {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        // “显示在其他应用上层”是用户明确授予的后台展示能力。CallActivity 已设置
        // showWhenLocked/turnScreenOn，因此该权限存在时锁屏也可以直接展示接听页。
        if (Settings.canDrawOverlays(context)) return true
        if (keyguard.isKeyguardLocked) return false

        val process = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(process)
        val appIsForeground =
            process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND

        // Android 10+ 在屏幕已解锁时通常只把 FullScreenIntent 展示为横幅。
        // SYSTEM_ALERT_WINDOW 是系统明确允许后台启动来电 Activity 的例外之一。
        return appIsForeground
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
