package top.chengdongqing.wechat.feature.moments.ui.cover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import top.chengdongqing.wechat.core.camera.rememberCameraLauncher
import top.chengdongqing.wechat.core.media.model.VisualMediaType
import top.chengdongqing.wechat.core.media.picker.rememberMediaPickerLauncher
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingGroup
import top.chengdongqing.wechat.core.designsystem.components.menu.WeSettingItem
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.feature.moments.ui.list.MomentsViewModel

@Composable
fun ChangeMomentCoverScreen(
    onBack: () -> Unit,
    onNavigateToPhotographerWorks: () -> Unit,
    viewModel: MomentsViewModel = hiltViewModel()
) {
    val mediaPickerLauncher = rememberMediaPickerLauncher { medias, _, _ ->
        viewModel.setCover(medias.first().uri)
        onBack()
    }
    val cameraLauncher = rememberCameraLauncher { uri, _ ->
        viewModel.setCover(uri)
        onBack()
    }

    Scaffold(
        topBar = { WeTopAppBar(title = "更换相册封面", onBack = onBack) },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WeSettingGroup {
                WeSettingItem(
                    label = "从手机相册选择",
                    height = 56.dp,
                    onClick = { mediaPickerLauncher(VisualMediaType.Image, 1) }
                )
                WeSettingItem(
                    label = "拍一个",
                    showDivider = false,
                    height = 56.dp,
                    onClick = {
                        cameraLauncher(VisualMediaType.Image)
                    }
                )
            }
            WeSettingGroup {
                WeSettingItem(
                    label = "摄影师作品",
                    description = "从摄影师的作品中挑选图片",
                    showDivider = false,
                    height = 62.dp,
                    onClick = onNavigateToPhotographerWorks
                )
            }
        }
    }
}
