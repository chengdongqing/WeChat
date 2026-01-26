package top.chengdongqing.wechat.ui.chat.session.input.panels

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.ui.components.WeDivider
import top.chengdongqing.wechat.ui.utils.BounceOverscrollEffect

@Composable
fun MoreActionPanel(onAction: (MoreAction) -> Unit) {
    val scope = rememberCoroutineScope()
    val pages = remember { MoreActionItems.chunked(ChunkCount) }
    val pagerState = rememberPagerState { pages.size }
    val overscrollEffect = remember { BounceOverscrollEffect(scope, Orientation.Horizontal) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
    ) {
        WeDivider()
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(top = 28.dp)
                .overscroll(overscrollEffect),
            overscrollEffect = overscrollEffect
        ) { pageIndex ->
            MorePanelGrid(
                items = pages[pageIndex],
                onItemClick = onAction
            )
        }
        if (pages.size > 1) {
            Spacer(modifier = Modifier.weight(1f))
            PagerIndicator(
                count = pages.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun MorePanelGrid(
    items: List<MoreItemData>,
    onItemClick: (MoreAction) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        maxItemsInEachRow = 4,
        horizontalArrangement = Arrangement.SpaceAround,
        verticalArrangement = Arrangement.spacedBy(36.dp)
    ) {
        items.forEach { item ->
            MorePanelItem(item) {
                onItemClick(item.id)
            }
        }
        if (items.size < ChunkCount) {
            repeat(ChunkCount - items.size) {
                Spacer(modifier = Modifier.size(52.dp))
            }
        }
    }
}

@Composable
private fun MorePanelItem(
    item: MoreItemData,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(14.dp),
            color = Color.White
        ) {
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(item.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = Color.Unspecified
                )
            }
        }

        Text(
            text = item.title,
            modifier = Modifier.padding(top = 4.dp),
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
    }
}

@Composable
private fun PagerIndicator(
    count: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { iteration ->
            val color = if (currentPage == iteration) Color(0xFF8B8B8B) else Color(0xFFD8D8D8)
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(CircleShape)
                    .background(color)
                    .size(5.5.dp)
            )
        }
    }
}

private val MoreActionItems = listOf(
    MoreItemData(MoreAction.ALBUM, "照片", R.drawable.ic_album_filled),
    MoreItemData(MoreAction.CAMERA, "拍摄", R.drawable.ic_camera_filled),
    MoreItemData(MoreAction.VIDEO_CALL, "视频通话", R.drawable.ic_video_call_filled),
    MoreItemData(MoreAction.LOCATION, "位置", R.drawable.ic_location_filled),
    MoreItemData(MoreAction.TRANSFER, "转账", R.drawable.ic_transfer_filled),
    MoreItemData(MoreAction.FAVORITE, "收藏", R.drawable.ic_favorites_filled),
    MoreItemData(MoreAction.VOICE, "语音输入", R.drawable.ic_mic_filled),
    MoreItemData(MoreAction.CARD, "个人名片", R.drawable.ic_person_filled),
    MoreItemData(MoreAction.FILE, "文件", R.drawable.ic_folder_filled),
    MoreItemData(MoreAction.MUSIC, "音乐", R.drawable.ic_music_filled),
)

private const val ChunkCount = 8

enum class MoreAction {
    ALBUM, CAMERA, VIDEO_CALL, LOCATION, TRANSFER, FAVORITE, VOICE, CARD, FILE, MUSIC
}

private data class MoreItemData(
    val id: MoreAction,
    val title: String,
    @get:DrawableRes val iconRes: Int
)