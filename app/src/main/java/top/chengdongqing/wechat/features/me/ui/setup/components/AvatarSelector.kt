package top.chengdongqing.wechat.features.me.ui.setup.components

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.rememberActionSheetState
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonType
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.cropper.rememberImageCropperLauncher
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.weClickable
import top.chengdongqing.wechat.core.util.createImageUri

/**
 * 头像选择器组件
 * 封装了：权限请求、系统相机交互、系统相册交互、图片裁剪
 */
@Composable
fun AvatarSelector(
    avatarUri: Uri?,
    onAvatarChange: (Uri?) -> Unit,
    enabled: Boolean = true
) {
    val selectorState = rememberAvatarSelectorState(onAvatarChange)
    val scope = rememberCoroutineScope()
    val actionSheet = rememberActionSheetState()
    val options = remember { listOf(ActionSheetItem("拍照"), ActionSheetItem("从相册选择")) }

    val handleShowMenu = {
        if (enabled) {
            actionSheet.show(options) { index ->
                if (index == 0) {
                    scope.launch {
                        selectorState.handleCameraAction()
                    }
                } else {
                    selectorState.pickVisualMedia()
                }
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AvatarDisplay(
            avatarUri = avatarUri,
            enabled = enabled,
            onClick = handleShowMenu
        )

        Spacer(modifier = Modifier.height(20.dp))

        WeButton(
            text = if (avatarUri != null) "更换头像" else "设置头像",
            type = ButtonType.Plain,
            size = ButtonSize.Small,
            enabled = enabled,
            prefix = {
                Icon(
                    painter = painterResource(R.drawable.ic_camera_filled),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = WeTheme.colorScheme.textPrimary
                )
            },
            onClick = handleShowMenu
        )
    }
}

/**
 * 头像展示区域
 */
@Composable
private fun AvatarDisplay(
    avatarUri: Uri?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .weClickable(enabled = enabled, onClick = onClick)
    ) {
        if (avatarUri != null) {
            AsyncImage(
                model = avatarUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(R.drawable.img_avatar_placeholder),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 提取头像选择逻辑状态
 */
@OptIn(ExperimentalPermissionsApi::class)
private class AvatarSelectorState(
    val context: Context,
    val cameraPermission: PermissionState,
    val cameraLauncher: ManagedActivityResultLauncher<Uri, Boolean>,
    val mediaPicker: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>,
    val tempUri: MutableState<Uri?>
) {
    suspend fun takePicture() {
        val uri = context.createImageUri()
        tempUri.value = uri
        cameraLauncher.launch(uri)
    }

    fun pickVisualMedia() {
        mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    suspend fun handleCameraAction() {
        if (cameraPermission.status.isGranted) {
            takePicture()
        } else {
            cameraPermission.launchPermissionRequest()
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun rememberAvatarSelectorState(
    onAvatarCropped: (Uri?) -> Unit
): AvatarSelectorState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tempUri = remember { mutableStateOf<Uri?>(null) }

    val launchCropper = rememberImageCropperLauncher { onAvatarCropped(it) }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) tempUri.value?.let { launchCropper(it) }
        }

    val mediaPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { launchCropper(it) }
        }

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA) { granted ->
        if (granted) {
            scope.launch {
                val uri = context.createImageUri()
                tempUri.value = uri
                cameraLauncher.launch(uri)
            }
        }
    }

    return remember(cameraPermission) {
        AvatarSelectorState(
            context,
            cameraPermission,
            cameraLauncher,
            mediaPicker,
            tempUri
        )
    }
}