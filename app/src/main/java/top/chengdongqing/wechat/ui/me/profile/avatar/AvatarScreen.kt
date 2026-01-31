package top.chengdongqing.wechat.ui.me.profile.avatar

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.SnapSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.utils.createImageUri
import top.chengdongqing.wechat.core.utils.saveToAlbum
import top.chengdongqing.wechat.data.model.VisualMediaType
import top.chengdongqing.wechat.ui.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.ui.components.actionsheet.rememberActionSheetState
import top.chengdongqing.wechat.ui.components.camera.rememberCameraLauncher
import top.chengdongqing.wechat.ui.components.cropper.rememberImageCropperLauncher
import top.chengdongqing.wechat.ui.components.media.picker.rememberPickMediasLauncher
import top.chengdongqing.wechat.ui.components.toast.ToastIcon
import top.chengdongqing.wechat.ui.components.toast.rememberToastState
import top.chengdongqing.wechat.ui.components.topbar.WeTopBar
import top.chengdongqing.wechat.ui.theme.Black
import top.chengdongqing.wechat.ui.theme.White

@Composable
fun AvatarScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val toast = rememberToastState()
    val actionSheet = rememberActionSheetState()
    val zoomableState = rememberZoomableState()
    val state = rememberZoomableImageState(zoomableState)

    var avatarModel by remember { mutableStateOf<Any>(R.drawable.img_avatar) }

    val launchCropper = rememberImageCropperLauncher {
        scope.launch { zoomableState.resetZoom(SnapSpec()) } // 重置缩放，避免被之前的缩放影响
        avatarModel = it
    }
    val launchAlbum = rememberPickMediasLauncher { medias ->
        launchCropper(medias[0].uri)
    }
    val launchCamera = rememberCameraLauncher { uri, _ ->
        launchCropper(uri)
    }

    val saveAvatar = {
        scope.launch {
            val uri = resolveAvatarUri(context, resources, avatarModel) ?: return@launch
            val success = context.saveToAlbum(uri)

            toast.show(
                title = if (success) "已保存到相册" else "保存失败",
                icon = if (success) ToastIcon.SUCCESS else ToastIcon.FAIL
            )
        }
    }

    Box(modifier = Modifier.background(Black)) {
        WeTopBar(
            title = "头像",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(1f),
            bgColor = Color.Transparent,
            textColor = White,
            onBack = onBack
        ) {
            ActionIcon(iconResId = R.drawable.ic_more_outlined, description = "更多") {
                actionSheet.show(MenuOptions) { index ->
                    when (index) {
                        0 -> launchAlbum(VisualMediaType.IMAGE, 1)
                        1 -> launchCamera(VisualMediaType.IMAGE)
                        2 -> saveAvatar()
                    }
                }
            }
        }

        ZoomableAsyncImage(
            state = state,
            model = avatarModel,
            contentDescription = "头像",
            modifier = Modifier.fillMaxSize()
        )
    }
}

private val MenuOptions = listOf(
    ActionSheetItem("从相册选择"),
    ActionSheetItem("拍摄新照片"),
    ActionSheetItem("保存到本地")
)

/**
 * 将头像model解析为Uri
 *
 * avatarModel可能是：
 * - Int: 默认头像资源ID
 * - Uri: 用户选择/拍摄的头像
 */
private suspend fun resolveAvatarUri(
    context: Context,
    resources: Resources,
    model: Any
): Uri? = withContext(Dispatchers.IO) {
    when (model) {
        is Uri -> model
        is Int -> {
            val bitmap = BitmapFactory.decodeResource(resources, model) ?: return@withContext null
            context.createImageUri(bitmap)
        }

        else -> null
    }
}