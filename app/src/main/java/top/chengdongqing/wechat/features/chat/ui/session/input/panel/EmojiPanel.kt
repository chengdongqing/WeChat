package top.chengdongqing.wechat.features.chat.ui.session.input.panel

import android.os.Build
import androidx.annotation.DrawableRes
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.decode.StaticImageDecoder
import coil3.request.ImageRequest
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.button.DashedAddButton
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.model.Emoji
import top.chengdongqing.wechat.core.designsystem.model.Emojis
import top.chengdongqing.wechat.core.designsystem.model.Sticker
import top.chengdongqing.wechat.core.designsystem.model.Stickers
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.util.repeatingClickable
import top.chengdongqing.wechat.core.util.asAssetPath
import top.chengdongqing.wechat.core.util.copyAssetToPrivateDir
import top.chengdongqing.wechat.features.chat.domain.model.MessageContent
import top.chengdongqing.wechat.features.chat.theme.ChatTheme

/**
 * 表情面板
 */
@Composable
fun EmojiPanel(
    recentEmojis: List<Emoji>,
    onEmojiSelect: (Emoji) -> Unit,
    onStickerSelect: ((MessageContent.Sticker) -> Unit)? = null,
    onBackspace: () -> Unit
) {
    if (onStickerSelect == null) {
        EmojiGrid(
            recentEmojis = recentEmojis,
            onSelect = onEmojiSelect,
            onBackspace = onBackspace
        )
    } else {
        FullEmojiPanel(
            recentEmojis = recentEmojis,
            onEmojiSelect = onEmojiSelect,
            onStickerSelect = onStickerSelect,
            onBackspace = onBackspace
        )
    }
}

@Composable
private fun FullEmojiPanel(
    recentEmojis: List<Emoji>,
    onEmojiSelect: (Emoji) -> Unit,
    onStickerSelect: ((MessageContent.Sticker) -> Unit)?,
    onBackspace: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )
    val scope = rememberCoroutineScope()

    Column {
        CategoriesTab(
            currentTab = pagerState.currentPage,
            onTabChange = { index ->
                scope.launch {
                    pagerState.scrollToPage(index)
                }
            }
        )

        WeDivider()

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> EmojiGrid(
                    recentEmojis = recentEmojis,
                    onSelect = onEmojiSelect,
                    onBackspace = onBackspace
                )

                1 -> StickersGrid(
                    onSelect = onStickerSelect ?: {}
                )
            }
        }
    }
}

/**
 * 选项卡导航栏
 */
@Composable
private fun CategoriesTab(
    currentTab: Int,
    onTabChange: (index: Int) -> Unit
) {
    val tabs = remember {
        listOf(
            TabItem(0, R.drawable.ic_emoji_outlined, "表情"),
            TabItem(1, R.drawable.ic_like_outlined, "贴纸")
        )
    }

    Row(
        modifier = Modifier
            .zIndex(1f)
            .fillMaxWidth()
            .background(ChatTheme.colorScheme.bottomBarBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { tab ->
            TabButton(
                icon = tab.icon,
                contentDescription = tab.label,
                isSelected = currentTab == tab.index,
                onClick = { onTabChange(tab.index) }
            )
        }
    }
}

/**
 * 选项卡按钮
 */
@Composable
private fun TabButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) ChatTheme.colorScheme.textField else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(30.dp),
            tint = WeTheme.colorScheme.textPrimary
        )
    }
}

/**
 * 表情网格
 *
 * @param recentEmojis 最近使用的表情列表
 * @param onSelect 表情选择回调
 * @param onBackspace 退格键回调
 */
@Composable
private fun EmojiGrid(
    recentEmojis: List<Emoji>,
    onSelect: (Emoji) -> Unit,
    onBackspace: () -> Unit
) {
    val overscrollEffect = rememberBounceOverscrollEffect()

    Box(contentAlignment = Alignment.BottomEnd) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier
                .fillMaxSize()
                .overscroll(overscrollEffect),
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = 8.dp,
                bottom = 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            overscrollEffect = overscrollEffect
        ) {
            // 最近使用部分
            if (recentEmojis.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmojiSectionHeader("最近使用")
                }
                items(
                    items = recentEmojis,
                    key = { "recent_${it.description}" }
                ) { emoji ->
                    EmojiItem(emoji = emoji, onSelect = onSelect)
                }
            }

            // 所有表情部分
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmojiSectionHeader("所有表情")
            }
            items(
                items = Emojis.all,
                key = { it.description }
            ) { emoji ->
                EmojiItem(emoji = emoji, onSelect = onSelect)
            }
        }

        // 退格键悬浮按钮
        BackspaceButton(onBackspace = onBackspace)
    }
}

/**
 * 表情分组标题
 */
@Composable
private fun EmojiSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        color = Color.Gray,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
    )
}

/**
 * 单个表情项
 */
@Composable
private fun EmojiItem(
    emoji: Emoji,
    onSelect: (Emoji) -> Unit
) {
    val model = remember(emoji.localPath) {
        emoji.localPath.asAssetPath
    }

    AsyncImage(
        model = model,
        contentDescription = emoji.description,
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable { onSelect(emoji) }
            .padding(4.dp),
        contentScale = ContentScale.Inside
    )
}

/**
 * 退格按钮（悬浮在右下角）
 */
@Composable
private fun BackspaceButton(onBackspace: () -> Unit) {
    Box(
        modifier = Modifier
            .offset(x = (-12).dp, y = (-22).dp)
            .clip(RoundedCornerShape(8.dp))
            .repeatingClickable { onBackspace() }
            .background(ChatTheme.colorScheme.bottomBarBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = "退格",
            modifier = Modifier.size(22.dp),
            tint = WeTheme.colorScheme.textPrimary
        )
    }
}

/**
 * 贴纸网格
 */
@Composable
private fun StickersGrid(onSelect: (MessageContent.Sticker) -> Unit) {
    val overscrollEffect = rememberBounceOverscrollEffect()

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
        item(key = "add_sticker_button") {
            DashedAddButton(color = WeTheme.colorScheme.textPrimary) {}
        }
        items(
            items = Stickers.all,
            key = { it.localPath }
        ) { sticker ->
            StickerItem(
                sticker = sticker,
                onSelect = onSelect
            )
        }
    }
}

/**
 * 单个贴纸项
 */
@Composable
private fun StickerItem(
    sticker: Sticker,
    onSelect: (MessageContent.Sticker) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 在选择器里面表情图片要保持静态
    val imageRequest = remember(sticker.localPath) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ImageRequest.Builder(context)
                .data(sticker.localPath.asAssetPath)
                .decoderFactory(StaticImageDecoder.Factory())
                .build()
        } else {
            sticker.localPath.asAssetPath
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .clickable {
                scope.launch {
                    // 拷贝到私有目录，已存在则不再次拷贝
                    val localPath = context.copyAssetToPrivateDir(
                        assetName = sticker.localPath
                    ) ?: return@launch

                    onSelect(MessageContent.Sticker(localPath))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            modifier = Modifier.padding(4.dp),
            contentScale = ContentScale.Inside
        )
    }
}

/**
 * 选项卡数据类
 */
private data class TabItem(
    val index: Int,
    @get:DrawableRes val icon: Int,
    val label: String
)