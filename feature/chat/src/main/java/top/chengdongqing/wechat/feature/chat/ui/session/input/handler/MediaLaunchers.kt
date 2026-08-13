package top.chengdongqing.wechat.feature.chat.ui.session.input.handler

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import top.chengdongqing.wechat.core.camera.CameraLauncher
import top.chengdongqing.wechat.core.media.picker.MediaPickerLauncher

/**
 * 媒体启动器集合
 */
data class MediaLaunchers(
    val launchMediaPicker: MediaPickerLauncher,
    val launchSystemMediaPicker: ManagedActivityResultLauncher<PickVisualMediaRequest, List<Uri>>,
    val launchCamera: CameraLauncher,
    val takePicture: ManagedActivityResultLauncher<Uri, Boolean>,
    val captureVideo: ManagedActivityResultLauncher<Uri, Boolean>,
    val capturedUri: () -> Uri?,
    val setCapturedUri: (Uri?) -> Unit
)
