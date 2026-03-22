package top.chengdongqing.wechat.core.media.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.util.weClickable
import top.chengdongqing.wechat.core.media.model.MediaItem
import top.chengdongqing.wechat.core.media.preview.previewMedias
import top.chengdongqing.wechat.core.util.format
import top.chengdongqing.wechat.core.util.loadMediaThumbnail
import top.chengdongqing.wechat.core.util.showToast
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun ColumnScope.MediaGrid(state: MediaPickerState) {
    val context = LocalContext.current
    val overscrollEffect = rememberBounceOverscrollEffect()

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .weight(1f)
            .overscroll(overscrollEffect),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        overscrollEffect = overscrollEffect
    ) {
        itemsIndexed(state.mediaList) { index, item ->
            val selectedIndex = state.selectedMediaList.indexOf(item)
            val selected = selectedIndex != -1

            MediaGridCell(
                item,
                selected,
                selectedIndex,
                onClick = {
                    context.previewMedias(state.mediaList, index)
                }
            ) {
                if (selectedIndex == -1) {
                    if (state.selectedMediaList.size < state.count) {
                        state.add(item)
                    } else {
                        context.showToast("你最多只能选择${state.count}个")
                    }
                } else {
                    state.removeAt(selectedIndex)
                }
            }
        }
    }
}

@Composable
private fun MediaGridCell(
    media: MediaItem,
    selected: Boolean,
    selectedIndex: Int,
    onClick: () -> Unit,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(WeTheme.colorScheme.divider)
            .clickable { onClick() }
    ) {
        val context = LocalContext.current
        val thumbnail by produceState<Any?>(initialValue = null) {
            value = context.loadMediaThumbnail(media.uri, media.isVideo)
        }

        AsyncImage(
            model = thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        // 视频标识及时长
        if (media.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = media.duration.milliseconds.format(),
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
        // 遮罩层
        if (selected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0f, 0f, 0f, 0.4f))
            )
        }
        // 选择框
        MediaCheckbox(selected, selectedIndex, onSelect)
    }
}

@Composable
private fun BoxScope.MediaCheckbox(selected: Boolean, selectedIndex: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .weClickable { onClick() }
            .padding(top = 6.dp, end = 6.dp, start = 18.dp, bottom = 18.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .selectable(selected),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Text(
                    text = (selectedIndex + 1).toString(),
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun Modifier.selectable(selected: Boolean) = this.then(
    if (selected) {
        Modifier.background(WeTheme.colorScheme.primary, CircleShape)
    } else {
        Modifier.border(1.dp, Color.White, CircleShape)
    }
)