package top.chengdongqing.wechat.feature.call.manager

import android.annotation.SuppressLint
import android.content.Context
import android.telecom.DisconnectCause
import android.util.Log
import androidx.core.net.toUri
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.common.di.IoScope
import top.chengdongqing.wechat.core.model.CallState
import top.chengdongqing.wechat.core.model.CallType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 将应用内 WebRTC 通话登记到 Android Telecom。
 * 系统、蓝牙耳机、手表和车机产生的接听/挂断/静音事件统一回调到 CallManager。
 */
@Singleton
@SuppressLint("MissingPermission")
class SystemTelecomController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val callManager: CallManager,
    @param:IoScope private val scope: CoroutineScope
) {
    private val callsManager = CallsManager(context)
    private var observerJob: Job? = null
    private var activeCallId: String? = null

    fun start() {
        if (observerJob != null) return
        runCatching {
            callsManager.registerAppWithTelecom(
                CallsManager.CAPABILITY_BASELINE or CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING
            )
        }.onFailure { Log.w(TAG, "Telecom 注册失败，将继续使用应用内通话界面", it) }

        observerJob = scope.launch {
            callManager.state
                .filter { !it.callState.isTerminal && it.callId.isNotBlank() }
                .map { it.callId }
                .distinctUntilChanged()
                .collectLatest { callId ->
                    activeCallId = callId
                    runCatching { addCall(callId) }
                        .onFailure { Log.w(TAG, "通话接入 Telecom 失败", it) }
                    activeCallId = null
                }
        }
    }

    fun stop() {
        observerJob?.cancel()
        observerJob = null
        activeCallId = null
    }

    private suspend fun addCall(callId: String) {
        val initial = callManager.state.first { it.callId == callId }
        val attributes = CallAttributesCompat(
            displayName = initial.peerName.ifBlank { "微信用户" },
            address = "sip:${initial.peerId}".toUri(),
            direction = if (initial.isOutgoing) {
                CallAttributesCompat.DIRECTION_OUTGOING
            } else {
                CallAttributesCompat.DIRECTION_INCOMING
            },
            callType = if (initial.callType == CallType.Video) {
                CallAttributesCompat.CALL_TYPE_VIDEO_CALL
            } else {
                CallAttributesCompat.CALL_TYPE_AUDIO_CALL
            }
        )

        callsManager.addCall(
            callAttributes = attributes,
            onAnswer = { callManager.accept() },
            onDisconnect = { callManager.hangup() },
            onSetActive = {},
            onSetInactive = {
                if (callManager.state.value.isMicOn) callManager.toggleMic()
            }
        ) {
            launch {
                isMuted.collect { muted ->
                    if (muted == callManager.state.value.isMicOn) callManager.toggleMic()
                }
            }
            launch {
                var answered = false
                var activated = false
                callManager.state.collect { state ->
                    if (state.callId != callId) return@collect
                    when {
                        !initial.isOutgoing && !answered &&
                            state.callState == CallState.Connecting -> {
                            answered = true
                            answer(attributes.callType)
                        }
                        !activated && state.callState == CallState.Connected -> {
                            activated = true
                            setActive()
                        }
                        state.callState.isTerminal -> {
                            disconnect(DisconnectCause(DisconnectCause.LOCAL))
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val TAG = "SystemTelecom"
    }
}
