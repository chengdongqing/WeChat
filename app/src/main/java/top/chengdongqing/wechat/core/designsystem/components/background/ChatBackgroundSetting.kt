package top.chengdongqing.wechat.core.designsystem.components.background

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
import androidx.compose.ui.platform.LocalResources
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.rememberActionSheetState
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.theme.Danger
import top.chengdongqing.wechat.core.util.createImageUri

@Composable
fun ChatBackgroundSetting(
    label: String,
    value: String?,
    onChange: (Uri?) -> Unit
) {
    val selectorState = rememberBackgroundSelectorState(onChange)
    val scope = rememberCoroutineScope()
    val dialog = rememberDialogState()
    val actionSheet = rememberActionSheetState()
    val resources = LocalResources.current

    val options = remember(value) {
        val list = mutableListOf(
            ActionSheetItem(R.string.action_take_photo),
            ActionSheetItem(R.string.action_select_from_gallery)
        )
        if (value != null) {
            list.add(
                ActionSheetItem(
                    labelRes = R.string.chat_info_clear_background,
                    color = Danger
                )
            )
        }
        list
    }

    val handleShowMenu = {
        actionSheet.show(options) { index ->
            when (index) {
                0 -> scope.launch { selectorState.handleCameraAction() }
                1 -> selectorState.pickVisualMedia()
                2 -> {
                    dialog.show(resources.getString(R.string.chat_info_background_clear_title)) {
                        onChange(null)
                    }
                }
            }
        }
    }

    WeSettingItem(
        label = label,
        showDivider = false,
        onClick = handleShowMenu
    )
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
        context.createImageUri().let { uri ->
            tempUri.value = uri
            cameraLauncher.launch(uri)
        }
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
                val uri = context.createImageUri()
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