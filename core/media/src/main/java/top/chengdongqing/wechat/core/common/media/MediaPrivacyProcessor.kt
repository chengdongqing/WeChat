package top.chengdongqing.wechat.core.common.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Produces shareable media with identifying metadata removed.
 */
class MediaPrivacyProcessor(private val context: Context) {
    suspend fun process(source: Uri, mimeType: String, original: Boolean): File =
        if (mimeType.startsWith("image/")) processImage(source, original)
        else processVideo(source, original)

    private suspend fun processImage(source: Uri, original: Boolean): File =
        withContext(Dispatchers.IO) {
            val output = tempFile(if (original) source.extensionFromMime() else "jpg")
            if (original) {
                context.contentResolver.openInputStream(source)!!.use { input ->
                    output.outputStream().use(input::copyTo)
                }
                removeExif(output)
                return@withContext output
            }

            val bitmap =
                context.contentResolver.openInputStream(source)!!.use(BitmapFactory::decodeStream)
                    ?: error("无法解码图片")
            val orientation = context.contentResolver.openInputStream(source)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
            val rotated = bitmap.applyOrientation(orientation)
            val maxSide = maxOf(rotated.width, rotated.height)
            val scaled = if (maxSide > 2560) {
                val ratio = 2560f / maxSide
                rotated.scale((rotated.width * ratio).toInt(), (rotated.height * ratio).toInt())
            } else rotated
            FileOutputStream(output).use { scaled.compress(Bitmap.CompressFormat.JPEG, 82, it) }
            if (scaled !== rotated) scaled.recycle()
            if (rotated !== bitmap) rotated.recycle()
            bitmap.recycle()
            output
        }

    private suspend fun processVideo(source: Uri, original: Boolean): File =
        if (original) withContext(Dispatchers.IO) { remuxVideo(source) } else transcodeVideo(source)

