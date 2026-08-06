package top.chengdongqing.wechat.feature.chat.ui.session.input.panel

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.decode.StaticImageDecoder
import coil3.request.ImageRequest
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.common.file.asAssetPath
import top.chengdongqing.wechat.core.common.file.copyAssetToUri
import top.chengdongqing.wechat.core.common.file.deleteFileByUri
import top.chengdongqing.wechat.core.data.model.MessageContent
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.button.DashedAddButton
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.model.Emoji
import top.chengdongqing.wechat.core.designsystem.model.Emojis
import top.chengdongqing.wechat.core.designsystem.modifier.repeatingClickable
import top.chengdongqing.wechat.core.designsystem.overscroll.rememberBouncedOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.model.MessageType
import top.chengdongqing.wechat.feature.chat.data.store.ManagedSticker
import top.chengdongqing.wechat.feature.chat.theme.ChatTheme
import top.chengdongqing.wechat.feature.chat.ui.session.input.InputBarViewModel
import java.io.File

/**
 * 表情面板
 */
@Composable
fun ExpressionPanel(
    recentEmojis: List<Emoji>,
    onEmojiSelect: (Emoji) -> Unit,
    onStickerSelect: ((MessageContent.Sticker) -> Unit)? = null,
    onBackspace: () -> Unit,
    resizeHandle: (@Composable () -> Unit)? = null
) {
    if (onStickerSelect == null) {
        BuiltInEmojiGrid(
            recentEmojis = recentEmojis,
            onSelect = onEmojiSelect,
            onBackspace = onBackspace
        )
    } else {
        EmojiStickerPicker(
            recentEmojis = recentEmojis,
            onEmojiSelect = onEmojiSelect,
            onStickerSelect = onStickerSelect,
            onBackspace = onBackspace,
            resizeHandle = resizeHandle
        )
    }
}

@Composable
private fun EmojiStickerPicker(
    recentEmojis: List<Emoji>,
    onEmojiSelect: (Emoji) -> Unit,
    onStickerSelect: ((MessageContent.Sticker) -> Unit)?,
    onBackspace: () -> Unit,
    resizeHandle: (@Composable () -> Unit)?
) {
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )
    val scope = rememberCoroutineScope()

    Column {
        ExpressionCategoryTabs(
            currentTab = pagerState.currentPage,
            onTabChange = { index ->
                scope.launch {
                    pagerState.scrollToPage(index)
                }
            }
        )

        WeDivider()

        resizeHandle?.invoke()

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> BuiltInEmojiGrid(
                    recentEmojis = recentEmojis,
                    onSelect = onEmojiSelect,
                    onBackspace = onBackspace,
                    topPadding = 0.dp
                )

                1 -> StickerGrid(
                    onSelect = onStickerSelect ?: {},
                    topPadding = 0.dp
                )
            }
        }
    }
}

/**
 * 选项卡导航栏
 */
@Composable
private fun ExpressionCategoryTabs(
    currentTab: Int,
    onTabChange: (index: Int) -> Unit
) {
    val resources = LocalResources.current
    val tabs = remember {
        listOf(
            TabItem(0, R.drawable.ic_emoji_outlined, resources.getString(R.string.sticker_title)),
            TabItem(
                1,
                R.drawable.ic_like_outlined,
                resources.getString(R.string.sticker_custom_title)
            )
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
 * 内置表情网格
 */
@Composable
private fun BuiltInEmojiGrid(
    recentEmojis: List<Emoji>,
    onSelect: (Emoji) -> Unit,
    onBackspace: () -> Unit,
    topPadding: Dp = 8.dp
) {
    val overscrollEffect = rememberBouncedOverscrollEffect()

    Box(contentAlignment = Alignment.BottomEnd) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier
                .fillMaxSize()
                .overscroll(overscrollEffect),
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = topPadding,
                bottom = 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            overscrollEffect = overscrollEffect
        ) {
            // 最近使用部分
            if (recentEmojis.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmojiSectionHeader(stringResource(R.string.sticker_recent))
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
                EmojiSectionHeader(stringResource(R.string.sticker_all))
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
    var showPreview by remember(emoji.description) { mutableStateOf(false) }
    val model = remember(emoji.localPath) {
        emoji.localPath.asAssetPath
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(4.dp))
            .pointerInput(emoji.description) {
                detectTapGestures(
                    onPress = {
                        tryAwaitRelease()
                        showPreview = false
                    },
                    onLongPress = {
                        showPreview = true
                    },
                    onTap = { onSelect(emoji) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = model,
            contentDescription = emoji.description,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentScale = ContentScale.Inside
        )
        if (showPreview) {
            EmojiPressPreview(emoji)
        }
    }
}

@Composable
private fun EmojiPressPreview(emoji: Emoji) {
    val popoverColor = WeTheme.colorScheme.surface

    Popup(
        popupPositionProvider = EmojiPreviewPositionProvider,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            clippingEnabled = false
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.width(112.dp),
                shape = RoundedCornerShape(12.dp),
                color = popoverColor,
                shadowElevation = 10.dp,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = emoji.localPath.asAssetPath,
                        contentDescription = emoji.description,
                        modifier = Modifier.size(72.dp),
                        contentScale = ContentScale.Inside
                    )
                    Text(
                        text = emoji.description,
                        color = WeTheme.colorScheme.textPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
            Canvas(Modifier.size(width = 18.dp, height = 9.dp)) {
                drawPath(
                    Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width / 2f, size.height)
                        close()
                    },
                    color = popoverColor
                )
            }
        }
    }
}

private object EmojiPreviewPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val margin = 8
        val x = (anchorBounds.left + anchorBounds.width / 2 - popupContentSize.width / 2)
            .coerceIn(
                margin,
                (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)
            )
        val above = anchorBounds.top - popupContentSize.height - 6
        val y = if (above >= margin) above else anchorBounds.bottom + 6
        return IntOffset(x, y)
    }
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
            contentDescription = stringResource(R.string.sticker_backspace),
            modifier = Modifier.size(22.dp),
            tint = WeTheme.colorScheme.textPrimary
        )
    }
}

