package top.chengdongqing.wechat.core.network.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.model.ConnectionMode
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.core.network.messaging.RealtimePacketBus
import top.chengdongqing.wechat.core.network.model.PacketType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.TreeMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Screen-scoped low-latency LAN audio transport.
 *
 * Frames are 16 kHz mono Opus in 20 ms packets. Each sender has an independent
 * decoder and bounded jitter queue; the playback loop mixes decoded PCM frames.
 */
@Singleton
class IntercomAudioEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val realtimePackets: RealtimePacketBus
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val remoteStreams = ConcurrentHashMap<String, RemoteStream>()
    private val sequence = AtomicInteger()
    private val audioManager = context.getSystemService(AudioManager::class.java)

    private var channelId: String? = null
    private var receiveSocket: DatagramSocket? = null
    private var sendSocket: DatagramSocket? = null
    private var receiveJob: Job? = null
    private var realtimeJob: Job? = null
    private var playbackJob: Job? = null
    private var captureJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var audioRecord: AudioRecord? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var opusEncoder: IntercomOpusEncoder? = null
    private var routingConfigured = false
    private var previousAudioMode = AudioManager.MODE_NORMAL
    private var previousSpeakerphone = false
    private var connectionMode = ConnectionMode.WiFiLan
    private var transportMode = IntercomTransport.Nearby

    @Synchronized
    fun start(
        channel: String,
        mode: ConnectionMode = ConnectionMode.WiFiLan,
        transport: IntercomTransport = IntercomTransport.Nearby
    ) {
        if (channelId == channel && connectionMode == mode && transportMode == transport &&
            (receiveJob?.isActive == true || realtimeJob?.isActive == true)
        ) return
        stop()
        channelId = channel
        connectionMode = mode
        transportMode = transport
        if (transport == IntercomTransport.Nearby && mode == ConnectionMode.WiFiLan) {
            sendSocket = DatagramSocket().apply { broadcast = true }
            startReceiver()
        } else {
            val packetType = transport.packetType
            realtimeJob = scope.launch {
                realtimePackets.events
                    .filter { it.type == packetType }
                    .collect { event -> acceptFrame(event.body, event.body.size) }
            }
        }
        configureSpeakerRoute()
        startPlayback()
    }

    fun canRecord(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

    @Synchronized
    fun setTransmitting(enabled: Boolean): Boolean {
        if (!enabled) {
            stopCapture()
            return true
        }
        if (!canRecord() || channelId == null) return false
        if (captureJob?.isActive != true) startCapture()
        return true
    }

    @Synchronized
    fun stop() {
        stopCapture()
        opusEncoder = null
        receiveSocket?.close()
        receiveSocket = null
        sendSocket?.close()
        sendSocket = null
        receiveJob?.cancel()
        receiveJob = null
        realtimeJob?.cancel()
        realtimeJob = null
        playbackJob?.cancel()
        playbackJob = null
        remoteStreams.clear()
        runCatching { audioTrack?.pause() }
        runCatching { audioTrack?.flush() }
        runCatching { audioTrack?.release() }
        audioTrack = null
        restoreAudioRoute()
        channelId = null
    }

    private fun startReceiver() {
        val socket = DatagramSocket(null).apply {
            reuseAddress = true
            broadcast = true
            receiveBufferSize = SOCKET_BUFFER_BYTES
            soTimeout = 1_000
            bind(InetSocketAddress(AUDIO_PORT))
        }
        receiveSocket = socket
        receiveJob = scope.launch {
            val buffer = ByteArray(MAX_PACKET_SIZE)
            while (isActive && !socket.isClosed) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    acceptFrame(packet.data, packet.length)
                } catch (_: SocketTimeoutException) {
                    // Allows cancellation checks while the channel is quiet.
                } catch (error: Exception) {
                    if (!socket.isClosed) Log.w(TAG, "接收对讲音频失败", error)
                }
            }
        }
    }

    private fun acceptFrame(bytes: ByteArray, length: Int) {
        val frame = decodeFrame(bytes, length) ?: return
        if (frame.channel != channelId) return
        if (frame.senderId == profileRepository.requireUserId()) return
        remoteStreams.getOrPut(frame.senderId) {
            RemoteStream(fecEnabled = usesUnreliableUdp())
        }.offer(frame)
    }

    private fun startPlayback() {
        val minBuffer = AudioTrack.getMinBufferSize(
            IntercomAudioFormat.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(IntercomAudioFormat.SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(max(minBuffer, IntercomAudioFormat.FRAME_BYTES * 8))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
        audioTrack = track
        track.play()
        playbackJob = scope.launch {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            val accumulator = IntArray(IntercomAudioFormat.FRAME_SAMPLES)
            val mixed = ShortArray(IntercomAudioFormat.FRAME_SAMPLES)
            while (isActive) {
                accumulator.fill(0)
                var streamCount = 0
                remoteStreams.values.forEach { stream ->
                    val samples = stream.poll() ?: return@forEach
                    streamCount++
                    for (index in samples.indices) {
                        accumulator[index] += samples[index].toInt()
                    }
                }
                if (streamCount > 0) {
                    for (index in mixed.indices) {
                        // Soft normalization prevents two simultaneous speakers clipping.
                        val equalPower = accumulator[index] / sqrt(streamCount.toDouble())
                        val boosted = (equalPower * OUTPUT_GAIN).toInt()
                        mixed[index] = boosted
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                            .toShort()
                    }
                } else {
                    // Keep AudioTrack clocked even when a network packet is late. Pausing
                    // writes here causes a hardware-buffer underrun and an audible gap.
                    mixed.fill(0)
                }
                track.write(mixed, 0, mixed.size, AudioTrack.WRITE_BLOCKING)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startCapture() {
        val minBuffer = AudioRecord.getMinBufferSize(
            IntercomAudioFormat.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            IntercomAudioFormat.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            max(minBuffer, IntercomAudioFormat.FRAME_BYTES * 4)
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return
        }
        audioRecord = recorder
        if (AcousticEchoCanceler.isAvailable()) {
            echoCanceler =
                AcousticEchoCanceler.create(recorder.audioSessionId)?.apply { enabled = true }
        }
        recorder.startRecording()
        if (opusEncoder == null) {
            // Keep codec state across push-to-talk bursts so remote decoders remain in sync.
            opusEncoder = IntercomOpusEncoder(enableFec = usesUnreliableUdp())
        }
        captureJob = scope.launch {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            val samples = ShortArray(IntercomAudioFormat.FRAME_SAMPLES)
            var offset = 0
            while (isActive && recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val count = recorder.read(
                    samples,
                    offset,
                    samples.size - offset,
                    AudioRecord.READ_BLOCKING
                )
                if (count > 0) offset += count
                if (offset == samples.size) {
                    sendFrame(samples)
                    offset = 0
                }
            }
        }
    }

    private suspend fun sendFrame(samples: ShortArray) {
        val channel = channelId ?: return
        val payload = opusEncoder?.encode(samples) ?: return
        val bytes = encodeFrame(
            channel = channel,
            senderId = profileRepository.requireUserId(),
            sequence = sequence.incrementAndGet(),
            payload = payload
        )
        if (usesUnreliableUdp()) {
            runCatching {
                sendSocket?.send(
                    DatagramPacket(
                        bytes,
                        bytes.size,
                        InetAddress.getByName(BROADCAST_ADDRESS),
                        AUDIO_PORT
                    )
                )
            }.onFailure { Log.w(TAG, "发送对讲音频失败", it) }
        } else {
            val packetType = transportMode.packetType
            // Capture already runs on Dispatchers.IO. Sending inline preserves frame order
            // and applies backpressure instead of spawning an unbounded coroutine per frame.
            realtimePackets.broadcast(packetType, bytes)
        }
    }

    @Synchronized
    private fun stopCapture() {
        captureJob?.cancel()
        captureJob = null
        runCatching { audioRecord?.stop() }
        echoCanceler?.release()
        echoCanceler = null
        audioRecord?.release()
        audioRecord = null
    }

    private data class AudioFrame(
        val channel: String,
        val senderId: String,
        val sequence: Int,
        val payload: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AudioFrame

            if (sequence != other.sequence) return false
            if (channel != other.channel) return false
            if (senderId != other.senderId) return false
            if (!payload.contentEquals(other.payload)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = sequence
            result = 31 * result + channel.hashCode()
            result = 31 * result + senderId.hashCode()
            result = 31 * result + payload.contentHashCode()
            return result
        }
    }

    /**
     * Per-speaker reorder/jitter buffer. A few missing frames are concealed by fading the
     * last frame instead of inserting hard silence, which is much less noticeable for speech.
     */
    private class RemoteStream(private val fecEnabled: Boolean) {
        private val frames = TreeMap<Int, AudioFrame>()
        private val decoder = IntercomOpusDecoder()
        private var started = false
        private var expectedSequence: Int? = null

        @Synchronized
        fun offer(frame: AudioFrame) {
            expectedSequence?.let { if (frame.sequence < it) return }
            frames.putIfAbsent(frame.sequence, frame)
            // Retain the frames due to play soonest when the producer gets too far ahead.
            if (frames.size > JITTER_FRAMES) frames.pollLastEntry()
        }

        @Synchronized
        fun poll(): ShortArray? {
            if (!started) {
                if (frames.size < JITTER_START_FRAMES) return null
                started = true
                expectedSequence = frames.firstKey()
            }
            val expected = expectedSequence ?: return null
            frames.remove(expected)?.let { frame ->
                expectedSequence = expected + 1
                return runCatching { decoder.decode(frame.payload) }
                    .getOrElse { decoder.concealLoss() }
            }

            val following = frames[expected + 1]
            if (fecEnabled && following != null) {
                expectedSequence = expected + 1
                return runCatching { decoder.decode(following.payload, fec = true) }
                    .getOrElse { decoder.concealLoss() }
            }
            expectedSequence = expected + 1
            val concealed = runCatching { decoder.concealLoss() }.getOrNull()
            if (frames.isEmpty()) {
                started = false
                expectedSequence = null
            }
            return concealed
        }
    }

    private fun encodeFrame(
        channel: String,
        senderId: String,
        sequence: Int,
        payload: ByteArray
    ): ByteArray = ByteArrayOutputStream(MAX_PACKET_SIZE).use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(MAGIC)
            output.writeByte(PROTOCOL_VERSION)
            output.writeUTF(channel)
            output.writeUTF(senderId)
            output.writeInt(sequence)
            output.writeShort(payload.size)
            output.write(payload)
        }
        bytes.toByteArray()
    }

    private fun decodeFrame(bytes: ByteArray, length: Int): AudioFrame? = runCatching {
        DataInputStream(ByteArrayInputStream(bytes, 0, length)).use { input ->
            require(input.readInt() == MAGIC)
            require(input.readUnsignedByte() == PROTOCOL_VERSION)
            val channel = input.readUTF()
            val senderId = input.readUTF()
            val sequence = input.readInt()
            val payloadSize = input.readUnsignedShort()
            require(payloadSize in 1..IntercomAudioFormat.MAX_ENCODED_FRAME_BYTES)
            val payload = ByteArray(payloadSize)
            input.readFully(payload)
            AudioFrame(channel, senderId, sequence, payload)
        }
    }.getOrNull()

    private fun usesUnreliableUdp(): Boolean =
        transportMode == IntercomTransport.Nearby && connectionMode == ConnectionMode.WiFiLan

    @Suppress("DEPRECATION")
    private fun configureSpeakerRoute() {
        if (routingConfigured) return
        previousAudioMode = audioManager.mode
        previousSpeakerphone = audioManager.isSpeakerphoneOn
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.availableCommunicationDevices
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                ?.let(audioManager::setCommunicationDevice)
        } else {
            audioManager.isSpeakerphoneOn = true
        }
        routingConfigured = true
    }

    @Suppress("DEPRECATION")
    private fun restoreAudioRoute() {
        if (!routingConfigured) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        } else {
            audioManager.isSpeakerphoneOn = previousSpeakerphone
        }
        audioManager.mode = previousAudioMode
        routingConfigured = false
    }

    private companion object {
        const val TAG = "IntercomAudio"
        const val MAGIC = 0x57435054 // "WCPT"
        const val PROTOCOL_VERSION = 2
        const val AUDIO_PORT = 52_141
        const val BROADCAST_ADDRESS = "255.255.255.255"
        const val JITTER_FRAMES = 12
        const val JITTER_START_FRAMES = 5
        const val SOCKET_BUFFER_BYTES = 256 * 1_024
        const val MAX_PACKET_SIZE = 1_200
        const val OUTPUT_GAIN = 1.0
    }
}

enum class IntercomTransport(val packetType: Byte) {
    Nearby(PacketType.INTERCOM_AUDIO),
    PrivateChat(PacketType.LOCATION_INTERCOM_AUDIO)
}
