package top.chengdongqing.wechat.feature.chat.ui.session.input.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.isTrue
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.feature.chat.theme.ChatTheme
import top.chengdongqing.wechat.feature.chat.ui.session.LocalChatSessionContext

@Composable
fun MoreActionPanel(onAction: (action: MoreAction, isLongClick: Boolean) -> Unit) {
    val chatContext = LocalChatSessionContext.current
    val isSelf = chatContext?.isSelf.isTrue()
    val isGroup = chatContext?.isGroup == true

    val pages = remember(isSelf, isGroup) {
        MoreAction.entries
            .filter { action ->
                // 如果是自己，则过滤掉视频通话
                !(isSelf && action == MoreAction.VideoCall) &&
                    (isGroup || action != MoreAction.Live)
            }
            .chunked(ChunkCount)
    }

    val pagerState = rememberPagerState { pages.size }
    val overscrollEffect = rememberBounceOverscrollEffect(Orientation.Horizontal)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatTheme.colorScheme.bottomBarBackground)
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
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(36.dp),
        userScrollEnabled = false
    ) {
        items(ChunkCount) { index ->
            val item = items.getOrNull(index)
            if (item != null) {
                MorePanelItem(
                    item = item,
                    onClick = { onAction(item, false) },
                    onLongClick = { onAction(item, true) }
                )
            } else {
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ChatTheme.colorScheme.textField)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(item.icon),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = WeTheme.colorScheme.textPrimary
            )
        }
        Text(
            text = stringResource(item.labelRes),
            modifier = Modifier.padding(top = 4.dp),
            fontSize = 12.sp,
            color = WeTheme.colorScheme.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
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
