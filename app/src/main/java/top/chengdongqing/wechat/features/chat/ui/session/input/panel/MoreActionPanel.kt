package top.chengdongqing.wechat.features.chat.ui.session.input.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.util.isTrue
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.features.chat.ui.session.LocalChatSessionContext

@Composable
fun MoreActionPanel(onAction: (action: MoreAction, isLongClick: Boolean) -> Unit) {
    val chatContext = LocalChatSessionContext.current
    val isMyself = chatContext?.isMyself.isTrue()

    val pages = remember(isMyself) {
        MoreAction.entries
            .filter { action ->
                // 如果是自己，则过滤掉视频通话
                !(isMyself && action == MoreAction.VideoCall)
            }
            .chunked(ChunkCount)
    }

    val pagerState = rememberPagerState { pages.size }
    val overscrollEffect = rememberBounceOverscrollEffect(Orientation.Horizontal)

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
                onAction = onAction
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
    items: List<MoreAction>,
    onAction: (action: MoreAction, isLongClick: Boolean) -> Unit
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
            MorePanelItem(
                item = item,
                onClick = { onAction(item, false) },
                onLongClick = { onAction(item, true) }
            )
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
    item: MoreAction,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            color = Color.White
        ) {
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(item.iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = Color.Unspecified
                )
            }
        }

        Text(
            text = item.label,
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

private const val ChunkCount = 8