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
import top.chengdongqing.wechat.features.call.domain.model.CallType
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * WebRTC 引擎管理器
 */
@Singleton
class WebRTCManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private companion object {
        const val TAG = "WebRTCManager"
    }

    val eglBase: EglBase = EglBase.create()

    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    // 媒体资源
    private var videoCapturer: CameraVideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null

    private var isUsingFrontCamera = true
    private var activeProfile: VideoProfile = VideoProfile.FHD_30

    // 事件流
    private val _localIceCandidates = MutableSharedFlow<IceCandidate>(extraBufferCapacity = 32)
    val localIceCandidates: SharedFlow<IceCandidate> = _localIceCandidates.asSharedFlow()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)

    private val _iceConnectionState = MutableStateFlow(PeerConnection.IceConnectionState.NEW)
    val iceConnectionState: StateFlow<PeerConnection.IceConnectionState> =
        _iceConnectionState.asStateFlow()

    // ==================================================================================
    //  视频质量档位（从高到低，自动选择设备支持的最高档）
    // ==================================================================================

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

    private fun selectBestProfile(enumerator: Camera2Enumerator, cameraName: String): VideoProfile {
        val formats = enumerator.getSupportedFormats(cameraName)
        if (formats.isNullOrEmpty()) return VideoProfile.SD_30

        for (profile in VideoProfile.entries) {
            val supported = formats.any { format ->
                format.width >= profile.width
                        && format.height >= profile.height
                        && format.framerate.max >= profile.fps * 1000
            }
            if (supported) {
                Log.d(TAG, "选中视频档位: ${profile.label}")
                return profile
            }
        }
        return VideoProfile.SD_30
    }

    // ==================================================================================
    //  音频配置
    // ==================================================================================

    private object AudioConfig {
        const val SAMPLE_RATE = 48000       // 48kHz CD 级
        const val MAX_BITRATE = 128_000     // 128kbps Opus
    }

    // ==================================================================================
    //  初始化
    // ==================================================================================

    fun initialize() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )

        val audioModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .setSampleRate(AudioConfig.SAMPLE_RATE)
            .setAudioFormat(AudioFormat.ENCODING_PCM_16BIT)
            .createAudioDeviceModule()

        factory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
            )
            .setAudioDeviceModule(audioModule)
            .createPeerConnectionFactory()

        Log.d(TAG, "WebRTC 引擎已初始化")
    }

    // ==================================================================================
    //  PeerConnection
    // ==================================================================================

    fun createPeerConnection() {
        val config = PeerConnection.RTCConfiguration(emptyList()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            candidateNetworkPolicy = PeerConnection.CandidateNetworkPolicy.LOW_COST
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
        }
        peerConnection = factory?.createPeerConnection(config, PeerConnectionObserver())
    }

    // ==================================================================================
    //  媒体采集
    // ==================================================================================

    fun startLocalMedia(callType: CallType, renderer: SurfaceViewRenderer? = null) {
        val f = factory ?: return
        startAudioCapture(f)
        if (callType == CallType.Video) startVideoCapture(f, renderer)
    }

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

    private fun startVideoCapture(factory: PeerConnectionFactory, renderer: SurfaceViewRenderer?) {
        val enumerator = Camera2Enumerator(context)
        val cameraName = enumerator.deviceNames
            .firstOrNull { enumerator.isFrontFacing(it) }
            ?: enumerator.deviceNames.firstOrNull()
            ?: return

        activeProfile = selectBestProfile(enumerator, cameraName)

        videoCapturer = Camera2Capturer(context, cameraName, null)
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        videoSource = factory.createVideoSource(false)
        videoCapturer?.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
        videoCapturer?.startCapture(activeProfile.width, activeProfile.height, activeProfile.fps)

        localVideoTrack = factory.createVideoTrack("video_track", videoSource).apply {
            setEnabled(true)
            renderer?.let { addSink(it) }
        }
        peerConnection?.addTrack(localVideoTrack)?.also { configureVideoSender(it) }

        Log.d(
            TAG,
            "视频采集: ${activeProfile.label}, ${activeProfile.minBitrate / 1_000_000}~${activeProfile.maxBitrate / 1_000_000} Mbps"
        )
    }

    // ==================================================================================
    //  码率配置
    // ==================================================================================

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

    private fun configureBitrate(sender: RtpSender, maxBitrate: Int = AudioConfig.MAX_BITRATE) {
        val params = sender.parameters
        params.encodings.forEach { it.maxBitrateBps = maxBitrate }
        sender.parameters = params
    }

    // ==================================================================================
    //  SDP 协商
    // ==================================================================================

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

    suspend fun setRemoteDescription(sdp: SessionDescription) =
        suspendCancellableCoroutine { cont ->
            peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
                override fun onSetSuccess() = cont.resume(Unit)
                override fun onSetFailure(error: String) =
                    cont.resumeWithException(RuntimeException("setRemoteDescription: $error"))
            }, sdp)
        }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    private fun sdpConstraints() = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
    }

    // ==================================================================================
    //  SDP 优化：编码器排序 + Opus 增强
    // ==================================================================================

    /**
     * 优化 SDP
     *
     * 1. 重排视频编码器优先级: H265 > AV1 > H264 > VP9 > VP8
     *    设备不支持 H265/AV1 时 SDP 中不会有对应条目，自动 fallback
     *
     * 2. 增强 Opus 音频: 48kHz 立体声 128kbps + 前向纠错
     */
    private fun optimizeSdp(sdp: SessionDescription): SessionDescription {
        var desc = sdp.description
        desc = reorderVideoCodecs(desc)
        desc = enhanceOpus(desc)
        return SessionDescription(sdp.type, desc)
    }

    /**
     * 重排 m=video 行中的 payload type 顺序
     *
     * SDP 格式: m=video 9 UDP/TLS/RTP/SAVPF 96 97 98 ...
     * 每个数字对应 a=rtpmap 中的编码器，排在前面的优先协商
     */
    private fun reorderVideoCodecs(sdp: String): String {
        val lines = sdp.split("\r\n").toMutableList()
        val mVideoIdx = lines.indexOfFirst { it.startsWith("m=video") }
        if (mVideoIdx == -1) return sdp

        // payload type → codec 名称映射
        val codecMap = mutableMapOf<String, String>()
        for (line in lines) {
            Regex("^a=rtpmap:(\\d+) ([\\w-]+)/").find(line)?.let {
                codecMap[it.groupValues[1]] = it.groupValues[2].uppercase()
            }
        }

        val parts = lines[mVideoIdx].split(" ").toMutableList()
        if (parts.size < 4) return sdp

        val payloadTypes = parts.subList(3, parts.size).toMutableList()
        val sorted = payloadTypes.sortedBy { pt ->
            val codec = codecMap[pt] ?: ""
            when {
                codec.contains("H265") || codec.contains("HEVC") -> 0
                codec.contains("AV1") -> 1
                codec.contains("H264") -> 2
                codec.contains("VP9") -> 3
                codec.contains("VP8") -> 4
                codec.contains("RTX") -> 10
                codec.contains("RED") -> 11
                else -> 5
            }
        }

        lines[mVideoIdx] = parts.subList(0, 3).joinToString(" ") + " " + sorted.joinToString(" ")
        val preferred = sorted.firstOrNull()?.let { codecMap[it] } ?: "unknown"
        Log.d(TAG, "视频编码器首选: $preferred")

        return lines.joinToString("\r\n")
    }

    /**
     * 增强 Opus 参数
     *
     * stereo=1          立体声
     * maxaveragebitrate  128kbps
     * useinbandfec=1    前向纠错（抗丢包）
     * usedtx=0          禁用不连续传输（静音时也保持码率，音质更稳）
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

    // ==================================================================================
    //  通话中控制
    // ==================================================================================

    fun toggleMute(): Boolean {
        val track = localAudioTrack ?: return false
        track.setEnabled(!track.enabled())
        return !track.enabled()
    }

    fun toggleVideo(): Boolean {
        val track = localVideoTrack ?: return false
        track.setEnabled(!track.enabled())
        return !track.enabled()
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(null)
        isUsingFrontCamera = !isUsingFrontCamera
    }

    fun setLocalRenderer(renderer: SurfaceViewRenderer) {
        localRenderer = renderer
        localVideoTrack?.addSink(renderer)
    }

    fun setRemoteRenderer(renderer: SurfaceViewRenderer) {
        remoteRenderer = renderer
        _remoteVideoTrack.value?.addSink(renderer)
    }

    fun swapRenderers() {
        val local = localRenderer ?: return
        val remote = remoteRenderer ?: return

        // 移除旧的
        localVideoTrack?.removeSink(local)
        _remoteVideoTrack.value?.removeSink(remote)
        // 绑定新的
        localVideoTrack?.addSink(remote)
        _remoteVideoTrack.value?.addSink(local)
        // 交换引用
        localRenderer = remote
        remoteRenderer = local
    }

    // ==================================================================================
    //  资源释放
    // ==================================================================================

    fun release() {
        runCatching { videoCapturer?.stopCapture() }
        videoCapturer?.dispose()
        localVideoTrack?.dispose()
        localAudioTrack?.dispose()
        videoSource?.dispose()
        audioSource?.dispose()
        surfaceTextureHelper?.dispose()
        peerConnection?.dispose()

        videoCapturer = null
        videoSource = null
        audioSource = null
        localVideoTrack = null
        localAudioTrack = null
        surfaceTextureHelper = null
        peerConnection = null
        _remoteVideoTrack.value = null
        _iceConnectionState.value = PeerConnection.IceConnectionState.NEW

        Log.d(TAG, "WebRTC 资源已释放")
    }

    // ==================================================================================
    //  PeerConnection 回调
    // ==================================================================================

    private inner class PeerConnectionObserver : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate) {
            _localIceCandidates.tryEmit(candidate)
        }

        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
            Log.d(TAG, "ICE: $state")
            _iceConnectionState.value = state
        }

        override fun onTrack(transceiver: RtpTransceiver) {
            (transceiver.receiver.track() as? VideoTrack)?.let { _remoteVideoTrack.value = it }
        }

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

open class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String) {
        Log.e("SDP", "create: $error")
    }

    override fun onSetFailure(error: String) {
        Log.e("SDP", "set: $error")
    }
}