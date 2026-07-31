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
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Screen-scoped low-latency LAN audio transport.
 *
 * Frames are 16 kHz mono PCM in 20 ms UDP packets. Each sender has a small
 * bounded jitter queue; the playback loop mixes one frame per active sender.
 */
@Singleton
class IntercomAudioEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val realtimePackets: RealtimePacketBus
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val remoteFrames = ConcurrentHashMap<String, Channel<ShortArray>>()
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
    private var noiseSuppressor: NoiseSuppressor? = null
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

    fun setPlaybackEnabled(enabled: Boolean) {
        audioTrack?.setVolume(if (enabled) 1f else 0f)
    }

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
        remoteFrames.values.forEach { it.close() }
        remoteFrames.clear()
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
        val queue = remoteFrames.getOrPut(frame.senderId) {
            Channel(capacity = JITTER_FRAMES)
        }
        if (queue.trySend(frame.samples).isFailure) {
            queue.tryReceive()
            queue.trySend(frame.samples)
        }
    }

    private fun startPlayback() {
        val minBuffer = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
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
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(max(minBuffer, FRAME_BYTES * 8))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        audioTrack = track
        track.play()
        playbackJob = scope.launch {
            val accumulator = IntArray(FRAME_SAMPLES)
            val mixed = ShortArray(FRAME_SAMPLES)
            while (isActive) {
                accumulator.fill(0)
                var streamCount = 0
                remoteFrames.values.forEach { queue ->
                    val samples = queue.tryReceive().getOrNull() ?: return@forEach
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
                    track.write(mixed, 0, mixed.size, AudioTrack.WRITE_BLOCKING)
                } else {
                    delay(FRAME_DURATION_MS)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startCapture() {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            max(minBuffer, FRAME_BYTES * 4)
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
        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor =
                NoiseSuppressor.create(recorder.audioSessionId)?.apply { enabled = true }
        }
        recorder.startRecording()
        captureJob = scope.launch {
            val samples = ShortArray(FRAME_SAMPLES)
            while (isActive && recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val count = recorder.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                if (count == samples.size) {
                    for (index in samples.indices) {
                        samples[index] = (samples[index] * INPUT_GAIN)
                            .toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                            .toShort()
                    }
                    sendFrame(samples)
                }
            }
        }
    }

    private fun sendFrame(samples: ShortArray) {
        val channel = channelId ?: return
        val bytes = encodeFrame(
            channel = channel,
            senderId = profileRepository.requireUserId(),
            sequence = sequence.incrementAndGet(),
            samples = samples
        )
        if (transportMode == IntercomTransport.Nearby &&
            connectionMode == ConnectionMode.WiFiLan
        ) {
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
            scope.launch {
                realtimePackets.broadcast(packetType, bytes)
            }
        }
    }

    @Synchronized
    private fun stopCapture() {
        captureJob?.cancel()
        captureJob = null
        runCatching { audioRecord?.stop() }
        echoCanceler?.release()
        noiseSuppressor?.release()
        echoCanceler = null
        noiseSuppressor = null
        audioRecord?.release()
        audioRecord = null
    }

    private data class AudioFrame(
        val channel: String,
        val senderId: String,
        val samples: ShortArray
    )

    private fun encodeFrame(
        channel: String,
        senderId: String,
        sequence: Int,
        samples: ShortArray
    ): ByteArray = ByteArrayOutputStream(MAX_PACKET_SIZE).use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(MAGIC)
            output.writeByte(PROTOCOL_VERSION)
            output.writeUTF(channel)
            output.writeUTF(senderId)
            output.writeInt(sequence)
            output.writeShort(samples.size)
            samples.forEach { output.writeShort(it.toInt()) }
        }
        bytes.toByteArray()
    }

    private fun decodeFrame(bytes: ByteArray, length: Int): AudioFrame? = runCatching {
        DataInputStream(ByteArrayInputStream(bytes, 0, length)).use { input ->
            require(input.readInt() == MAGIC)
            require(input.readUnsignedByte() == PROTOCOL_VERSION)
            val channel = input.readUTF()
            val senderId = input.readUTF()
            input.readInt() // Sequence is reserved for future loss/reorder metrics.
            val sampleCount = input.readUnsignedShort()
            require(sampleCount == FRAME_SAMPLES)
            val samples = ShortArray(sampleCount) { input.readShort() }
            AudioFrame(channel, senderId, samples)
        }
    }.getOrNull()

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
        const val PROTOCOL_VERSION = 1
        const val AUDIO_PORT = 52_141
        const val BROADCAST_ADDRESS = "255.255.255.255"
        const val SAMPLE_RATE = 16_000
        const val FRAME_DURATION_MS = 20L
        const val FRAME_SAMPLES = 320
        const val FRAME_BYTES = FRAME_SAMPLES * 2
        const val JITTER_FRAMES = 5
        const val MAX_PACKET_SIZE = 1_200
        const val INPUT_GAIN = 1.6f
        const val OUTPUT_GAIN = 1.25
    }
}

enum class IntercomTransport(val packetType: Byte) {
    Nearby(PacketType.INTERCOM_AUDIO),
    PrivateChat(PacketType.LOCATION_INTERCOM_AUDIO)
}
