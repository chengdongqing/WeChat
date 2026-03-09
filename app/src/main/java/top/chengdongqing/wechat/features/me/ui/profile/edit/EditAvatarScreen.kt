package top.chengdongqing.wechat.features.me.ui.profile.edit

import androidx.compose.animation.core.SnapSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.rememberActionSheetState
import top.chengdongqing.wechat.core.designsystem.components.camera.rememberCameraLauncher
import top.chengdongqing.wechat.core.designsystem.components.cropper.rememberImageCropperLauncher
import top.chengdongqing.wechat.core.designsystem.components.media.model.VisualMediaType
import top.chengdongqing.wechat.core.designsystem.components.media.picker.rememberPickMediasLauncher
import top.chengdongqing.wechat.core.designsystem.components.toast.ToastIcon
import top.chengdongqing.wechat.core.designsystem.components.toast.rememberToastState
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.Black
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.core.designsystem.util.StatusBarAppearanceEffect
import top.chengdongqing.wechat.features.me.ui.profile.ProfileField
import top.chengdongqing.wechat.features.me.ui.profile.ProfileViewModel
import java.io.File

@Composable
fun EditAvatarScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = uiState.profile

    val scope = rememberCoroutineScope()
    val toast = rememberToastState()
    val actionSheet = rememberActionSheetState()
    val zoomableState = rememberZoomableState()
    val state = rememberZoomableImageState(zoomableState)

    val launchCropper = rememberImageCropperLauncher {
        scope.launch { zoomableState.resetZoom(SnapSpec()) } // 重置缩放，避免被之前的缩放影响
        viewModel.updateField(ProfileField.Avatar(it))
    }
    val launchAlbum = rememberPickMediasLauncher { medias ->
        launchCropper(medias[0].uri)
    }
    val launchCamera = rememberCameraLauncher { uri, _ ->
        launchCropper(uri)
    }

    val saveAvatar = {
        scope.launch {
            val localPath = profile?.avatarPath ?: return@launch
            val uri = File(localPath).toUri()
            val success = viewModel.saveImage(uri)

            toast.show(
                title = if (success) "已保存到相册" else "保存失败",
                icon = if (success) ToastIcon.Success else ToastIcon.Fail
            )
        }
    }

    val menuOptions = rememberMenuOptions()

    StatusBarAppearanceEffect(isDark = false)
    Box(modifier = Modifier.background(Black)) {
        WeTopBar(
            title = stringResource(R.string.me_profile_avatar),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(1f),
            containerColor = Color.Transparent,
            contentColor = White,
            onBack = onBack
        ) {
            ActionIcon(
                icon = R.drawable.ic_more_outlined,
                description = "更多"
            ) {
                actionSheet.show(menuOptions) { index ->
                    when (index) {
                        0 -> launchAlbum(VisualMediaType.Image, 1)
                        1 -> launchCamera(VisualMediaType.Image)
                        2 -> saveAvatar()
                    }
                }
            }
        }

        ZoomableAsyncImage(
            state = state,
            model = profile?.avatarPath,
            contentDescription = stringResource(R.string.me_profile_avatar),
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun rememberMenuOptions(): List<ActionSheetItem> {
    val selectText = stringResource(R.string.action_select_from_gallery)
    val takePhotoText = stringResource(R.string.action_take_photo)
    val saveText = stringResource(R.string.action_save_to_phone)

    return remember {
        listOf(
            ActionSheetItem(selectText),
            ActionSheetItem(takePhotoText),
            ActionSheetItem(saveText)
        )
    }
}