package top.chengdongqing.wechat.feature.chat.ui.live

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpParameters
import org.webrtc.RtpReceiver
import org.webrtc.RtpSender
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import javax.inject.Inject
import kotlin.math.roundToInt

@Serializable
data class LiveSignal(
    val type: String,
    val targetId: String,
    val sdp: String? = null,
    val candidate: String? = null,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int = 0
)

class LiveWebRtcManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private companion object {
        const val TOTAL_UPLOAD_BUDGET_BPS = 6_000_000
        const val MIN_VIDEO_BITRATE_BPS = 900_000
        const val MAX_VIDEO_BITRATE_BPS = 3_000_000
        const val MAX_SCREEN_BITRATE_BPS = 5_000_000
        const val MIN_SCREEN_BITRATE_BPS = 1_500_000
        const val MAX_SCREEN_DIMENSION = 1920
    }

    private val eglBase = EglBase.create()
    private var factory: PeerConnectionFactory? = null
    private var capturer: CameraVideoCapturer? = null
    private var textureHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null
    private val beautyProcessor = BeautyVideoProcessor()
    private var audioSource: AudioSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var localRenderer: SurfaceViewRenderer? = null
    private var screenCapturer: ScreenCapturerAndroid? = null
    private var screenTextureHelper: SurfaceTextureHelper? = null
    private var screenVideoSource: VideoSource? = null
    private var screenVideoTrack: VideoTrack? = null
    private var remoteRenderer: SurfaceViewRenderer? = null
    private val peers = mutableMapOf<String, PeerConnection>()
    private val pendingIce = mutableMapOf<String, MutableList<IceCandidate>>()
    private var signalEmitter: ((LiveSignal) -> Unit)? = null
    private val _remoteTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteTrack = _remoteTrack.asStateFlow()
    val eglContext get() = eglBase.eglBaseContext

    fun initialize(onSignal: (LiveSignal) -> Unit) {
        if (factory != null) {
            signalEmitter = onSignal
            return
        }
        signalEmitter = onSignal
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglContext))
            .createPeerConnectionFactory()
    }

    fun startHostMedia(renderer: SurfaceViewRenderer) {
        localRenderer = renderer
        val factory = factory ?: return
        if (localVideoTrack != null) {
            localVideoTrack?.addSink(renderer)
            return
        }
        val enumerator = Camera2Enumerator(context)
        val name = enumerator.deviceNames.firstOrNull(enumerator::isFrontFacing)
            ?: enumerator.deviceNames.firstOrNull() ?: return
        capturer = Camera2Capturer(context, name, null)
        textureHelper = SurfaceTextureHelper.create("LiveCapture", eglContext)
        videoSource = factory.createVideoSource(false)
        videoSource?.setVideoProcessor(beautyProcessor)
        capturer?.initialize(textureHelper, context, videoSource?.capturerObserver)
        capturer?.startCapture(1280, 720, 24)
        localVideoTrack = factory.createVideoTrack("live_video", videoSource).also {
            it.addSink(renderer)
        }
        audioSource = factory.createAudioSource(MediaConstraints())
        localAudioTrack = factory.createAudioTrack("live_audio", audioSource)
    }

    fun switchCamera() {
        capturer?.switchCamera(null)
    }

    fun setMicEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun setCameraEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun setBeautyStrength(strength: Float) {
        beautyProcessor.setStrength(strength)
    }

    fun startScreenShare(permissionData: Intent, onStopped: () -> Unit): Boolean {
        val factory = factory ?: return false
        if (screenCapturer != null) return true
        return runCatching {
            val capturer = ScreenCapturerAndroid(
                permissionData,
                object : MediaProjection.Callback() {
                    override fun onStop() {
                        stopScreenShare()
                        onStopped()
                    }
                }
            )
            val helper = SurfaceTextureHelper.create("LiveScreenCapture", eglContext)
            val source = factory.createVideoSource(true)
            capturer.initialize(helper, context, source.capturerObserver)
            val metrics = context.resources.displayMetrics
            val longest = maxOf(metrics.widthPixels, metrics.heightPixels)
            val scale = minOf(1f, MAX_SCREEN_DIMENSION.toFloat() / longest)
            val captureWidth = ((metrics.widthPixels * scale).roundToInt() / 2 * 2)
                .coerceAtLeast(2)
            val captureHeight = ((metrics.heightPixels * scale).roundToInt() / 2 * 2)
                .coerceAtLeast(2)
            capturer.startCapture(captureWidth, captureHeight, 12)
            val track = factory.createVideoTrack("live_screen", source)
            screenCapturer = capturer
            screenTextureHelper = helper
            screenVideoSource = source
            screenVideoTrack = track
            localRenderer?.let { renderer ->
                localVideoTrack?.removeSink(renderer)
                track.addSink(renderer)
                renderer.setMirror(false)
            }
            replaceOutgoingVideoTrack(track, isScreenShare = true)
            rebalanceVideoBitrate()
            true
        }.getOrElse {
            stopScreenShare()
            false
        }
    }

    fun stopScreenShare() {
        val track = screenVideoTrack ?: return
        replaceOutgoingVideoTrack(localVideoTrack, isScreenShare = false)
        rebalanceVideoBitrate()
        localRenderer?.let { renderer ->
            track.removeSink(renderer)
            localVideoTrack?.addSink(renderer)
            renderer.setMirror(true)
        }
        runCatching { screenCapturer?.stopCapture() }
        screenCapturer?.dispose()
        screenTextureHelper?.dispose()
        track.dispose()
        screenVideoSource?.dispose()
        screenCapturer = null
        screenTextureHelper = null
        screenVideoTrack = null
        screenVideoSource = null
    }

    fun addViewer(peerId: String) {
        val peer = peers.getOrPut(peerId) { createPeer(peerId) ?: return }
        val outgoingTrack = screenVideoTrack ?: localVideoTrack
        outgoingTrack?.let {
            configureVideoSender(peer.addTrack(it), screenVideoTrack != null)
        }
        localAudioTrack?.let { peer.addTrack(it) }
        rebalanceVideoBitrate()
        peer.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peer.setLocalDescription(SimpleSdpObserver(), sdp)
                signalEmitter?.invoke(LiveSignal("offer", peerId, sdp = sdp.description))
            }
        }, MediaConstraints())
    }

    fun handleOffer(hostId: String, sdp: String) {
        val peer = peers.getOrPut(hostId) { createPeer(hostId) ?: return }
        peer.setRemoteDescription(
            object : SimpleSdpObserver() {
                override fun onSetSuccess() {
                    flushPendingIce(hostId, peer)
                    peer.createAnswer(object : SimpleSdpObserver() {
                        override fun onCreateSuccess(sdp: SessionDescription) {
                            peer.setLocalDescription(SimpleSdpObserver(), sdp)
                            signalEmitter?.invoke(
                                LiveSignal("answer", hostId, sdp = sdp.description)
                            )
                        }
                    }, MediaConstraints())
                }
            },
            SessionDescription(SessionDescription.Type.OFFER, sdp)
        )
    }

    fun handleAnswer(peerId: String, sdp: String) {
        val peer = peers[peerId] ?: return
        peer.setRemoteDescription(
            object : SimpleSdpObserver() {
                override fun onSetSuccess() = flushPendingIce(peerId, peer)
            },
            SessionDescription(SessionDescription.Type.ANSWER, sdp)
        )
    }

    fun handleIce(peerId: String, signal: LiveSignal) {
        val candidate = signal.candidate ?: return
        val ice = IceCandidate(signal.sdpMid, signal.sdpMLineIndex, candidate)
        val peer = peers[peerId]
        if (peer == null || peer.remoteDescription == null) {
            pendingIce.getOrPut(peerId, ::mutableListOf).add(ice)
        } else {
            peer.addIceCandidate(ice)
        }
    }

    fun bindRemoteRenderer(renderer: SurfaceViewRenderer) {
        remoteRenderer = renderer
        _remoteTrack.value?.addSink(renderer)
    }

    fun removePeer(peerId: String) {
        peers.remove(peerId)?.dispose()
        pendingIce.remove(peerId)
        rebalanceVideoBitrate()
    }

    fun release() {
        peers.values.forEach(PeerConnection::dispose)
        peers.clear()
        pendingIce.clear()
        stopScreenShare()
        runCatching { capturer?.stopCapture() }
        capturer?.dispose()
        textureHelper?.dispose()
        localVideoTrack?.dispose()
        localAudioTrack?.dispose()
        videoSource?.dispose()
        audioSource?.dispose()
        factory?.dispose()
        eglBase.release()
    }

    private fun createPeer(peerId: String): PeerConnection? {
        val config = PeerConnection.RTCConfiguration(emptyList()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        return factory?.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                signalEmitter?.invoke(
                    LiveSignal(
                        type = "ice",
                        targetId = peerId,
                        candidate = candidate.sdp,
                        sdpMid = candidate.sdpMid,
                        sdpMLineIndex = candidate.sdpMLineIndex
                    )
                )
            }
            override fun onTrack(transceiver: org.webrtc.RtpTransceiver) {
                (transceiver.receiver.track() as? VideoTrack)?.let {
                    _remoteTrack.value = it
                    remoteRenderer?.let(it::addSink)
                }
            }
            override fun onAddStream(stream: MediaStream) {
                stream.videoTracks.firstOrNull()?.let {
                    _remoteTrack.value = it
                    remoteRenderer?.let(it::addSink)
                }
            }
            override fun onSignalingChange(state: PeerConnection.SignalingState) = Unit
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) = Unit
            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) = Unit
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) = Unit
            override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) = Unit
            override fun onRemoveStream(stream: MediaStream) = Unit
            override fun onDataChannel(channel: org.webrtc.DataChannel) = Unit
            override fun onRenegotiationNeeded() = Unit
        })
    }

    private fun replaceOutgoingVideoTrack(track: VideoTrack?, isScreenShare: Boolean) {
        peers.values.forEach { peer ->
            peer.senders.firstOrNull { it.track()?.kind() == "video" }?.let { sender ->
                sender.setTrack(track, false)
                configureVideoSender(sender, isScreenShare)
            }
        }
    }

    private fun rebalanceVideoBitrate() {
        val viewerCount = peers.size.coerceAtLeast(1)
        val isScreenShare = screenVideoTrack != null
        val maximum = if (isScreenShare) MAX_SCREEN_BITRATE_BPS else MAX_VIDEO_BITRATE_BPS
        val minimum = if (isScreenShare) MIN_SCREEN_BITRATE_BPS else MIN_VIDEO_BITRATE_BPS
        val totalBudget = if (isScreenShare) 8_000_000 else TOTAL_UPLOAD_BUDGET_BPS
        val maxPerViewer = (totalBudget / viewerCount).coerceIn(minimum, maximum)
        peers.values.flatMap { it.senders }.forEach { sender ->
            if (sender.track()?.kind() == "video") {
                configureVideoSender(sender, isScreenShare, maxPerViewer)
            }
        }
    }

    private fun configureVideoSender(
        sender: RtpSender,
        isScreenShare: Boolean = false,
        maxBitrate: Int = if (isScreenShare) MAX_SCREEN_BITRATE_BPS else MAX_VIDEO_BITRATE_BPS
    ) {
        val parameters = sender.parameters
        parameters.degradationPreference = RtpParameters.DegradationPreference.MAINTAIN_RESOLUTION
        parameters.encodings.forEach { encoding ->
            val minimum = if (isScreenShare) {
                MIN_SCREEN_BITRATE_BPS
            } else {
                MIN_VIDEO_BITRATE_BPS
            }
            encoding.minBitrateBps = minimum.coerceAtMost(maxBitrate)
            encoding.maxBitrateBps = maxBitrate
            encoding.maxFramerate = if (isScreenShare) 12 else 24
            encoding.bitratePriority = 2.0
        }
        sender.parameters = parameters
    }

    private fun flushPendingIce(peerId: String, peer: PeerConnection) {
        pendingIce.remove(peerId)?.forEach(peer::addIceCandidate)
    }
}

private open class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription) = Unit
    override fun onSetSuccess() = Unit
    override fun onCreateFailure(error: String) = Unit
    override fun onSetFailure(error: String) = Unit
}
