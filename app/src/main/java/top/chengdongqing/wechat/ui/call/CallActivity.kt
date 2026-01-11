package top.chengdongqing.wechat.ui.call

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer
import top.chengdongqing.wechat.core.util.ServiceLocator
import top.chengdongqing.wechat.data.model.ChatPayload
import top.chengdongqing.wechat.data.network.WifiLanManager
import top.chengdongqing.wechat.data.webrtc.WebRtcManager

class CallActivity : ComponentActivity() {
    private lateinit var webRtcManager: WebRtcManager

    // 使用 lazy 确保 context 环境完全就绪
    private val localRenderer by lazy { SurfaceViewRenderer(this) }
    private val remoteRenderer by lazy { SurfaceViewRenderer(this) }

    // 建议从你的全局单例获取，保证 server 正在运行
    private lateinit var wifiLanManager: WifiLanManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dispatcher = ServiceLocator.getMessageDispatcher(this)

        // 1. 获取全局 Manager
        wifiLanManager = ServiceLocator.getWifiLanManager(this)

        // 2. 获取目标 Peer
        val targetIp = intent.getStringExtra("targetIp")
        val isOfferer = intent.getBooleanExtra("isOfferer", true)

        // 如果没有目标，直接关闭
        if (targetIp == null) {
            finish()
            return
        }

        // 3. 初始化 WebRtcManager
        webRtcManager = WebRtcManager(this) { payload ->
            lifecycleScope.launch(Dispatchers.IO) {
                // 直接发送 Payload 实体，WifiLanManager 内部处理序列化
                wifiLanManager.sendPayload(targetIp, payload)
            }
        }

        // 4. 监听信令 (保持原有逻辑)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dispatcher.signalingFlow.collect { payload ->
                    when (payload) {
                        is ChatPayload.Sdp -> webRtcManager.onRemoteSessionReceived(payload)
                        is ChatPayload.Ice -> webRtcManager.onRemoteIceReceived(payload)
                        is ChatPayload.CallAction -> {
                            if (payload.action == "HANGUP") finish()
                        }

                        else -> {}
                    }
                }
            }
        }

        // 5. UI 与权限处理
        setContent {
            CallPermissionWrapper {
                // 只有权限通过后才初始化
                LaunchedEffect(Unit) {
                    webRtcManager.initVideoViews(localRenderer, remoteRenderer)

                    // 【关键】如果你是拨打方，在此处发起 Offer
                    if (isOfferer) {
                        // 【新增】第一步：先告诉对方“我要找你视频了，快开界面！”
                        println("----WebRTC: 发送呼叫邀请...")
                        wifiLanManager.sendPayload(
                            targetIp,
                            ChatPayload.CallAction("START_VIDEO")
                        )

                        // 【优化】给对方一点点启动 Activity 的缓冲时间（例如 800ms）
                        // 如果太快发 Offer，对方界面还没初始化完，可能会丢包
                        delay(800)

                        // 第二步：正式开始 WebRTC 握手
                        println("----WebRTC: 发送 SDP Offer...")
                        webRtcManager.startCall()
                    }
                }

                CallScreen(
                    webRtcManager = webRtcManager,
                    localRenderer = localRenderer,
                    remoteRenderer = remoteRenderer,
                    isOfferer = isOfferer
                ) {
                    // 挂断逻辑：发信号给对方并关闭自己
                    lifecycleScope.launch {
                        wifiLanManager.sendPayload(
                            targetIp,
                            ChatPayload.CallAction("HANGUP")
                        )
                        finish()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        // 顺序很重要：先停渲染器，后销毁引擎
        localRenderer.release()
        remoteRenderer.release()
        if (::webRtcManager.isInitialized) {
            webRtcManager.dispose()
        }
        super.onDestroy()
    }
}