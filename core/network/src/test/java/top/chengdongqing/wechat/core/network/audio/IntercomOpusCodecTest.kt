package top.chengdongqing.wechat.core.network.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class IntercomOpusCodecTest {
    @Test
    fun encodeAndDecodeProducesOnePcmFrame() {
        val encoder = IntercomOpusEncoder(enableFec = false)
        val decoder = IntercomOpusDecoder()
        val encoded = encoder.encode(toneFrame(440.0))

        val decoded = decoder.decode(encoded)

        assertTrue(encoded.size < IntercomAudioFormat.FRAME_BYTES)
        assertEquals(IntercomAudioFormat.FRAME_SAMPLES, decoded.size)
        assertTrue(decoded.any { it.toInt() != 0 })
    }

    @Test
    fun decoderCanConcealMissingFrame() {
        val encoder = IntercomOpusEncoder(enableFec = true)
        val decoder = IntercomOpusDecoder()
        decoder.decode(encoder.encode(toneFrame(330.0)))

        val concealed = decoder.concealLoss()

        assertEquals(IntercomAudioFormat.FRAME_SAMPLES, concealed.size)
    }

    private fun toneFrame(frequency: Double): ShortArray =
        ShortArray(IntercomAudioFormat.FRAME_SAMPLES) { index ->
            (sin(2.0 * PI * frequency * index / IntercomAudioFormat.SAMPLE_RATE) * 8_000)
                .toInt()
                .toShort()
        }
}
