package top.chengdongqing.wechat.ui.chat.session.input.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.sticker.Emoji
import top.chengdongqing.wechat.data.sticker.Emojis
import top.chengdongqing.wechat.data.sticker.Stickers
import top.chengdongqing.wechat.ui.components.WeDivider
import top.chengdongqing.wechat.ui.theme.Black
import top.chengdongqing.wechat.ui.utils.BounceOverscrollEffect
import top.chengdongqing.wechat.ui.utils.repeatingClickable

@Composable
fun EmojiPanel(
    emojiOnly: Boolean = false,
    onEmojiSelect: (value: Emoji) -> Unit,
    onStickerSelect: ((sticker: String) -> Unit)?,
    onBackspace: () -> Unit
) {
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    if (emojiOnly) {
        EmojiGrid(onSelect = onEmojiSelect, onBackspace = onBackspace)
    } else {
        Column {
            CategoriesTab(pagerState.currentPage) {
                scope.launch {
                    pagerState.scrollToPage(it)
                }
            }
            WeDivider()
            HorizontalPager(pagerState) { page ->
                if (page == 0) {
                    EmojiGrid(onSelect = onEmojiSelect, onBackspace = onBackspace)
                } else {
                    StickersGrid {
                        onStickerSelect?.invoke(it)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoriesTab(currentTab: Int, onTabChange: (index: Int) -> Unit) {
    val tabs = remember {
        listOf(
            R.drawable.ic_emoji_outlined,
            R.drawable.ic_like_outlined
        )
    }

    Row(
        modifier = Modifier
            .zIndex(1f)
            .fillMaxWidth()
            .background(Color(0xFFF1F1F1))
            .padding(16.dp, 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, icon ->
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (currentTab == index) Color.White else Color.Transparent)
                    .clickable {
                        onTabChange(index)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
private fun EmojiGrid(
    recentEmojis: List<Emoji> = Emojis.take(8),
    onSelect: (value: Emoji) -> Unit,
    onBackspace: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val overscrollEffect = remember(scope) { BounceOverscrollEffect(scope) }

    Box(contentAlignment = Alignment.BottomEnd) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier
                .fillMaxSize()
                .overscroll(overscrollEffect),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            overscrollEffect = overscrollEffect
        ) {
            if (recentEmojis.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmojiSectionHeader("最近使用")
                }
                items(recentEmojis, key = { "recent_${it.description}" }) { emoji ->
                    EmojiItem(emoji, onSelect)
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                EmojiSectionHeader("所有表情")
            }
            items(items = Emojis, key = { it.description }) { emoji ->
                EmojiItem(emoji, onSelect)
            }
        }

        // 退格键悬浮在右下角
        BackspaceButton(onBackspace)
    }
}

@Composable
private fun EmojiSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        color = Color.Gray,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp)
    )
}

@Composable
private fun EmojiItem(emoji: Emoji, onSelect: (Emoji) -> Unit) {
    AsyncImage(
        model = emoji.iconPath.asAssetPath,
        contentDescription = emoji.description,
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable { onSelect(emoji) }
            .padding(4.dp),
        contentScale = ContentScale.Inside
    )
}

@Composable
private fun BackspaceButton(onBackspace: () -> Unit) {
    Box(
        Modifier
            .offset(x = (-12).dp, y = (-22).dp)
            .clip(RoundedCornerShape(8.dp))
            .repeatingClickable { onBackspace() }
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = "退格",
            modifier = Modifier.size(22.dp),
            tint = Black
        )
    }
}

@Composable
private fun StickersGrid(onSelect: (sticker: String) -> Unit) {
    val scope = rememberCoroutineScope()
    val overscrollEffect = remember { BounceOverscrollEffect(scope) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier
            .fillMaxSize()
            .overscroll(overscrollEffect),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        overscrollEffect = overscrollEffect
    ) {
        items(items = Stickers, key = { it }) { sticker ->
            Box(
                Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        onSelect(sticker)
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = sticker.asAssetPath,
                    contentDescription = null,
                    modifier = Modifier.padding(4.dp),
                    contentScale = ContentScale.Inside
                )
            }
        }
    }
}

private val String.asAssetPath: String
    get() = "file:///android_asset/$this"