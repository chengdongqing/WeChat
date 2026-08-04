package top.chengdongqing.wechat.core.network.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ChatOpusFileTest {
    @Test
    fun roundTripProducesWavWithExpectedPcmLength() {
        val opus = File.createTempFile("voice_", ".wopus")
        val wav = File.createTempFile("voice_", ".wav")
        try {
            val encoder = IntercomOpusEncoder(enableFec = false)
            ChatOpusFileWriter(opus).use { writer ->
                repeat(5) {
                    writer.write(encoder.encode(ShortArray(IntercomAudioFormat.FRAME_SAMPLES)))
                }
            }

            assertTrue(ChatOpusFileWriter.isChatOpus(opus))
            ChatOpusFileWriter.decodeToWav(opus, wav)
            assertEquals(
                44L + 5L * IntercomAudioFormat.FRAME_SAMPLES * 2L,
                wav.length()
            )
        } finally {
            opus.delete()
            wav.delete()
        }
    }
}
