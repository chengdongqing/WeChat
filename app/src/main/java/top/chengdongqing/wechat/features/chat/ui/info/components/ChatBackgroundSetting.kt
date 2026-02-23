package top.chengdongqing.wechat.features.chat.ui.info.components

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.rememberActionSheetState
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.designsystem.theme.Danger
import top.chengdongqing.wechat.core.util.createMediaUri
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat.features.chat.ui.info.SettingItem

@Composable
fun ChatBackgroundSetting(background: String?, onBackgroundChange: (Uri?) -> Unit) {
    val context = LocalContext.current
    val selectorState = rememberBackgroundSelectorState { uri ->
        onBackgroundChange(uri)
        context.showToast("背景设置成功")
    }
    val scope = rememberCoroutineScope()
    val dialog = rememberDialogState()
    val actionSheet = rememberActionSheetState()
    val options = remember(background) {
        val list = mutableListOf(
            ActionSheetItem("拍照"),
            ActionSheetItem("从相册选择")
        )
        if (background != null) {
            list.add(ActionSheetItem(label = "清除当前背景", color = Danger))
        }
        list
    }

    val handleShowMenu = {
        actionSheet.show(options) { index ->
            when (index) {
                0 -> scope.launch { selectorState.handleCameraAction() }
                1 -> selectorState.pickVisualMedia()
                2 -> {
                    dialog.show("确定清除当前聊天背景吗？", onOk = {
                        onBackgroundChange(null)
                        context.showToast("背景清除成功")
                    })
                }
            }
        }
    }

    SettingItem("设置当前聊天背景", showDivider = false, onClick = handleShowMenu)
}

@OptIn(ExperimentalPermissionsApi::class)
private class BackgroundSelectorState(
    val context: Context,
    val cameraPermission: PermissionState,
    val cameraLauncher: ManagedActivityResultLauncher<Uri, Boolean>,
    val mediaPicker: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>,
    val tempUri: MutableState<Uri?>
) {
    suspend fun takePicture() {
        val uri = context.createMediaUri()
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
private fun rememberBackgroundSelectorState(
    onBackgroundSelect: (Uri?) -> Unit
): BackgroundSelectorState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tempUri = remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) tempUri.value?.let { onBackgroundSelect(it) }
        }

    val mediaPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { onBackgroundSelect(it) }
        }

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA) { granted ->
        if (granted) {
            scope.launch {
                val uri = context.createMediaUri()
                tempUri.value = uri
                cameraLauncher.launch(uri)
            }
        }
    }

    return remember(cameraPermission) {
        BackgroundSelectorState(
            context,
            cameraPermission,
            cameraLauncher,
            mediaPicker,
            tempUri
        )
    }
}