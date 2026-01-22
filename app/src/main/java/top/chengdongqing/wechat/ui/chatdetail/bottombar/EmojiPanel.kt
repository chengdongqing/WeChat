package top.chengdongqing.wechat.ui.chatdetail.bottombar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
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
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.sticker.Emoji
import top.chengdongqing.wechat.data.sticker.Emojis
import top.chengdongqing.wechat.data.sticker.Stickers
import top.chengdongqing.wechat.ui.components.WeDivider
import top.chengdongqing.wechat.ui.theme.Black

@Composable
fun EmojiPanel(
    onEmojiSelect: (value: Emoji) -> Unit,
    onStickerSelect: (sticker: String) -> Unit,
    onBackspace: () -> Unit
) {
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    Column {
        CategoriesTab(pagerState.currentPage) {
            scope.launch {
                pagerState.scrollToPage(it)
            }
        }
        WeDivider()
        HorizontalPager(pagerState) { page ->
            if (page == 0) {
                EmojiGrid(onEmojiSelect, onBackspace)
            } else {
                StickersGrid(onStickerSelect)
            }
        }
    }
}

@Composable
private fun CategoriesTab(currentTab: Int, onTabChange: (index: Int) -> Unit) {
    val tabs = remember {
        listOf(
            R.drawable.ic_sticker_outlined,
            R.drawable.ic_like_outlined
        )
    }

    Row(
        Modifier.padding(16.dp, 8.dp),
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
private fun EmojiGrid(onSelect: (value: Emoji) -> Unit, onBackspace: () -> Unit) {
    Box(contentAlignment = Alignment.BottomEnd) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(Emojis) { emoji ->
                AsyncImage(
                    model = emoji.icon.asAssetPath,
                    contentDescription = emoji.description,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onSelect(emoji) }
                        .padding(4.dp),
                    contentScale = ContentScale.Inside
                )
            }
        }

        BackspaceButton(onBackspace)
    }
}

@Composable
private fun BackspaceButton(onBackspace: () -> Unit) {
    Box(
        Modifier
            .zIndex(1f)
            .size(102.dp, 66.dp)
            .padding(end = 12.dp, bottom = 18.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onBackspace() }
                .background(Color.White)
                .padding(12.dp, 6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "回退",
                modifier = Modifier.size(28.dp),
                tint = Black
            )
        }
    }
}

@Composable
private fun StickersGrid(onSelect: (sticker: String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(Stickers) { sticker ->
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