package top.chengdongqing.wechat.features.call.manager

import android.content.Context
import android.media.AudioFormat
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpSender
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule
import top.chengdongqing.wechat.features.call.model.CallType
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * WebRTC 引擎管理器
 *
 * 负责 PeerConnection 生命周期、媒体采集、SDP 协商和通话中控制。
 * 单例持有 EglBase，跨通话复用，避免重复初始化 GPU 上下文。
 *
 * 视频质量：启动时自动探测摄像头支持的最高 [VideoProfile]，从 2K@60fps 向下兼容至 480p。
 * 音频质量：48kHz 立体声，Opus 128kbps + 前向纠错（FEC）。
 */
@Singleton
class WebRTCManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private companion object {
        const val TAG = "WebRTCManager"
    }

    /** EglBase 跨通话复用，不随 [release] 销毁 */
    val eglBase: EglBase by lazy { EglBase.create() }

    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    // 媒体资源
    private var videoCapturer: CameraVideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var audioDeviceModule: JavaAudioDeviceModule? = null
    private var audioSource: AudioSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null

    private var isUsingFrontCamera = true
    private var activeProfile: VideoProfile = VideoProfile.FHD_30

    /** 本端 ICE 候选，采集到后发给对端 */
    private val _localIceCandidates = MutableSharedFlow<IceCandidate>(extraBufferCapacity = 32)
    val localIceCandidates: SharedFlow<IceCandidate> = _localIceCandidates.asSharedFlow()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)

    /** ICE 连接状态，Connected 时通话正式建立 */
    private val _iceConnectionState = MutableStateFlow(PeerConnection.IceConnectionState.NEW)
    val iceConnectionState: StateFlow<PeerConnection.IceConnectionState> =
        _iceConnectionState.asStateFlow()

    // ==================== 视频质量档位 ====================

    /**
     * 视频质量档位，从高到低排列
     *
     * [selectBestProfile] 会选取设备摄像头支持的最高档位，
     * 设备不支持时自动降级，最低兜底为 [SD_30]。
     */
    enum class VideoProfile(
        val width: Int,
        val height: Int,
        val fps: Int,
        val maxBitrate: Int,
        val minBitrate: Int,
        val label: String
    ) {
        QHD_60(2560, 1440, 60, 20_000_000, 6_000_000, "2K@60fps"),
        QHD_30(2560, 1440, 30, 15_000_000, 4_000_000, "2K@30fps"),
        FHD_60(1920, 1080, 60, 12_000_000, 4_000_000, "1080p@60fps"),
        FHD_30(1920, 1080, 30, 8_000_000, 2_000_000, "1080p@30fps"),
        HD_30(1280, 720, 30, 4_000_000, 1_000_000, "720p@30fps"),
        SD_30(640, 480, 30, 1_500_000, 500_000, "480p@30fps");
    }

    /** 探测摄像头支持的最高档位，找不到匹配则返回 [VideoProfile.SD_30] */
    private fun selectBestProfile(enumerator: Camera2Enumerator, cameraName: String): VideoProfile {
        val formats = enumerator.getSupportedFormats(cameraName)
        if (formats.isNullOrEmpty()) return VideoProfile.SD_30

        return VideoProfile.entries.firstOrNull { profile ->
            formats.any { f ->
                f.width >= profile.width &&
                        f.height >= profile.height &&
                        f.framerate.max >= profile.fps * 1000
            }
        } ?: VideoProfile.SD_30.also { Log.d(TAG, "未找到匹配档位，降级至 ${it.label}") }
    }

    // ==================== 音频配置 ====================

    private object AudioConfig {
        const val SAMPLE_RATE = 48_000      // 48kHz CD 级采样率
        const val MAX_BITRATE = 128_000     // Opus 128kbps
    }

    // ==================== 初始化 ====================

    /**
     * 初始化 WebRTC 引擎
     *
     * 必须在任何 PeerConnection 操作前调用。
     * 启用硬件 AEC（回声消除）和 NS（噪声抑制）。
     */
    fun initialize() {
        // 如果 factory 还在且没被销毁，不重复初始化
        if (factory != null) return

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )

        audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .setSampleRate(AudioConfig.SAMPLE_RATE)
            .setAudioFormat(AudioFormat.ENCODING_PCM_16BIT)
            .createAudioDeviceModule()

        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setAudioDeviceModule(audioDeviceModule)
            .createPeerConnectionFactory()

        Log.d(TAG, "WebRTC 引擎已初始化")
    }

    // ==================== PeerConnection ====================

    /**
     * 创建 PeerConnection
     *
     * 使用 UNIFIED_PLAN 语义，持续 ICE 收集，仅使用低成本候选路径（LAN 优先）。
     */
    fun createPeerConnection() {
        val config = PeerConnection.RTCConfiguration(emptyList()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            candidateNetworkPolicy = PeerConnection.CandidateNetworkPolicy.LOW_COST
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
        }
        peerConnection = factory?.createPeerConnection(config, PeerConnectionObserver())
    }

    // ==================== 媒体采集 ====================

    /**
     * 启动本端媒体采集
     *
     * 音频：每次调用都确保采集已启动。
     * 视频：首次调用创建轨道；若轨道已存在但 capturer 未启动（如权限延迟获取），则重启 capturer。
     */
    fun startLocalMedia(callType: CallType, renderer: SurfaceViewRenderer? = null) {
        val f = factory ?: return
        if (localAudioTrack == null) startAudioCapture(f)
        if (callType == CallType.Video) {
            if (localVideoTrack == null) startVideoCapture(f, renderer)
            else restartVideoCapture()
        }
    }

    /**
     * 重启视频采集
     *
     * 用于权限补救场景：handleOffer 时相机权限未授予，接受通话后补充启动。
     */
    fun restartVideoCapture() {
        runCatching {
            videoCapturer?.startCapture(
                activeProfile.width,
                activeProfile.height,
                activeProfile.fps
            )
        }.onFailure { Log.e(TAG, "重启视频采集失败", it) }
    }

    /**
     * 启动音频采集
     *
     * 关闭 AGC（自动增益）和高通滤波，保留原始音色；启用 AEC 和 NS 由硬件处理。
     */
    private fun startAudioCapture(factory: PeerConnectionFactory) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl2", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "false"))
        }
        audioSource = factory.createAudioSource(constraints)
        localAudioTrack = factory.createAudioTrack("audio_track", audioSource).apply {
            setEnabled(true)
        }
        peerConnection?.addTrack(localAudioTrack)?.also { configureBitrate(it) }
    }

    /**
     * 启动视频采集
     *
     * 自动选取前置摄像头，探测支持的最高画质档位。
     * 检查是否已添加视频轨道，避免重复 addTrack 导致协商异常。
     */
    private fun startVideoCapture(factory: PeerConnectionFactory, renderer: SurfaceViewRenderer?) {
        val enumerator = Camera2Enumerator(context)
        val cameraName = enumerator.deviceNames
            .firstOrNull { enumerator.isFrontFacing(it) }
            ?: enumerator.deviceNames.firstOrNull()
            ?: return

        activeProfile = selectBestProfile(enumerator, cameraName)
        Log.d(
            TAG,
            "视频采集: ${activeProfile.label}, ${activeProfile.minBitrate / 1_000_000}~${activeProfile.maxBitrate / 1_000_000} Mbps"
        )

        videoCapturer = Camera2Capturer(context, cameraName, null)
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        videoSource = factory.createVideoSource(false)
        videoCapturer?.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
        videoCapturer?.startCapture(activeProfile.width, activeProfile.height, activeProfile.fps)

        localVideoTrack = factory.createVideoTrack("video_track", videoSource).apply {
            setEnabled(true)
            renderer?.let { addSink(it) }
        }

        // 避免重复 addTrack 导致 SDP 协商出现多余轨道
        val alreadyAdded =
            peerConnection?.senders?.any { it.track()?.id() == "video_track" } ?: false
        if (!alreadyAdded) {
            peerConnection?.addTrack(localVideoTrack)?.also { configureVideoSender(it) }
        }
    }

    // ==================== 码率配置 ====================

    /** 配置视频发送码率和帧率上限 */
    private fun configureVideoSender(sender: RtpSender) {
        val params = sender.parameters
        params.encodings.forEach { encoding ->
            encoding.maxBitrateBps = activeProfile.maxBitrate
            encoding.minBitrateBps = activeProfile.minBitrate
            encoding.maxFramerate = activeProfile.fps
            encoding.scaleResolutionDownBy = 1.0
        }
        sender.parameters = params
    }

    /** 配置音频发送码率上限 */
    private fun configureBitrate(sender: RtpSender, maxBitrate: Int = AudioConfig.MAX_BITRATE) {
        val params = sender.parameters
        params.encodings.forEach { it.maxBitrateBps = maxBitrate }
        sender.parameters = params
    }

    // ==================== SDP 协商 ====================

    /** 创建 Offer，自动优化 SDP 并设置为本端描述 */
    suspend fun createOffer(): SessionDescription = suspendCancellableCoroutine { cont ->
        peerConnection?.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                val optimized = optimizeSdp(sdp)
                peerConnection?.setLocalDescription(SimpleSdpObserver(), optimized)
                cont.resume(optimized)
            }

            override fun onCreateFailure(error: String) =
                cont.resumeWithException(RuntimeException("createOffer: $error"))
        }, sdpConstraints())
    }

    /** 创建 Answer，自动优化 SDP 并设置为本端描述 */
    suspend fun createAnswer(): SessionDescription = suspendCancellableCoroutine { cont ->
        peerConnection?.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                val optimized = optimizeSdp(sdp)
                peerConnection?.setLocalDescription(SimpleSdpObserver(), optimized)
                cont.resume(optimized)
            }

            override fun onCreateFailure(error: String) =
                cont.resumeWithException(RuntimeException("createAnswer: $error"))
        }, sdpConstraints())
    }

    /** 设置远端 SDP 描述 */
    suspend fun setRemoteDescription(sdp: SessionDescription) =
        suspendCancellableCoroutine { cont ->
            peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
                override fun onSetSuccess() = cont.resume(Unit)
                override fun onSetFailure(error: String) =
                    cont.resumeWithException(RuntimeException("setRemoteDescription: $error"))
            }, sdp)
        }

    /** 添加远端 ICE 候选 */
    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    private fun sdpConstraints() = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
    }

    // ==================== SDP 优化 ====================

    /**
     * 优化 SDP
     *
     * 1. 视频编码器优先级重排：H265 > AV1 > H264 > VP9 > VP8
     *    设备不支持的编码器不会出现在 SDP 中，自动 fallback
     * 2. Opus 增强：48kHz 立体声、128kbps、FEC 前向纠错、禁用 DTX
     */
    private fun optimizeSdp(sdp: SessionDescription): SessionDescription {
        val desc = enhanceOpus(reorderVideoCodecs(sdp.description))
        return SessionDescription(sdp.type, desc)
    }

    /**
     * 重排 m=video 行中的 payload type 顺序
     *
     * SDP 格式：`m=video 9 UDP/TLS/RTP/SAVPF 96 97 98 ...`
     * 排在前面的 payload type 优先协商。
     */
    private fun reorderVideoCodecs(sdp: String): String {
        val lines = sdp.split("\r\n").toMutableList()
        val mVideoIdx = lines.indexOfFirst { it.startsWith("m=video") }
        if (mVideoIdx == -1) return sdp

        val codecMap = mutableMapOf<String, String>()
        lines.forEach { line ->
            Regex("^a=rtpmap:(\\d+) ([\\w-]+)/").find(line)?.let {
                codecMap[it.groupValues[1]] = it.groupValues[2].uppercase()
            }
        }

        val parts = lines[mVideoIdx].split(" ").toMutableList()
        if (parts.size < 4) return sdp

        val sorted = parts.subList(3, parts.size).sortedBy { pt ->
            when {
                codecMap[pt].orEmpty().let { it.contains("H265") || it.contains("HEVC") } -> 0
                codecMap[pt].orEmpty().contains("AV1") -> 1
                codecMap[pt].orEmpty().contains("H264") -> 2
                codecMap[pt].orEmpty().contains("VP9") -> 3
                codecMap[pt].orEmpty().contains("VP8") -> 4
                codecMap[pt].orEmpty().contains("RTX") -> 10
                codecMap[pt].orEmpty().contains("RED") -> 11
                else -> 5
            }
        }

        lines[mVideoIdx] = parts.subList(0, 3).joinToString(" ") + " " + sorted.joinToString(" ")
        Log.d(TAG, "视频编码器首选: ${sorted.firstOrNull()?.let { codecMap[it] } ?: "unknown"}")
        return lines.joinToString("\r\n")
    }

    /**
     * 增强 Opus 参数
     *
     * stereo=1            立体声
     * maxaveragebitrate   128kbps
     * useinbandfec=1      FEC 前向纠错，抗丢包
     * usedtx=0            禁用 DTX（静音期保持码率，音质更稳）
     */
    private fun enhanceOpus(sdp: String): String {
        val params = "stereo=1;sprop-stereo=1;maxaveragebitrate=128000;useinbandfec=1;usedtx=0"
        val pt = Regex("a=rtpmap:(\\d+) opus/48000/2").find(sdp)?.groupValues?.get(1) ?: return sdp
        val fmtpRegex = Regex("(a=fmtp:$pt )(.*)")

        return if (fmtpRegex.containsMatchIn(sdp)) {
            sdp.replace(fmtpRegex) { match ->
                if (match.groupValues[2].contains("maxaveragebitrate")) match.value
                else "${match.groupValues[1]}${match.groupValues[2]};$params"
            }
        } else {
            sdp.replace(
                "a=rtpmap:$pt opus/48000/2",
                "a=rtpmap:$pt opus/48000/2\r\na=fmtp:$pt minptime=10;$params"
            )
        }
    }

    // ==================== 通话中控制 ====================

    /** 切换麦克风静音状态，返回静音后的状态（true = 已静音） */
    fun toggleMute(): Boolean {
        val track = localAudioTrack ?: return false
        track.setEnabled(!track.enabled())
        return !track.enabled()
    }

    /**
     * 切换摄像头开关状态，返回切换后的状态（true = 已开启）
     *
     * 关闭时停止 capturer 省电；开启时重启 capturer 确保画面恢复。
     */
    fun toggleVideo(): Boolean {
        val track = localVideoTrack ?: return false
        val isEnabled = !track.enabled()
        track.setEnabled(isEnabled)

        if (isEnabled) {
            runCatching {
                videoCapturer?.startCapture(
                    activeProfile.width,
                    activeProfile.height,
                    activeProfile.fps
                )
            }.onFailure { Log.e(TAG, "toggleVideo 启动摄像头失败", it) }
        } else {
            videoCapturer?.stopCapture()
        }
        return isEnabled
    }

    /** 前后摄像头切换 */
    fun switchCamera() {
        videoCapturer?.switchCamera(null)
        isUsingFrontCamera = !isUsingFrontCamera
    }

    /** 绑定本端视频渲染器 */
    fun setLocalRenderer(renderer: SurfaceViewRenderer) {
        localRenderer = renderer
        localVideoTrack?.addSink(renderer)
    }

    /** 绑定远端视频渲染器；若远端轨道已到达，立即挂载 */
    fun setRemoteRenderer(renderer: SurfaceViewRenderer) {
        remoteRenderer = renderer
        _remoteVideoTrack.value?.addSink(renderer)
    }

    /**
     * 交换本端和远端渲染器（大小屏切换）
     *
     * 移除旧绑定 → 交叉绑定 → 互换引用，三步保证不丢帧。
     */
    fun swapRenderers() {
        val local = localRenderer ?: return
        val remote = remoteRenderer ?: return
        localVideoTrack?.removeSink(local)
        _remoteVideoTrack.value?.removeSink(remote)
        localVideoTrack?.addSink(remote)
        _remoteVideoTrack.value?.addSink(local)
        localRenderer = remote
        remoteRenderer = local
    }

    /** 本端视频轨道是否尚未创建（用于判断摄像头是否已启动） */
    fun isLocalVideoTrackNull() = localVideoTrack == null

    // ==================== 资源释放 ====================

    /**
     * 释放所有媒体资源和 PeerConnection
     *
     * 注意：[eglBase] 不在此处释放，跨通话复用。
     */
    fun release() {
        // 1. 先停掉采集器，防止它继续往 Source 送帧
        try {
            videoCapturer?.stopCapture()
        } catch (e: Exception) {
            Log.e(TAG, "stopCapture failed", e)
        }

        // 2. 移除渲染器绑定
        localVideoTrack?.removeSink(localRenderer)
        _remoteVideoTrack.value?.removeSink(remoteRenderer)

        // 3. 释放轨道 (Track)
        localVideoTrack?.dispose()
        localVideoTrack = null
        localAudioTrack?.dispose()
        localAudioTrack = null

        // 4. 释放源 (Source)
        videoSource?.dispose()
        videoSource = null
        audioSource?.dispose()
        audioSource = null

        // 5. 释放辅助工具
        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null

        // 6. 释放采集器
        videoCapturer?.dispose()
        videoCapturer = null

        // 7. 关闭连接
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null

        // 8. 销毁 Factory
        factory?.dispose()
        factory = null

        // 9. 清理远端轨道状态
        _remoteVideoTrack.value = null
    }

    // ==================== PeerConnection 回调 ====================

    private inner class PeerConnectionObserver : PeerConnection.Observer {
        /** ICE 候选采集完成，发给对端 */
        override fun onIceCandidate(candidate: IceCandidate) {
            _localIceCandidates.tryEmit(candidate)
        }

        /** ICE 连接状态变化，Connected 时通话正式建立 */
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
            _iceConnectionState.value = state
        }

        /** UNIFIED_PLAN：通过 transceiver 获取远端视频轨道 */
        override fun onTrack(transceiver: RtpTransceiver) {
            (transceiver.receiver.track() as? VideoTrack)?.let { _remoteVideoTrack.value = it }
        }

        /** PLAN_B 兼容：通过 stream 获取远端视频轨道 */
        override fun onAddStream(stream: MediaStream) {
            stream.videoTracks?.firstOrNull()?.let { _remoteVideoTrack.value = it }
        }

        override fun onSignalingChange(state: PeerConnection.SignalingState) {}
        override fun onIceConnectionReceivingChange(receiving: Boolean) {}
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
        override fun onRemoveStream(stream: MediaStream) {}
        override fun onDataChannel(channel: DataChannel) {}
        override fun onRenegotiationNeeded() {}
        override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {}
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
    }
}

/** SdpObserver 空实现基类，子类只需覆盖关心的回调 */
open class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String) {
        Log.e("SDP", "create 失败: $error")
    }

    override fun onSetFailure(error: String) {
        Log.e("SDP", "set 失败: $error")
    }
}