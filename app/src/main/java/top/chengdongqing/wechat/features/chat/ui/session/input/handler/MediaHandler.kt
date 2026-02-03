package top.chengdongqing.wechat.features.chat.ui.session.input.handler

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
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
import top.chengdongqing.wechat.core.designsystem.components.media.model.VisualMediaType
import top.chengdongqing.wechat.core.designsystem.components.media.picker.rememberPickMediasLauncher
import top.chengdongqing.wechat.core.util.prepareMediaResource
import top.chengdongqing.wechat.data.model.MessageContent

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
     * 处理媒体选择结果
     */
    fun handleMediaSelection(items: Array<MediaItem>) {
        // 切换回文本模式
        onModeSwitch()

        // 转换为消息内容
        val contents = items.map { item ->
            if (item.isImage) {
                MessageContent.Image(
                    uri = item.uri,
                    mimeType = item.mimeType,
                    filename = item.filename,
                    width = item.width,
                    height = item.height
                )
            } else {
                MessageContent.Video(
                    uri = item.uri,
                    mimeType = item.mimeType,
                    filename = item.filename,
                    width = item.width,
                    height = item.height,
                    duration = item.duration
                )
            }
        }

        // 批量发送
        scope.launch {
            contents.forEach { content ->
                onSendMessage(content, null)
                delay(50) // 避免发送过快
            }
        }
    }

    /**
     * 处理相机拍摄结果
     */
    fun handleCameraCapture(mediaUri: Uri, isImage: Boolean) {
        scope.launch {
            val resource = prepareMediaResource(context, mediaUri) ?: return@launch

            val content = if (isImage) {
                MessageContent.Image(
                    uri = mediaUri,
                    mimeType = resource.mimeType,
                    filename = resource.filename,
                    width = resource.width,
                    height = resource.height
                )
            } else {
                MessageContent.Video(
                    uri = mediaUri,
                    mimeType = resource.mimeType,
                    filename = resource.filename,
                    width = resource.width,
                    height = resource.height,
                    duration = resource.duration
                )
            }
            onSendMessage(content, null)
        }
    }

    /**
     * 处理系统相机拍照结果
     */
    fun handleSystemPictureCapture(success: Boolean, capturedUri: Uri?) {
        if (!success || capturedUri == null) return

        scope.launch {
            val resource = prepareMediaResource(context, capturedUri) ?: return@launch

            val content = MessageContent.Image(
                uri = capturedUri,
                mimeType = resource.mimeType,
                filename = resource.filename,
                width = resource.width,
                height = resource.height
            )
            onSendMessage(content, null)
        }
    }

    /**
     * 处理系统相机录像结果
     */
    fun handleSystemVideoCapture(success: Boolean, capturedUri: Uri?) {
        if (!success || capturedUri == null) return

        scope.launch {
            val resource = prepareMediaResource(context, capturedUri) ?: return@launch

            val content = MessageContent.Video(
                uri = capturedUri,
                mimeType = resource.mimeType,
                filename = resource.filename,
                width = resource.width,
                height = resource.height,
                duration = resource.duration
            )
            onSendMessage(content, null)
        }
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
    val mediaPicker = rememberPickMediasLauncher { items ->
        mediaHandler.handleMediaSelection(items)
    }

    // 相机启动器
    val camera = rememberCameraLauncher { uri, mediaType ->
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

    return remember(mediaPicker, camera, takePicture, captureVideo) {
        MediaLaunchers(
            mediaPicker = mediaPicker,
            camera = camera,
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
    val mediaPicker: (VisualMediaType, Int) -> Unit,
    val camera: (VisualMediaType) -> Unit,
    val takePicture: ManagedActivityResultLauncher<Uri, Boolean>,
    val captureVideo: ManagedActivityResultLauncher<Uri, Boolean>,
    val capturedUri: () -> Uri?,
    val setCapturedUri: (Uri?) -> Unit
)