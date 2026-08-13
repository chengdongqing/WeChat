package top.chengdongqing.wechat.feature.chat.ui.session.input.handler

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.camera.rememberCameraLauncher
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.file.PrivateFileManager
import top.chengdongqing.wechat.core.file.deleteFileByUri
import top.chengdongqing.wechat.core.file.getFileMetadata
import top.chengdongqing.wechat.core.media.MediaPrivacyProcessor
import top.chengdongqing.wechat.core.media.picker.rememberMediaPickerLauncher
import top.chengdongqing.wechat.core.model.MessageType
import top.chengdongqing.wechat.core.util.randomUUID
import kotlin.time.Duration.Companion.milliseconds

/**
 * 媒体处理器
 *
 * 负责处理图片、视频、相机等媒体相关操作
 */
class MediaHandler(
    private val context: Context,
    private val scope: CoroutineScope,
    private val privateFileManager: PrivateFileManager,
    private val onModeChange: () -> Unit,
    private val onSendMessage: (MessageContent) -> Unit
) {
    /**
     * 将媒体 Uri 处理并发送
     */
    private suspend fun processAndSend(
        uri: Uri,
        isFromCapture: Boolean = false,
        albumId: String? = null,
        albumIndex: Int = 0,
        albumSize: Int = 1,
        original: Boolean = false
    ) {
        val sourceMetadata = context.getFileMetadata(uri) ?: return
        val isImage = sourceMetadata.isImage
        val messageType = if (isImage) MessageType.Image else MessageType.Video

        // 原图只跳过压缩；所有模式均生成不含位置、设备、拍摄时间等信息的新文件。
        val processedFile =
            MediaPrivacyProcessor(context).process(uri, sourceMetadata.mimeType, original)
        val processedUri = Uri.fromFile(processedFile)
        val metadata = context.getFileMetadata(processedUri) ?: run {
            processedFile.delete()
            return
        }

        // 拷贝到私有目录持久化保存
        val localPath = privateFileManager.saveMedia(
            messageType = messageType,
            sourceFile = processedFile
        ).getOrThrow()
        processedFile.delete()

        // 清理临时文件
        if (isFromCapture) {
            context.deleteFileByUri(uri)
        }

        // 构建消息对象
        val content = if (isImage) {
            MessageContent.Image(
                localPath = localPath,
                mimeType = metadata.mimeType,
                filename = metadata.filename,
                width = metadata.width,
                height = metadata.height,
                size = metadata.size, albumId = albumId,
                albumIndex = albumIndex,
                albumSize = albumSize
            )
        } else {
            MessageContent.Video(
                localPath = localPath,
                mimeType = metadata.mimeType,
                filename = metadata.filename,
                width = metadata.width,
                height = metadata.height,
                duration = metadata.duration,
                size = metadata.size, albumId = albumId,
                albumIndex = albumIndex,
                albumSize = albumSize
            )
        }

        onSendMessage(content)
    }

    fun handleMediaSelection(uris: List<Uri>, merge: Boolean = false, original: Boolean = false) {
        onModeChange()
        scope.launch {
            val albumId = if (merge && uris.size >= 3) randomUUID() else null
            uris.forEachIndexed { index, uri ->
                processAndSend(
                    uri = uri,
                    albumId = albumId,
                    albumIndex = index,
                    albumSize = if (albumId != null) uris.size else 1,
                    original = original
                )
                if (index < uris.lastIndex) {
                    delay(50.milliseconds)
                }
            }
        }
    }

    fun handleCameraCapture(uri: Uri?) {
        uri?.let {
            scope.launch {
                processAndSend(uri, isFromCapture = true)
            }
        }
    }
}

@Composable
fun rememberMediaHandler(
    privateFileManager: PrivateFileManager,
    onModeChange: () -> Unit,
    onSendMessage: (MessageContent) -> Unit
): MediaHandler {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()

    return remember(scope) {
        MediaHandler(
            context = context,
            scope = scope,
            privateFileManager = privateFileManager,
            onModeChange = onModeChange,
            onSendMessage = onSendMessage
        )
    }
}

/**
 * 媒体启动器集合
 *
 * 封装所有与媒体相关的Activity启动器
 */
@Composable
fun rememberMediaLaunchers(
    mediaHandler: MediaHandler
): MediaLaunchers {
    var capturedUri by remember { mutableStateOf<Uri?>(null) }

    // 媒体选择器
    val launchMediaPicker =
        rememberMediaPickerLauncher { result ->
            mediaHandler.handleMediaSelection(
                result.items.map { it.uri },
                result.merge,
                result.original
            )
        }

    // 系统媒体选择器
    val launchSystemMediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { mediaUris: List<Uri> ->
        mediaHandler.handleMediaSelection(mediaUris)
    }

    // 相机启动器
    val launchCamera = rememberCameraLauncher { uri, _ ->
        mediaHandler.handleCameraCapture(uri)
    }

    // 系统拍照启动器
    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            mediaHandler.handleCameraCapture(capturedUri)
        }
    }

    // 系统录像启动器
    val captureVideo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            mediaHandler.handleCameraCapture(capturedUri)
        }
    }

    return remember(launchMediaPicker, launchCamera, takePicture, captureVideo) {
        MediaLaunchers(
            launchMediaPicker = launchMediaPicker,
            launchSystemMediaPicker = launchSystemMediaPicker,
            launchCamera = launchCamera,
            takePicture = takePicture,
            captureVideo = captureVideo,
            capturedUri = { capturedUri },
            setCapturedUri = { capturedUri = it }
        )
    }
}
