package top.chengdongqing.wechat.core.media.picker

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.checkbox.WeCheckBox
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.file.getFileMetadata
import top.chengdongqing.wechat.core.media.editor.ImageEditor
import top.chengdongqing.wechat.core.media.model.MediaItem
import top.chengdongqing.wechat.core.playback.video.WeVideoPlayer
import top.chengdongqing.wechat.core.playback.video.rememberVideoPlayerState

/** A preview owned by the picker. The app-wide media preview intentionally stays unchanged. */
@Composable
internal fun PickerMediaPreview(
    medias: List<MediaItem>,
    initialIndex: Int,
    state: MediaPickerState,
    original: Boolean,
    onOriginalChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (medias.isEmpty()) {
        onDismiss()
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialIndex) { medias.size }
    var editingUri by remember { mutableStateOf<Uri?>(null) }

    editingUri?.let { uri ->
        ImageEditor(
            sourceUri = uri,
            onCancel = { editingUri = null },
            onConfirm = { editedUri ->
                val source = medias[pagerState.currentPage]
                scope.launch {
                    val metadata = context.getFileMetadata(editedUri)
                    state.replace(
                        source,
                        source.copy(
                            uri = editedUri,
                            filename = metadata?.filename ?: source.filename,
                            mimeType = metadata?.mimeType ?: source.mimeType,
                            width = metadata?.width ?: source.width,
                            height = metadata?.height ?: source.height,
                            size = metadata?.size ?: source.size
                        )
                    )
                    editingUri = null
                }
            }
        )
        return
    }

    BackHandler(onBack = onDismiss)
    Box(Modifier
        .fillMaxSize()
        .background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
            val media = medias[index]
            if (media.isVideo) {
                WeVideoPlayer(rememberVideoPlayerState(videoSource = media.uri))
            } else {
                AsyncImage(
                    model = media.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "返回",
                tint = Color.White,
                modifier = Modifier
                    .size(30.dp)
                    .clickable(onClick = onDismiss)
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${pagerState.currentPage + 1}/${medias.size}",
                color = Color.White,
                fontSize = 18.sp
            )
            Spacer(Modifier.weight(1f))
            val current = medias[pagerState.currentPage]
            val selectedIndex = state.selectedMediaList.indexOf(current)
            Row(
                modifier = Modifier.clickable {
                    if (selectedIndex >= 0) state.removeAt(selectedIndex)
                    else if (state.selectedMediaList.size < state.count) state.add(current)
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(26.dp)
                        .background(
                            if (selectedIndex >= 0) WeTheme.colorScheme.primary else Color.Transparent,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedIndex >= 0) Text("${selectedIndex + 1}", color = Color.White)
                }
                Text("  选择", color = Color.White, fontSize = 17.sp)
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xE61C1C1C))
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val current = medias[pagerState.currentPage]
            Text(
                "编辑",
                color = if (current.isImage) Color.White else Color.Gray,
                fontSize = 18.sp,
                modifier = Modifier.clickable(enabled = current.isImage) {
                    editingUri = current.uri
                }
            )
            Row(
                modifier = Modifier.clickable { onOriginalChange(!original) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                WeCheckBox(checked = original)
                Text("  原图", color = Color.White, fontSize = 17.sp)
            }
            Spacer(Modifier.weight(1f))
            WeButton(
                text = if (state.selectedMediaList.isEmpty()) "发送" else "发送(${state.selectedMediaList.size})",
                size = ButtonSize.Small,
                onClick = {
                    // 预览页允许不勾选直接发送，此时以当前页作为唯一发送项。
                    if (state.selectedMediaList.isEmpty()) state.add(current)
                    onConfirm()
                }
            )
        }
    }
}
