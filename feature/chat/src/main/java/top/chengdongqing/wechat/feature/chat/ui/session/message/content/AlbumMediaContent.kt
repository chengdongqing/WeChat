package top.chengdongqing.wechat.feature.chat.ui.session.message.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.data.model.ChatMessage
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import kotlin.math.absoluteValue

val LocalExpandedMediaAlbums = compositionLocalOf<MutableList<String>?> { null }

@Composable
fun AlbumMediaContent(
    messages: List<ChatMessage>,
    onMediaClick: (ChatMessage) -> Unit
) {
    val ordered = remember(messages) {
        messages.sortedBy { (it.content as MessageContent.Media).albumIndex }
    }
    val albumId = (ordered.firstOrNull()?.content as? MessageContent.Media)?.albumId.orEmpty()
    val expandedAlbums = LocalExpandedMediaAlbums.current
    var localExpanded by remember { mutableStateOf(false) }
    val expanded = expandedAlbums?.contains(albumId) ?: localExpanded
    val toggleExpanded = {
        if (expandedAlbums == null) {
            localExpanded = !localExpanded
        } else if (expanded) {
            expandedAlbums.remove(albumId)
        } else {
            expandedAlbums.add(albumId)
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = WeTheme.colorScheme.divider,
            shape = CircleShape,
            modifier = Modifier
                .padding(top = 10.dp)
                .clickable { toggleExpanded() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起" else "展开",
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (expanded) "收起" else "展开 ${ordered.size}",
                    fontSize = 13.sp
                )
            }
        }

        if (expanded) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ordered.forEach { message ->
                    AlbumImage(message, Modifier.clickable { onMediaClick(message) })
                }
            }
        } else {
            val pagerState = rememberPagerState(
                initialPage = ordered.size / 2,
                pageCount = { ordered.size }
            )
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(230.dp),
                contentAlignment = Alignment.Center
            ) {
                val cardWidth = maxWidth * 0.82f
                val sidePadding = maxWidth * 0.09f
                // Pager 保持正常页宽；绘制时将相邻页拉回主卡片下方，只露出边缘。
                val stackPull = cardWidth - sidePadding

                HorizontalPager(
                    state = pagerState,
                    pageSize = PageSize.Fixed(cardWidth),
                    contentPadding = PaddingValues(horizontal = sidePadding),
                    pageSpacing = 0.dp,
                    beyondViewportPageCount = 2,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val pageDistance =
                        (pagerState.currentPage - page).absoluteValue.coerceAtMost(2)

                    AsyncImage(
                        model = (ordered[page].content as MessageContent.Media).localPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp)
                            .zIndex(10f - pageDistance)
                            .graphicsLayer {
                                val pageOffset =
                                    pagerState.currentPage - page +
                                            pagerState.currentPageOffsetFraction
                                val distance = pageOffset.absoluteValue.coerceAtMost(2f)
                                val transitionDistance = distance.coerceAtMost(1f)
                                val cardScale = if (transitionDistance <= 0.5f) {
                                    // 主图离开中心时快速缩小，强化被滑动的反馈。
                                    1f - transitionDistance * 0.16f
                                } else {
                                    // 完成交接后，后层卡片保持轻微缩小。
                                    0.92f + (transitionDistance - 0.5f) * 0.08f
                                }
                                scaleX = cardScale
                                scaleY = cardScale
                                translationX = pageOffset * stackPull.toPx()
                                translationY = distance * 6.dp.toPx()
                                transformOrigin = TransformOrigin(0.5f, 1f)
                                rotationZ = -pageOffset.coerceIn(-1f, 1f) * 3.5f
                                alpha = 1f
                                shadowElevation = (12f - distance * 3f).dp.toPx()
                                shape = RoundedCornerShape(12.dp)
                                clip = true
                            }
                            .clickable { onMediaClick(ordered[page]) }
                    )
                }
                Text(
                    text = "${pagerState.currentPage + 1}/${ordered.size}",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                        .zIndex(20f)
                )
            }
        }
    }
}

@Composable
private fun AlbumImage(message: ChatMessage, modifier: Modifier = Modifier) {
    val media = message.content as MessageContent.Media
    AsyncImage(
        model = media.localPath,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(if (media.ratio > 0f) media.ratio.coerceIn(0.8f, 1.5f) else 1f)
            .clip(RoundedCornerShape(10.dp))
    )
}
