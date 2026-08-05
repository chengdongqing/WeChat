package top.chengdongqing.wechat.feature.moments.ui.post

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.RgbFilter
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File
import java.util.UUID

enum class MomentVideoFilter(val title: String) {
    Original("原图"),
    Vivid("鲜明"),
    Clear("清透"),
    Warm("暖阳"),
    Cool("冷调"),
    Cinema("电影"),
    Mono("黑白"),
    Retro("复古")
}

data class MomentVideoEdit(
    val startMs: Long,
    val endMs: Long,
    val filter: MomentVideoFilter,
    val voiceOver: Uri? = null
)

@OptIn(UnstableApi::class)
class VideoExportManager(private val context: Context) {
    private var transformer: Transformer? = null

    fun export(
        source: Uri,
        edit: MomentVideoEdit,
        onProgress: (Int) -> Unit,
        onComplete: (Result<Uri>) -> Unit
    ) {
        val output = File(
            File(context.filesDir, "moments").apply { mkdirs() },
            "edited_${UUID.randomUUID()}.mp4"
        )
        val clipping = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(edit.startMs)
            .setEndPositionMs(edit.endMs)
            .build()
        val mediaItem = MediaItem.Builder().setUri(source)
            .setClippingConfiguration(clipping).build()
        val edited = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), edit.filter.effects()))
            .build()
        val composition = edit.voiceOver?.let { voiceUri ->
            val voiceMedia = MediaItem.Builder().setUri(voiceUri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setEndPositionMs(edit.endMs - edit.startMs)
                        .build()
                ).build()
            val voice = EditedMediaItem.Builder(voiceMedia).build()
            Composition.Builder(
                listOf(
                    EditedMediaItemSequence.withAudioAndVideoFrom(listOf(edited)),
                    EditedMediaItemSequence.withAudioFrom(listOf(voice))
                )
            ).build()
        }
        transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: androidx.media3.transformer.Composition, result: ExportResult) {
                    onProgress(100)
                    onComplete(Result.success(Uri.fromFile(output)))
                }

                override fun onError(
                    composition: androidx.media3.transformer.Composition,
                    result: ExportResult,
                    exception: ExportException
                ) {
                    output.delete()
                    onComplete(Result.failure(exception))
                }
            })
            .build()
            .also {
                if (composition == null) it.start(edited, output.absolutePath)
                else it.start(composition, output.absolutePath)
            }
    }

    fun cancel() = transformer?.cancel()
}

@OptIn(UnstableApi::class)
private fun MomentVideoFilter.effects(): List<Effect> = when (this) {
    MomentVideoFilter.Original -> emptyList()
    MomentVideoFilter.Vivid -> listOf(HslAdjustment.Builder().adjustSaturation(28f).build())
    MomentVideoFilter.Clear -> listOf(HslAdjustment.Builder().adjustLightness(8f).adjustSaturation(8f).build())
    MomentVideoFilter.Warm -> listOf(HslAdjustment.Builder().adjustHue(8f).adjustSaturation(14f).build())
    MomentVideoFilter.Cool -> listOf(HslAdjustment.Builder().adjustHue(-10f).adjustSaturation(8f).build())
    MomentVideoFilter.Cinema -> listOf(HslAdjustment.Builder().adjustSaturation(-18f).adjustLightness(-7f).build())
    MomentVideoFilter.Mono -> listOf(RgbFilter.createGrayscaleFilter())
    MomentVideoFilter.Retro -> listOf(HslAdjustment.Builder().adjustHue(14f).adjustSaturation(-12f).build())
}