/**
 * 表情贴纸网格
 */
@Composable
private fun StickerGrid(
    onSelect: (MessageContent.Sticker) -> Unit,
    topPadding: Dp = 12.dp,
    viewModel: StickersViewModel = hiltViewModel()
) {
    val overscrollEffect = rememberBouncedOverscrollEffect()
    val stickers by viewModel.stickers.collectAsStateWithLifecycle()
    val addSticker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let(viewModel::add)
        }

    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier
            .fillMaxSize()
            .overscroll(overscrollEffect),
        contentPadding = PaddingValues(
            start = 12.dp,
            top = topPadding,
            end = 12.dp,
            bottom = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        overscrollEffect = overscrollEffect
    ) {
        item(key = "add_sticker_button") {
            DashedAddButton(color = WeTheme.colorScheme.textPrimary) {
                addSticker.launch("image/*")
            }
        }
        items(
            items = stickers,
            key = { it.path }
        ) { sticker ->
            StickerItem(
                sticker = sticker,
                onSelect = onSelect,
                onMoveToFront = { viewModel.moveToFront(sticker) },
                onDelete = { viewModel.delete(sticker) }
            )
        }
    }
}

@Composable
private fun StickerItem(
    sticker: ManagedSticker,
    onSelect: (MessageContent.Sticker) -> Unit,
    onMoveToFront: () -> Unit,
    onDelete: () -> Unit,
    viewModel: InputBarViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPreview by remember { mutableStateOf(false) }

    // 在选择器里面表情图片要保持静态
    val imageRequest = remember(sticker.path, sticker.isAsset) {
        if (sticker.isAsset && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ImageRequest.Builder(context)
                .data(sticker.path.asAssetPath)
                .decoderFactory(StaticImageDecoder.Factory())
                .build()
        } else {
            if (sticker.isAsset) {
                sticker.path.asAssetPath
            } else {
                sticker.path
            }
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .pointerInput(sticker.path) {
                detectTapGestures(
                    onLongPress = {
                        showPreview = true
                    },
                    onTap = {
                        scope.launch { sendSticker(context, sticker, viewModel, onSelect) }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            modifier = Modifier.padding(4.dp),
            contentScale = ContentScale.Inside
        )

        if (showPreview) {
            StickerPressPreview(
                sticker = sticker,
                onDismiss = { showPreview = false },
                onMoveToFront = {
                    onMoveToFront()
                    showPreview = false
                },
                onDelete = {
                    onDelete()
                    showPreview = false
                }
            )
        }
    }
}

@Composable
private fun StickerPressPreview(
    sticker: ManagedSticker,
    onDismiss: () -> Unit,
    onMoveToFront: () -> Unit,
    onDelete: () -> Unit
) {
    val model = if (sticker.isAsset) sticker.path.asAssetPath else sticker.path
    val popoverColor = WeTheme.colorScheme.surface
    Popup(
        popupPositionProvider = StickerPreviewPositionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            clippingEnabled = false
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = popoverColor,
                shadowElevation = 8.dp,
                tonalElevation = 1.dp
            ) {
                Column(Modifier.width(174.dp)) {
                    AsyncImage(
                        model = model,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .padding(12.dp),
                        contentScale = ContentScale.Inside
                    )
                    WeDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = onMoveToFront) {
                            Text("移到最前", color = WeTheme.colorScheme.textPrimary)
                        }
                        TextButton(onClick = onDelete) {
                            Text("删除", color = Color(0xFFFA5151))
                        }
                    }
                }
            }
            Canvas(Modifier.size(width = 18.dp, height = 9.dp)) {
                drawPath(
                    Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width / 2f, size.height)
                        close()
                    },
                    color = popoverColor
                )
            }
        }
    }
}

private object StickerPreviewPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = (anchorBounds.left + anchorBounds.width / 2 - popupContentSize.width / 2)
            .coerceIn(8, (windowSize.width - popupContentSize.width - 8).coerceAtLeast(8))
        val above = anchorBounds.top - popupContentSize.height - 8
        val y = if (above >= 8) above else anchorBounds.bottom + 8
        return IntOffset(x, y)
    }
}

private suspend fun sendSticker(
    context: android.content.Context,
    sticker: ManagedSticker,
    viewModel: InputBarViewModel,
    onSelect: (MessageContent.Sticker) -> Unit
) {
    if (!sticker.isAsset) {
        onSelect(MessageContent.Sticker(sticker.path))
        return
    }
    @Suppress("BlockingMethodInNonBlockingContext")
    val tempFile = File.createTempFile("Sticker_", ".gif")
    val uri = context.copyAssetToUri(
        assetName = sticker.path,
        targetFile = tempFile
    ) ?: return
    val localPath = viewModel.privateFileManager.saveMedia(
        messageType = MessageType.Sticker,
        sourceUri = uri
    ).getOrThrow()
    context.deleteFileByUri(uri)
    onSelect(MessageContent.Sticker(localPath))
}

/**
 * 选项卡数据类
 */
private data class TabItem(
    val index: Int,
    @get:DrawableRes val icon: Int,
    val label: String
)
