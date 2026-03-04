package top.chengdongqing.wechat.features.chat.ui.session.input.handler

import android.content.Context
import android.net.Uri
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
import top.chengdongqing.wechat.core.designsystem.components.media.picker.rememberPickMediasLauncher
import top.chengdongqing.wechat.core.util.copyUriToPrivateDir
import top.chengdongqing.wechat.core.util.deleteFileByUri
import top.chengdongqing.wechat.core.util.getFileConfig
import top.chengdongqing.wechat.core.util.getFileMetadata
import top.chengdongqing.wechat.data.model.MessageType
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent

/**
 * 媒体处理器
 *
 * 负责处理图片、视频、相机等媒体相关操作
 */
class MediaHandler(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSendMessage: (MessageContent) -> Unit,
    private val onModeSwitch: () -> Unit
) {
    /**
     * 将媒体 Uri 处理并发送
     */
    private suspend fun processAndSend(uri: Uri, isFromCapture: Boolean = false) {
        // 获取元数据
        val metadata = context.getFileMetadata(uri) ?: return
        val isImage = metadata.isImage
        val messageType = if (isImage) MessageType.Image else MessageType.Video

        // 拷贝到私有目录持久化保存
        val localPath = context.copyUriToPrivateDir(
            uri = uri,
            subDir = messageType.getFileConfig().dirName
        ) ?: return

        // 清除临时文件
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
                size = metadata.size
            )
        } else {
            MessageContent.Video(
                localPath = localPath,
                mimeType = metadata.mimeType,
                filename = metadata.filename,
                width = metadata.width,
                height = metadata.height,
                duration = metadata.duration,
                size = metadata.size
            )
        }

        onSendMessage(content)
    }

    fun handleMediaSelection(uris: List<Uri>) {
        onModeSwitch()
        scope.launch {
            uris.forEachIndexed { index, uri ->
                processAndSend(uri)
                if (index < uris.size - 1) delay(50)
            }
        }
    }

    fun handleCameraCapture(uri: Uri?) {
        uri?.let {
            scope.launch { processAndSend(uri, isFromCapture = true) }
        }
    }
}

@Composable
fun rememberMediaHandler(
    context: Context,
    scope: CoroutineScope,
    onSendMessage: (MessageContent) -> Unit,
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
        mediaHandler.handleMediaSelection(items.map { it.uri })
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
        if (success) mediaHandler.handleCameraCapture(capturedUri)
    }

    // 系统录像启动器
    val captureVideo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) mediaHandler.handleCameraCapture(capturedUri)
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