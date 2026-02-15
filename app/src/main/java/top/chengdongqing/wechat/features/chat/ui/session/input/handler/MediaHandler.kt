package top.chengdongqing.wechat.features.chat.ui.session.input.handler

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.components.camera.rememberCameraLauncher
import top.chengdongqing.wechat.core.designsystem.components.media.model.MediaItem
import top.chengdongqing.wechat.core.designsystem.components.media.model.MediaType
import top.chengdongqing.wechat.core.designsystem.components.media.model.VisualMediaType
import top.chengdongqing.wechat.core.designsystem.components.media.picker.rememberPickMediasLauncher
import top.chengdongqing.wechat.core.util.copyUriToPrivateDir
import top.chengdongqing.wechat.core.util.prepareMediaResource
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import java.io.File

/**
 * 媒体处理器
 *
 * 负责处理图片、视频、相机等媒体相关操作
 */
class MediaHandler(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSendMessage: (MessageContent, (() -> Unit)?) -> Unit,
    private val onModeSwitch: () -> Unit
) {
    /**
     * 核心逻辑：将任何来源的媒体（Uri 或 MediaItem）处理并发送
     */
    private suspend fun processAndSend(uri: Uri, isImage: Boolean, mediaItem: MediaItem? = null) {
        // 获取元数据
        val resource = prepareMediaResource(context, uri) ?: return
        val mediaType = if (isImage) MediaType.Image else MediaType.Video

        // 拷贝文件
        val localPath = context.copyUriToPrivateDir(uri, mediaType) ?: return
        val fileSize = File(localPath).length()

        // 构建消息对象
        val content = if (isImage) {
            MessageContent.Image(
                localPath = localPath,
                mimeType = resource.mimeType,
                filename = resource.filename,
                width = mediaItem?.width ?: resource.width,
                height = mediaItem?.height ?: resource.height,
                size = if (fileSize > 0) fileSize else resource.size
            )
        } else {
            MessageContent.Video(
                localPath = localPath,
                mimeType = resource.mimeType,
                filename = resource.filename,
                width = mediaItem?.width ?: resource.width,
                height = mediaItem?.height ?: resource.height,
                duration = mediaItem?.duration ?: resource.duration,
                size = if (fileSize > 0) fileSize else resource.size
            )
        }

        onSendMessage(content, null)
    }

    // --- 对外接口 ---

    fun handleMediaSelection(items: Array<MediaItem>) {
        onModeSwitch()
        scope.launch {
            items.forEachIndexed { index, item ->
                processAndSend(item.uri, item.isImage, item)
                if (index < items.size - 1) delay(50)
            }
        }
    }

    fun handleMediaSelection(uris: List<Uri>) {
        onModeSwitch()
        scope.launch {
            uris.forEachIndexed { index, uri ->
                val mimeType = context.contentResolver.getType(uri) ?: ""
                processAndSend(uri, mimeType.startsWith("image"))
                if (index < uris.size - 1) delay(50)
            }
        }
    }

    fun handleCameraCapture(uri: Uri, isImage: Boolean) {
        scope.launch { processAndSend(uri, isImage) }
    }

    fun handleSystemPictureCapture(success: Boolean, uri: Uri?) {
        if (success && uri != null) handleCameraCapture(uri, true)
    }

    fun handleSystemVideoCapture(success: Boolean, uri: Uri?) {
        if (success && uri != null) handleCameraCapture(uri, false)
    }
}

@Composable
fun rememberMediaHandler(
    context: Context,
    scope: CoroutineScope,
    onSendMessage: (MessageContent, (() -> Unit)?) -> Unit,
    onModeSwitch: () -> Unit
): MediaHandler {
    return remember(context, scope) {
        MediaHandler(context, scope, onSendMessage, onModeSwitch)
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
    val launchMediaPicker = rememberPickMediasLauncher { items ->
        mediaHandler.handleMediaSelection(items)
    }

    // 系统媒体选择器
    val launchSystemMediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { mediaUris: List<Uri> ->
        mediaHandler.handleMediaSelection(mediaUris)
    }

    // 相机启动器
    val launchCamera = rememberCameraLauncher { uri, mediaType ->
        mediaHandler.handleCameraCapture(uri, mediaType.isImage)
    }

    // 系统拍照启动器
    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        mediaHandler.handleSystemPictureCapture(success, capturedUri)
    }

    // 系统录像启动器
    val captureVideo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        mediaHandler.handleSystemVideoCapture(success, capturedUri)
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

/**
 * 媒体启动器集合
 */
data class MediaLaunchers(
    val launchMediaPicker: (VisualMediaType, Int) -> Unit,
    val launchSystemMediaPicker: ManagedActivityResultLauncher<PickVisualMediaRequest, List<Uri>>,
    val launchCamera: (VisualMediaType) -> Unit,
    val takePicture: ManagedActivityResultLauncher<Uri, Boolean>,
    val captureVideo: ManagedActivityResultLauncher<Uri, Boolean>,
    val capturedUri: () -> Uri?,
    val setCapturedUri: (Uri?) -> Unit
)