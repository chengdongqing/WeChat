package top.chengdongqing.wechat.core.common.media.preview

import androidx.compose.animation.core.SnapSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import me.saket.telephoto.zoomable.rememberZoomableState
import top.chengdongqing.wechat.core.common.media.model.MediaItem
import top.chengdongqing.wechat.core.designsystem.components.videoplayer.VideoPlayerDefaults
import top.chengdongqing.wechat.core.designsystem.components.videoplayer.WeVideoPlayer
import top.chengdongqing.wechat.core.designsystem.components.videoplayer.rememberVideoPlayerState
import top.chengdongqing.wechat.core.designsystem.window.ImmersiveSystemBars
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
fun WeMediaPreview(
    medias: List<MediaItem>,
    current: Int = 0,
    interactiveContentEnabled: Boolean = true,
    interactiveContentDelayMillis: Long = 0L,
    pageModifier: @Composable (Int) -> Modifier = { Modifier },
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(current) { medias.size }

    ImmersiveSystemBars()
    Box {
        MediaPager(
            medias = medias,
            pagerState = pagerState,
            interactiveContentEnabled = interactiveContentEnabled,
            interactiveContentDelayMillis = interactiveContentDelayMillis,
            pageModifier = pageModifier,
            onDismiss = onDismiss
        )
        PagerInfo(
            total = medias.size,
            current = pagerState.currentPage + 1
        )
        ToolBar(medias, pagerState)
    }
}

@Composable
private fun MediaPager(
    medias: List<MediaItem>,
    pagerState: PagerState,
    interactiveContentEnabled: Boolean,
    interactiveContentDelayMillis: Long,
    pageModifier: @Composable (Int) -> Modifier,
    onDismiss: () -> Unit
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) { index ->
        val media = medias[index]
        val ratio = if (media.width > 0 && media.height > 0) {
            media.width.toFloat() / media.height
        } else {
            1f
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val viewportRatio = maxWidth / maxHeight
            val fittedSize = if (ratio >= viewportRatio) {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
            } else {
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(ratio)
            }

            var interactiveReady by remember(
                media.uri,
                interactiveContentEnabled,
                interactiveContentDelayMillis
            ) {
                mutableStateOf(
                    interactiveContentEnabled && interactiveContentDelayMillis == 0L
                )
            }
            LaunchedEffect(
                media.uri,
                interactiveContentEnabled,
                interactiveContentDelayMillis
            ) {
                if (!interactiveContentEnabled) return@LaunchedEffect
                if (interactiveContentDelayMillis == 0L) return@LaunchedEffect
                // 先让共享封面完成几何变化，再启用全屏缩放画布或视频 Surface。
                delay(interactiveContentDelayMillis)
                interactiveReady = true
            }

            // 共享元素只承担进出场。它始终保留在树中，以便退出时能精确匹配，
            // 但进入稳定预览后由真正的全屏内容覆盖。
            AsyncImage(
                model = media.uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = pageModifier(index).then(fittedSize)
            )

            if (interactiveContentEnabled && interactiveReady) {
                when {
                    media.isVideo -> {
                        val state = rememberVideoPlayerState(videoSource = media.uri)
                        WeVideoPlayer(state) {
                            VideoPlayerDefaults.ControlBar(
                                state,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(y = (-60).dp)
                            )
                        }
                    }

                    else -> {
                        val zoomableState = rememberZoomableState()

                        ImagePreview(media.uri, zoomableState, onDismiss)

                        // 滑到另一页后重置当前页的缩放状态
                        if (pagerState.settledPage != index) {
                            LaunchedEffect(Unit) {
                                zoomableState.resetZoom(SnapSpec())
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun BoxScope.PagerInfo(total: Int, current: Int) {
    Text(
        text = "${current}/${total}",
        color = Color.White,
        fontSize = 14.sp,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 50.dp)
    )
}

@Composable
private fun BoxScope.ToolBar(
    medias: List<MediaItem>,
    pagerState: PagerState,
    viewModel: MediaPreviewViewModel = hiltViewModel()
) {
    val media = medias[pagerState.currentPage]

    Row(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 26.dp, bottom = 26.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ActionIcon(
            imageVector = Icons.Outlined.Share,
            label = stringResource(DesignR.string.action_share)
        ) {
            viewModel.shareMedia(media)
        }
        ActionIcon(
            imageVector = Icons.Outlined.Download,
            label = stringResource(DesignR.string.action_save)
        ) {
            viewModel.saveMedia(media)
        }
    }
}

@Composable
private fun ActionIcon(imageVector: ImageVector, label: String?, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Gray.copy(0.6f)
        ),
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}
