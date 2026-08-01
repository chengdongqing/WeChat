package top.chengdongqing.wechat.feature.chat.ui.file

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.common.file.FileMetadata
import top.chengdongqing.wechat.core.common.file.getFileMetadata
import top.chengdongqing.wechat.core.common.media.model.MediaItem
import top.chengdongqing.wechat.core.common.media.model.MediaType
import top.chengdongqing.wechat.core.common.media.preview.previewMedias
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonType
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.checkbox.WeCheckBox
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.permission.RequestMediaPermission
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileSelectionScreen(
    viewModel: FileSelectionViewModel = hiltViewModel(),
    onCancel: () -> Unit,
    onConfirm: (List<Uri>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selected = remember { mutableStateListOf<FileMetadata>() }
    val pagerState = rememberPagerState(pageCount = { 4 })
    var expanded by remember { mutableStateOf(false) }
    val fileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            scope.launch {
                uris.forEach { uri ->
                    if (selected.none { it.uri == uri }) {
                        context.getFileMetadata(uri)?.let(selected::add)
                    }
                }
            }

            if (uris.isNotEmpty()) {
                expanded = true
            }
        }

    BackHandler {
        if (expanded) {
            expanded = false
        } else {
            onCancel()
        }
    }

    Scaffold(
        topBar = {
            Header(onCancel, pagerState.currentPage == 0)
        },
        containerColor = WeTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Tabs(pagerState) { page ->
                scope.launch {
                    pagerState.scrollToPage(page)
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                Box(Modifier.fillMaxSize()) {
                    when (page) {
                        0 -> ChatFileList(state.chatFiles, state.loadingChat, selected)
                        1 -> EmptyText("暂无可选择的收藏文件")
                        2 -> RequestMediaPermission {
                            LaunchedEffect(Unit) { viewModel.refreshMedia() }
                            MediaGrid(state.mediaFiles, state.loadingMedia, selected)
                        }

                        else -> PhoneFiles {
                            fileLauncher.launch("*/*")
                        }
                    }
                }
            }
            SelectionBar(
                count = selected.size,
                onToggle = { if (selected.isNotEmpty()) expanded = true }
            ) {
                onConfirm(selected.map { it.uri })
            }
        }

        if (expanded) {
            ModalBottomSheet(
                onDismissRequest = { expanded = false },
                containerColor = WeTheme.colorScheme.elevated
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(.74f)
                ) {
                    SelectionBar(
                        count = selected.size,
                        containerColor = WeTheme.colorScheme.elevated,
                        onToggle = { expanded = false }
                    ) {
                        onConfirm(selected.map { it.uri })
                    }
                    LazyColumn {
                        items(selected, key = { it.uri.toString() }) {
                            FileRow(
                                file = it,
                                checked = true
                            ) {
                                selected.remove(it)
                                if (selected.isEmpty()) {
                                    expanded = false
                                }
                            }
                            WeDivider(modifier = Modifier.padding(start = 56.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    onCancel: () -> Unit,
    search: Boolean
) {
    WeTopAppBar(
        title = "选择文件",
        containerColor = WeTheme.colorScheme.surface,
        onBack = onCancel
    ) {
        if (search) {
            IconButton(R.drawable.ic_search_outlined)
        }
    }
}

@Composable
private fun Tabs(pagerState: PagerState, onSelect: (Int) -> Unit) {
    val tabs = remember { listOf("聊天", "收藏", "手机相册", "手机文件") }
    val positions = remember { mutableStateListOf(0, 0, 0, 0) }
    val widths = remember { mutableStateListOf(0, 0, 0, 0) }
    val density = LocalDensity.current
    val selected = pagerState.currentPage
    val indicatorBounds by remember(pagerState) {
        derivedStateOf {
            val page = pagerState.currentPage
            val fraction = pagerState.currentPageOffsetFraction
            val target = (page + if (fraction > 0f) 1 else if (fraction < 0f) -1 else 0)
                .coerceIn(tabs.indices)
            val progress = kotlin.math.abs(fraction).coerceIn(0f, 1f)
            val x = positions[page] +
                    ((positions[target] - positions[page]) * progress).toInt()
            val width = widths[page] +
                    ((widths[target] - widths[page]) * progress).toInt()
            x to width
        }
    }
    val (indicatorX, indicatorWidthPx) = indicatorBounds

    Box(
        modifier = Modifier
            .height(52.dp)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            tabs.forEachIndexed { index, text ->
                val isSelected = selected == index

                Column(
                    Modifier
                        .fillMaxHeight()
                        .onGloballyPositioned { coordinates ->
                            positions[index] = coordinates.positionInParent().x.toInt()
                            widths[index] = coordinates.size.width
                        }
                        .clickable { onSelect(index) }
                        .padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = text,
                        color = if (isSelected) {
                            WeTheme.colorScheme.textPrimary
                        } else {
                            WeTheme.colorScheme.textSecondary
                        },
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        if (indicatorWidthPx > 0) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .offset { IntOffset(indicatorX, 0) }
                    .width(with(density) { indicatorWidthPx.toDp() })
                    .height(2.dp)
                    .background(WeTheme.colorScheme.textPrimary)
            )
        }
    }
}

@Composable
private fun ChatFileList(
    files: List<FileMetadata>,
    loading: Boolean,
    selected: MutableList<FileMetadata>
) {
    when {
        loading -> EmptyText("正在加载聊天文件…")
        files.isEmpty() -> EmptyText("暂无聊天文件")
        else -> LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
            item {
                Text(
                    text = "全部",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    color = WeTheme.colorScheme.textSecondary,
                    fontSize = 14.sp
                )
            }
            items(files, key = { item -> item.uri.toString() }) { file ->
                FileRow(
                    file = file,
                    checked = selected.any { it.uri == file.uri }
                ) {
                    toggle(selected, file)
                }
                WeDivider(modifier = Modifier.padding(start = 56.dp))
            }
        }
    }
}

@Composable
private fun MediaGrid(
    files: List<FileMetadata>,
    loading: Boolean,
    selected: MutableList<FileMetadata>
) {
    val context = LocalContext.current
    val previewItems = remember(files) {
        files.map { file ->
            MediaItem(
                uri = file.uri,
                filename = file.filename,
                mediaType = if (file.mimeType.startsWith("video/")) MediaType.Video else MediaType.Image,
                mimeType = file.mimeType,
                width = file.width,
                height = file.height,
                size = file.size,
                duration = file.duration
            )
        }
    }

    when {
        loading -> EmptyText("正在读取手机相册…")
        files.isEmpty() -> EmptyText("手机相册中暂无媒体文件")
        else -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(files, key = { _, file -> file.uri.toString() }) { index, file ->
                    Column(modifier = Modifier.clickable {
                        context.previewMedias(previewItems, index)
                    }) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        ) {
                            AsyncImage(
                                file.uri,
                                file.filename,
                                Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .clickable {
                                        toggle(selected, file)
                                    }
                            ) {
                                WeCheckBox(checked = selected.any { it.uri == file.uri })
                            }
                        }
                        Text(
                            file.filename,
                            modifier = Modifier.padding(top = 5.dp, start = 3.dp),
                            color = WeTheme.colorScheme.textPrimary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = sizeText(file.size),
                            modifier = Modifier.padding(start = 3.dp),
                            color = WeTheme.colorScheme.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneFiles(onPick: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        Arrangement.Center,
        Alignment.CenterHorizontally
    ) {
        Text(
            text = "你需要从手机中选择文件",
            color = WeTheme.colorScheme.textSecondary,
            fontSize = 15.sp
        )
        Spacer(Modifier.height(24.dp))
        WeButton(
            text = "选取",
            type = ButtonType.Plain,
            onClick = onPick
        )
    }
}

@Composable
private fun FileRow(
    file: FileMetadata,
    checked: Boolean,
    action: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = action)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WeCheckBox(checked)
        Spacer(Modifier.width(18.dp))
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(WeTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_file_filled),
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                tint = WeTheme.colorScheme.divider
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 18.dp)
        ) {
            Text(
                text = file.filename,
                color = WeTheme.colorScheme.textPrimary,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                sizeText(file.size),
                color = WeTheme.colorScheme.textSecondary,
                fontSize = 13.sp
            )
        }

        if (checked) {
            Text(
                text = "移除",
                color = WeTheme.colorScheme.link,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    containerColor: Color = WeTheme.colorScheme.background,
    onToggle: () -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(onClick = onToggle)
            .background(containerColor)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(WeTheme.colorScheme.divider),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_down_outlined),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = WeTheme.colorScheme.textSecondary
            )
        }
        if (count > 0) {
            Text(
                text = "已选中${count}个文件",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                color = WeTheme.colorScheme.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        WeButton(
            text = "发送",
            size = ButtonSize.Small,
            enabled = count > 0,
            onClick = onSend
        )
    }
}

@Composable
private fun EmptyText(text: String) =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = WeTheme.colorScheme.textSecondary,
            fontSize = 16.sp
        )
    }

private fun toggle(selected: MutableList<FileMetadata>, file: FileMetadata) {
    selected.indexOfFirst { it.uri == file.uri }.takeIf { it >= 0 }?.let(selected::removeAt)
        ?: selected.add(file)
}

private fun sizeText(bytes: Long) = when {
    bytes >= 1048576 -> "%.1f MB".format(bytes / 1048576f)
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024f)
    else -> "$bytes B"
}
