package top.chengdongqing.wechat2.ui.call_1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer
import top.chengdongqing.wechat2.core.util.ServiceLocator
import top.chengdongqing.wechat2.data.model_1.ChatPayload
import top.chengdongqing.wechat2.data.network.P2pConnectionManager
import top.chengdongqing.wechat2.data.webrtc.WebRtcManager

class CallActivity : ComponentActivity() {
    private lateinit var webRtcManager: WebRtcManager
    private val localRenderer by lazy { SurfaceViewRenderer(this) }
    private val remoteRenderer by lazy { SurfaceViewRenderer(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 接收传递过来的参数
        val targetIp = intent.getStringExtra("targetIp")
        val isOfferer = intent.getBooleanExtra("isOfferer", true)
        if (targetIp == null) {
            finish()
            return
        }

        val dispatcher = ServiceLocator.getMessageDispatcher(this)
        val connectionManager: P2pConnectionManager = ServiceLocator.getWifiLanManager(this)

        // 初始化WebRtcManager
        webRtcManager = WebRtcManager(this) { payload ->
            lifecycleScope.launch {
                // 发送信令
                connectionManager.sendPayload(targetIp, payload)
            }
        }

        // 接收信令
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dispatcher.signalingFlow.collect { envelope ->
                    when (val payload = envelope.payload) {
                        is ChatPayload.Sdp -> webRtcManager.onRemoteSessionReceived(payload)
                        is ChatPayload.Ice -> webRtcManager.onRemoteIceReceived(payload)
                        is ChatPayload.CallAction -> if (payload.action == "HANGUP") finish()
                        else -> {}
                    }
                }
            }
        }

        setContent {
            CallPermissionWrapper {
                LaunchedEffect(Unit) {
                    // 初始化视频流
                    webRtcManager.initVideoViews(localRenderer, remoteRenderer)

                    // 如果是拨打方则发起 Offer
                    if (isOfferer) {
                        // 发送视频通话信令
                        connectionManager.sendPayload(
                            targetIp,
                            ChatPayload.CallAction("START_VIDEO")
                        )
                        // 给对方一点启动缓冲时间，避免对方界面没有初始化完，导致丢包
                        delay(800)
                        // 正式开始 WebRTC 握手
                        webRtcManager.startCall()
                    }
                }

                CallScreen(
                    webRtcManager = webRtcManager,
                    localRenderer = localRenderer,
                    remoteRenderer = remoteRenderer,
                    isOfferer = isOfferer
                ) {
                    // 挂断通话
                    lifecycleScope.launch {
                        // 发送挂断通话信令
                        connectionManager.sendPayload(
                            targetIp,
                            ChatPayload.CallAction("HANGUP")
                        )
                        // 关闭界面
                        finish()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        // 释放渲染器
        localRenderer.release()
        remoteRenderer.release()
        // 销毁引擎
        if (::webRtcManager.isInitialized) {
            webRtcManager.dispose()
        }

        super.onDestroy()
    }
}