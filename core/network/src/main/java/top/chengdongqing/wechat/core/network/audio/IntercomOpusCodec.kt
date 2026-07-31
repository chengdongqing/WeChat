package top.chengdongqing.wechat.core.network.audio

import io.github.jaredmdobson.concentus.OpusApplication
import io.github.jaredmdobson.concentus.OpusDecoder
import io.github.jaredmdobson.concentus.OpusEncoder
import io.github.jaredmdobson.concentus.OpusSignal

/** Stateful 16 kHz mono Opus encoder used for one local talk spurt. */
internal class IntercomOpusEncoder(enableFec: Boolean) {
    private val encoder = OpusEncoder(
        IntercomAudioFormat.SAMPLE_RATE,
        IntercomAudioFormat.CHANNELS,
        OpusApplication.OPUS_APPLICATION_VOIP
    ).apply {
        bitrate = IntercomAudioFormat.BITRATE
        complexity = IntercomAudioFormat.COMPLEXITY
        signalType = OpusSignal.OPUS_SIGNAL_VOICE
        useVBR = true
        useInbandFEC = enableFec
        packetLossPercent = if (enableFec) IntercomAudioFormat.EXPECTED_PACKET_LOSS else 0
        useDTX = false
    }

    fun encode(samples: ShortArray): ByteArray {
        require(samples.size == IntercomAudioFormat.FRAME_SAMPLES)
        val output = ByteArray(IntercomAudioFormat.MAX_ENCODED_FRAME_BYTES)
        val count = encoder.encode(
            samples,
            0,
            samples.size,
            output,
            0,
            output.size
        )
        return output.copyOf(count)
    }
}

/** Stateful decoder; one instance is required for each remote speaker. */
internal class IntercomOpusDecoder {
    private val decoder = OpusDecoder(
        IntercomAudioFormat.SAMPLE_RATE,
        IntercomAudioFormat.CHANNELS
    )

    fun decode(payload: ByteArray, fec: Boolean = false): ShortArray {
        val output = ShortArray(IntercomAudioFormat.FRAME_SAMPLES)
        val count = decoder.decode(
            payload,
            0,
            payload.size,
            output,
            0,
            output.size,
            fec
        )
        return output.withDecodedSize(count)
    }

    /** Ask Opus to synthesize one missing frame from its decoder history. */
    fun concealLoss(): ShortArray {
        val output = ShortArray(IntercomAudioFormat.FRAME_SAMPLES)
        val count = decoder.decode(null, 0, 0, output, 0, output.size, false)
        return output.withDecodedSize(count)
    }

    private fun ShortArray.withDecodedSize(count: Int): ShortArray = when {
        count == size -> this
        count <= 0 -> ShortArray(size)
        else -> copyOf(count).copyOf(size)
    }
}

internal object IntercomAudioFormat {
    const val SAMPLE_RATE = 16_000
    const val CHANNELS = 1
    const val FRAME_SAMPLES = 320
    const val FRAME_BYTES = FRAME_SAMPLES * 2
    const val BITRATE = 24_000
    const val COMPLEXITY = 5
    const val EXPECTED_PACKET_LOSS = 10
    const val MAX_ENCODED_FRAME_BYTES = 400
}
