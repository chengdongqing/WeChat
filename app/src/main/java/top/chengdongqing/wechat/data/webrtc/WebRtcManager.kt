package top.chengdongqing.wechat.data.webrtc

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.Priority
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import top.chengdongqing.wechat.core.utils.getBestSupportedResolution
import top.chengdongqing.wechat.data.model.ChatPayload

class WebRtcManager(
    private val context: Context,
    private val onSignalingMessage: (ChatPayload) -> Unit
) {
    private val eglBase = EglBase.create() // EGL 上下文，用于视频硬件加速
    val eglContext: EglBase.Context = eglBase.eglBaseContext // 供 AndroidView 使用
    private val factory: PeerConnectionFactory by lazy { createFactory() }
    private var peerConnection: PeerConnection? = null

    // 渲染器引用
    private var localSink: SurfaceViewRenderer? = null
    private var remoteSink: SurfaceViewRenderer? = null

    // 需要手动销毁的对象
    private var videoCapturer: VideoCapturer? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private fun createFactory(): PeerConnectionFactory {
        // webrtc库的全局初始化：加载本地c++库，初始化某些硬件加速组件的基础环境
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )

        return PeerConnectionFactory.builder()
            // 视频编码配置
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(
                    eglBase.eglBaseContext, // 支持硬件加速
                    true, // 启用VP8编码
                    true // 启用H.264高级配置
                )
            )
            // 视频解码配置
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    /**
     * 初始化视频预览
     */
    fun initVideoViews(local: SurfaceViewRenderer, remote: SurfaceViewRenderer) {
        localSink = local
        remoteSink = remote

        // 启动本地摄像头和音频
        startLocalStreaming()
        // 将摄像头采集的数据流添加到视频轨道
        localVideoTrack?.addSink(local)
    }

    private fun startLocalStreaming() {
        // 创建视频源：设置为摄像头，而不是屏幕共享
        val videoSource = factory.createVideoSource(false)
        // 创建一个OpenGL线程
        val helper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        // 创建视频捕获器
        videoCapturer = createVideoCapturer()
        videoCapturer?.initialize(helper, context, videoSource.capturerObserver)
        // 获取摄像头最佳分辨率
        val bestResolution = context.getBestSupportedResolution(true)
        println("---------bestResolution:$bestResolution")
        // 设置采样参数：宽、高、帧率
        videoCapturer?.startCapture(bestResolution.width, bestResolution.height, 60)
        // 创建视频轨道
        localVideoTrack = factory.createVideoTrack("VIDEO_101", videoSource)
        // 将视频轨道接入视图渲染器
        localVideoTrack?.addSink(localSink)

        // 创建音频源
        val audioSource = factory.createAudioSource(MediaConstraints())
        // 创建音频轨道
        localAudioTrack = factory.createAudioTrack("AUDIO_101", audioSource)
        // 调整系统音频设置
        adjustAudioSettings(isStart = true)

        // 初始化webrtc核心对象
        setupPeerConnection()
        // 此处告诉webrtc：将来建立连接后，将这条视频轨道和音频轨道发送给对方
        peerConnection?.addTrack(localVideoTrack, listOf("stream1"))
        peerConnection?.addTrack(localAudioTrack, listOf("stream1"))
    }

    private fun setupPeerConnection() {
        val config = PeerConnection.RTCConfiguration(emptyList()) // 局域网不需要STUN
        peerConnection = factory.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                // 当搜到本地网络路径，传给对方
                onSignalingMessage(
                    ChatPayload.Ice(
                        candidate.sdp,
                        candidate.sdpMid,
                        candidate.sdpMLineIndex
                    )
                )
            }

            override fun onAddStream(stream: MediaStream) {
                // 收到对方视频，挂载到 UI
                stream.videoTracks.firstOrNull()?.addSink(remoteSink)
            }

            // ... 其余空实现 ...
            override fun onSignalingChange(s: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(i: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(b: Boolean) {}
            override fun onIceGatheringChange(s: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(c: Array<out IceCandidate>?) {}
            override fun onRemoveStream(s: MediaStream?) {}
            override fun onDataChannel(d: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
        })
    }

    private fun updateVideoBitrate() {
        val senders = peerConnection?.senders
        val videoSender = senders?.find { it.track()?.kind() == "video" }
        videoSender?.let { sender ->
            val parameters = sender.parameters
            for (encoding in parameters.encodings) {
                encoding.maxBitrateBps = 8_000_000 // 8Mbps
                encoding.minBitrateBps = 2_000_000 // 2Mbps
                encoding.maxFramerate = 60
                // 设置优先级为高，保证资源优先给视频
                encoding.networkPriority = Priority.HIGH
            }
            sender.parameters = parameters
        }
    }

    fun startCall() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let { sdp ->
                    // 设置本地描述
                    peerConnection?.setLocalDescription(this, sdp)
                    // 发送 Offer 给对方
                    onSignalingMessage(ChatPayload.Sdp(sdp.description, sdp.type.canonicalForm()))
                    // 设置画质
                    updateVideoBitrate()
                }
            }
        }, constraints)
    }

    // --- 外部调用 C: 处理收到的信号 ---
    fun onRemoteSessionReceived(payload: ChatPayload.Sdp) {
        val type = SessionDescription.Type.fromCanonicalForm(payload.type)
        val sdp = SessionDescription(type, payload.sdp)

        peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                remoteDescriptionSet = true
                // 把攒着的 ICE 全部喂进去
                pendingIceCandidates.forEach { peerConnection?.addIceCandidate(it) }
                pendingIceCandidates.clear()

                if (type == SessionDescription.Type.OFFER) {
                    createAnswer()
                }
            }
        }, sdp)
    }

    private fun createAnswer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let { sdp ->
                    // 先尝试设置本地描述
                    peerConnection?.setLocalDescription(object : SimpleSdpObserver() {
                        override fun onSetSuccess() {
                            // 只有设置本地成功了，再把这个 Answer 发给对方
                            onSignalingMessage(
                                ChatPayload.Sdp(
                                    sdp.description,
                                    sdp.type.canonicalForm()
                                )
                            )
                        }
                    }, sdp)
                }
            }
        }, constraints)
    }

    private var remoteDescriptionSet = false
    private val pendingIceCandidates = mutableListOf<IceCandidate>()

    fun onRemoteIceReceived(ice: ChatPayload.Ice) {
        val candidate = IceCandidate(ice.sdpMid, ice.sdpMLineIndex, ice.sdp)
        if (remoteDescriptionSet) {
            peerConnection?.addIceCandidate(candidate)
        } else {
            // 如果 SDP 还没准备好，先存起来
            pendingIceCandidates.add(candidate)
        }
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        return enumerator.deviceNames.find { enumerator.isFrontFacing(it) }
            ?.let { enumerator.createCapturer(it, null) }
    }

    fun dispose() {
        println("----WebRTC: 开始释放资源...")
        try {
            // 1. 停止并销毁摄像头采集
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            videoCapturer = null

            // 2. 释放音视频轨道
            localVideoTrack?.dispose()
            localAudioTrack?.dispose()

            // 3. 关闭 PeerConnection 连接
            peerConnection?.dispose() // dispose 比 close 更彻底
            peerConnection = null

            // 4. 销毁工厂
            factory.dispose()

            // 5. 释放 EGL 上下文（渲染核心）
            eglBase.release()

            adjustAudioSettings(false)

            println("----WebRTC: 资源释放完毕")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setMicrophoneMute(isMute: Boolean) {
        // WebRTC 的轨道自带 setEnabled 方法
        // false 表示禁用轨道，对方就听不到你的声音了
        localAudioTrack?.setEnabled(!isMute)
        println("----WebRTC: 麦克风已${if (isMute) "禁用" else "启用"}")
    }

    private fun adjustAudioSettings(isStart: Boolean) {
        if (isStart) {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            // 开启通话时，默认切换到扬声器
            setSpeakerphoneOn(true)
        } else {
            audioManager.mode = AudioManager.MODE_NORMAL
            // 结束通话，清除强制指定的设备
            clearCommunicationDevice()
        }
    }

    private fun setSpeakerphoneOn(on: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ 的新方式
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val speakerDevice = devices.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }

            if (on && speakerDevice != null) {
                audioManager.setCommunicationDevice(speakerDevice)
            } else {
                audioManager.clearCommunicationDevice()
            }
        } else {
            // 旧版本兼容处理
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = on
        }
    }

    private fun clearCommunicationDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
        }
    }
}

// 辅助类：减少冗余代码
open class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(reason: String?) {
        println("----WebRTC SDP Create Failure: $reason")
    }

    override fun onSetFailure(reason: String?) {
        println("----WebRTC SDP Set Failure: $reason")
    }
}
