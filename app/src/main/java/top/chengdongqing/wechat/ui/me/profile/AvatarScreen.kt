package top.chengdongqing.wechat.ui.me.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.model.VisualMediaType
import top.chengdongqing.wechat.ui.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.ui.components.actionsheet.rememberActionSheetState
import top.chengdongqing.wechat.ui.components.camera.rememberCameraLauncher
import top.chengdongqing.wechat.ui.components.cropper.rememberImageCropperLauncher
import top.chengdongqing.wechat.ui.components.media.picker.rememberPickMediasLauncher
import top.chengdongqing.wechat.ui.components.topbar.WeTopBar
import top.chengdongqing.wechat.ui.theme.Black
import top.chengdongqing.wechat.ui.theme.White

@Composable
fun AvatarScreen(onBack: () -> Unit) {
//    val context = LocalContext.current
//    val scope = rememberCoroutineScope()
//
//    val toast = rememberToastState()
    val actionSheet = rememberActionSheetState()
    val menuOptions = remember {
        listOf(
            ActionSheetItem("从相册选择"),
            ActionSheetItem("拍摄新照片"),
            ActionSheetItem("保存到本地"),
        )
    }

    val imageCropperLauncher = rememberImageCropperLauncher {

    }
    val mediasLauncher = rememberPickMediasLauncher { medias ->
        imageCropperLauncher(medias[0].uri)
    }
    val cameraLauncher = rememberCameraLauncher { uri, _ ->
        imageCropperLauncher(uri)
    }

    Scaffold(
        topBar = {
            WeTopBar(
                title = "头像",
                bgColor = Black,
                textColor = White,
                onBack = onBack
            ) {
                ActionIcon(iconResId = R.drawable.ic_more_outlined, description = "更多") {
                    actionSheet.show(menuOptions) { index ->
                        when (index) {
                            0 -> mediasLauncher(VisualMediaType.IMAGE, 1)
                            1 -> cameraLauncher(VisualMediaType.IMAGE)
                            2 -> {
//                                scope.launch {
//                                    if (context.saveToAlbum(media)) {
//                                        toast.show("已保存到相册", icon = ToastIcon.SUCCESS)
//                                    } else {
//                                        toast.show("保存失败", icon = ToastIcon.FAIL)
//                                    }
//                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Black)
                .padding(innerPadding)
        ) {
            AsyncImage(
                model = R.drawable.img_avatar,
                contentDescription = "头像",
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(1f)
            )
        }
    }
}