    private fun remuxVideo(source: Uri): File {
        val output = tempFile("mp4")
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(context, source, null)
            muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val retriever = MediaMetadataRetriever()
            runCatching {
                retriever.setDataSource(context, source)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                    ?.toIntOrNull()?.let(muxer::setOrientationHint)
            }
            retriever.release()
            val trackMap = IntArray(extractor.trackCount) { -1 }
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    trackMap[i] = muxer.addTrack(format)
                    extractor.selectTrack(i)
                }
            }
            muxer.start()
            val buffer = ByteBuffer.allocate(2 * 1024 * 1024)
            val info = android.media.MediaCodec.BufferInfo()
            while (true) {
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                val target = trackMap[extractor.sampleTrackIndex]
                if (target >= 0) {
                    info.set(0, size, extractor.sampleTime, extractor.sampleFlags)
                    muxer.writeSampleData(target, buffer, info)
                }
                extractor.advance()
            }
            clearMp4Timestamps(output)
            return output
        } catch (e: Exception) {
            output.delete()
            throw e
        } finally {
            extractor.release()
            runCatching { muxer?.stop() }
            muxer?.release()
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private suspend fun transcodeVideo(source: Uri): File =
        suspendCancellableCoroutine { continuation ->
            val output = tempFile("mp4")
            // A video effect makes Media3 decode and encode instead of optimizing compatible H.264
            // input into a near-instant transmux. Capping the encoded height also reduces large videos.
            val sourceHeight = MediaMetadataRetriever().run {
                try {
                    setDataSource(context, source)
                    extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                } finally {
                    release()
                }
            } ?: 1080
            val targetHeight = minOf(sourceHeight, 1080).coerceAtLeast(2).let { it - it % 2 }
            val editedMedia = EditedMediaItem.Builder(MediaItem.fromUri(source))
                .setEffects(
                    Effects(
                        emptyList(),
                        listOf(Presentation.createForHeight(targetHeight))
                    )
                )
                .build()
            val encoderFactory = DefaultEncoderFactory.Builder(context)
                .setRequestedVideoEncoderSettings(
                    VideoEncoderSettings.Builder().setBitrate(4_000_000).build()
                )
                .setEnableFallback(true)
                .build()
            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .setEncoderFactory(encoderFactory)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(
                        composition: androidx.media3.transformer.Composition,
                        result: ExportResult
                    ) {
                        runCatching { clearMp4Timestamps(output) }
                            .onSuccess { if (continuation.isActive) continuation.resume(output) }
                            .onFailure {
                                output.delete()
                                if (continuation.isActive) continuation.resumeWith(Result.failure(it))
                            }
                    }

                    override fun onError(
                        composition: androidx.media3.transformer.Composition,
                        result: ExportResult,
                        exception: ExportException
                    ) {
                        output.delete()
                        if (continuation.isActive) continuation.resumeWith(Result.failure(exception))
                    }
                }).build()
            continuation.invokeOnCancellation { transformer.cancel(); output.delete() }
            transformer.start(editedMedia, output.absolutePath)
        }

    private fun removeExif(file: File) {
        val exif = ExifInterface(file)
        PRIVACY_TAGS.forEach { exif.setAttribute(it, null) }
        exif.saveAttributes()
    }

    /**
     * MP4 stores creation/modification times in movie, track and media headers. Android's muxer
     * may preserve or generate these even when all user metadata has otherwise been omitted.
     */
    private fun clearMp4Timestamps(file: File) {
        RandomAccessFile(file, "rw").use { mp4 ->
            fun visitBoxes(start: Long, end: Long) {
                var position = start
                while (position + 8 <= end) {
                    mp4.seek(position)
                    val size32 = mp4.readInt().toLong() and 0xffffffffL
                    val typeBytes = ByteArray(4).also(mp4::readFully)
                    val type = String(typeBytes, Charsets.ISO_8859_1)
                    val headerSize: Long
                    val boxSize: Long
                    when (size32) {
                        0L -> {
                            headerSize = 8; boxSize = end - position
                        }

                        1L -> {
                            headerSize = 16; boxSize = mp4.readLong()
                        }

                        else -> {
                            headerSize = 8; boxSize = size32
                        }
                    }
                    if (boxSize < headerSize || position + boxSize > end) break
                    val payload = position + headerSize
                    if (type == "mvhd" || type == "tkhd" || type == "mdhd") {
                        mp4.seek(payload)
                        val version = mp4.readUnsignedByte()
                        // Skip the remaining FullBox flags, then erase creation and modification.
                        mp4.seek(payload + 4)
                        val timestampBytes = if (version == 1) 16 else 8
                        mp4.write(ByteArray(timestampBytes))
                    } else if (type in MP4_CONTAINER_BOXES) {
                        visitBoxes(payload, position + boxSize)
                    }
                    position += boxSize
                }
            }
            visitBoxes(0, mp4.length())
        }
    }

    private fun tempFile(extension: String) = File(
        File(context.cacheDir, "sanitized_media").apply { mkdirs() },
        "media_${UUID.randomUUID()}.$extension"
    )

    private fun Uri.extensionFromMime(): String = when (context.contentResolver.getType(this)) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/heic", "image/heif" -> "heic"
        else -> "jpg"
    }

    private fun Bitmap.applyOrientation(orientation: Int): Bitmap {
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    postScale(-1f, 1f); postRotate(270f)
                }

                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    postScale(-1f, 1f); postRotate(90f)
                }
            }
        }
        return if (matrix.isIdentity) this else Bitmap.createBitmap(
            this,
            0,
            0,
            width,
            height,
            matrix,
            true
        )
    }

    private companion object {
        val MP4_CONTAINER_BOXES = setOf("moov", "trak", "mdia", "minf", "stbl", "edts", "dinf")
        val PRIVACY_TAGS = arrayOf(
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_DATETIME_ORIGINAL,
            ExifInterface.TAG_OFFSET_TIME,
            ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
            ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_ALTITUDE,
            ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_TIMESTAMP,
            ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_GPS_PROCESSING_METHOD,
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_SOFTWARE,
            ExifInterface.TAG_ARTIST,
            ExifInterface.TAG_COPYRIGHT,
            ExifInterface.TAG_IMAGE_DESCRIPTION,
            ExifInterface.TAG_USER_COMMENT,
            ExifInterface.TAG_CAMERA_OWNER_NAME,
            ExifInterface.TAG_BODY_SERIAL_NUMBER,
            ExifInterface.TAG_LENS_MAKE,
            ExifInterface.TAG_LENS_MODEL,
            ExifInterface.TAG_LENS_SERIAL_NUMBER,
            ExifInterface.TAG_XMP
        )
    }
}
