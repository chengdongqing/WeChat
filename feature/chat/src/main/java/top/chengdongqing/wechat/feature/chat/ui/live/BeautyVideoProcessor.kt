package top.chengdongqing.wechat.feature.chat.ui.live

import org.webrtc.JavaI420Buffer
import org.webrtc.VideoFrame
import org.webrtc.VideoProcessor
import org.webrtc.VideoSink
import org.webrtc.YuvHelper
import kotlin.math.pow

/**
 * 轻量实时美颜：通过亮度色调映射柔化高光和暗部，色彩通道保持原样。
 * 不依赖厂商相机特效，处理后的帧同时用于主播预览和 WebRTC 发送。
 */
class BeautyVideoProcessor : VideoProcessor {
    @Volatile
    private var strength = 0f
    private var sink: VideoSink? = null
    private var lookupStrength = -1
    private val lumaLookup = ByteArray(256)

    fun setStrength(value: Float) {
        strength = value.coerceIn(0f, 1f)
    }

    override fun setSink(sink: VideoSink?) {
        this.sink = sink
    }

    override fun onCapturerStarted(success: Boolean) = Unit

    override fun onCapturerStopped() = Unit

    override fun onFrameCaptured(frame: VideoFrame) {
        val amount = strength
        val target = sink ?: return
        if (amount <= 0.01f) {
            target.onFrame(frame)
            return
        }
        val source = frame.buffer.toI420()
        if (source == null) {
            target.onFrame(frame)
            return
        }
        val output = JavaI420Buffer.allocate(source.width, source.height)
        YuvHelper.I420Copy(
            source.dataY,
            source.strideY,
            source.dataU,
            source.strideU,
            source.dataV,
            source.strideV,
            output.dataY,
            source.width,
            output.dataU,
            (source.width + 1) / 2,
            output.dataV,
            (source.width + 1) / 2,
            source.width,
            source.height
        )
        applyToneMap(output, amount)
        source.release()
        val processed = VideoFrame(output, frame.rotation, frame.timestampNs)
        target.onFrame(processed)
        processed.release()
    }

    private fun applyToneMap(buffer: JavaI420Buffer, amount: Float) {
        val strengthKey = (amount * 100).toInt()
        if (lookupStrength != strengthKey) {
            lookupStrength = strengthKey
            for (value in 0..255) {
                val normalized = value / 255f
                // Gamma 提亮中间调，同时压住高光，增强档也不会整张画面发白。
                val softened = normalized.toDouble()
                    .pow((1.0 - amount * 0.38).coerceAtLeast(0.58))
                    .toFloat() + amount * 0.025f
                lumaLookup[value] = (softened * 255f).toInt()
                    .coerceIn(16, 235).toByte()
            }
        }
        val data = buffer.dataY
        val row = ByteArray(buffer.width)
        for (y in 0 until buffer.height) {
            val offset = y * buffer.strideY
            val read = data.duplicate().apply { position(offset) }
            read.get(row)
            for (x in row.indices) {
                row[x] = lumaLookup[row[x].toInt() and 0xff]
            }
            data.duplicate().apply { position(offset) }.put(row)
        }
    }
}
