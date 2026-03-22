package top.chengdongqing.wechat.features.chat.ui.session.input.handler

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import top.chengdongqing.wechat.core.media.model.VisualMediaType

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