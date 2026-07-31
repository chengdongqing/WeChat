package top.chengdongqing.wechat.feature.profile.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.database.entity.FavoriteEntity
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.searchbar.WeSearchBar
import top.chengdongqing.wechat.core.navigation.LocalContactPickerLauncher
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val FavoritesBackground = Color(0xFFEDEDED)
private val CategorySelected = Color(0xFF000000)
private val CategoryUnselected = Color(0xFF888888)

@Composable
fun FavoritesScreen(
    targetChatId: String?,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val pagingItems = viewModel.favorites.collectAsLazyPagingItems()
    val selected by viewModel.selected.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val query by viewModel.query.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val selecting = selectionMode || targetChatId != null
    val pickContacts = LocalContactPickerLauncher.current.rememberLauncher { contacts ->
        viewModel.forwardSelected(contacts.map { it.id }.toSet())
    }

    Column(Modifier
        .fillMaxSize()
        .background(FavoritesBackground)) {
        WeTopAppBar(
            title = if (selecting) "已选择 ${selected.size} 项" else "收藏",
            containerColor = FavoritesBackground,
            onBack = { if (selectionMode) viewModel.clearSelection() else onBack() },
            actions = {
                when {
                    selected.isNotEmpty() && targetChatId != null ->
                        TextButton("发送") { viewModel.forwardSelected(targetChatId, onBack) }

                    selected.isNotEmpty() -> {
                        IconButton(R.drawable.ic_forward_outlined, description = "转发") {
                            pickContacts(99)
                        }
                        IconButton(R.drawable.ic_delete_outlined, description = "删除") {
                            viewModel.deleteSelected()
                        }
                    }

                    targetChatId == null -> IconButton(
                        R.drawable.ic_plus_circle_outlined,
                        description = "新建",
                        onClick = onCreate
                    )
                }
            }
        )

        WeSearchBar(
            value = query,
            placeholder = "搜索",
            backgroundColor = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            onChange = { viewModel.query.value = it }
        )
        FavoriteCategories(selectedType) { viewModel.selectedType.value = it }
        Spacer(Modifier.height(10.dp))

        if (pagingItems.itemCount == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无收藏", color = Color(0xFFAAAAAA), fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    count = pagingItems.itemCount,
                    key = { index -> pagingItems[index]?.id ?: index }
                ) { index ->
                    pagingItems[index]?.let { item ->
                        FavoriteCard(
                            item = item,
                            selected = item.id in selected,
                            selecting = selecting,
                            onClick = {
                                if (selecting) viewModel.toggle(item.id) else onOpen(item.id)
                            },
                            onLongClick = {
                                viewModel.enterSelectionMode()
                                viewModel.toggle(item.id)
                            },
                            onDelete = {
                                viewModel.toggle(item.id)
                                viewModel.deleteSelected()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteCategories(selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_tag_filled),
            contentDescription = "标签",
            tint = if (selected == "TAG") CategorySelected else CategoryUnselected,
            modifier = Modifier
                .size(20.dp)
                .clickable { onSelect("TAG") }
        )

        listOf(
            "" to "全部",
            "RICH_TEXT" to "笔记",
            "VOICE" to "语音",
            "LOCATION" to "位置",
            "MEDIA" to "图片与视频",
            "LINK" to "链接",
            "FILE" to "文件"
        ).forEach { (type, label) ->
            Text(
                label,
                color = if (selected == type) CategorySelected else CategoryUnselected,
                fontSize = 15.sp,
                fontWeight = if (selected == type) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .clickable { onSelect(type) }
                    .padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun FavoriteCard(
    item: FavoriteEntity,
    selected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Red)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text("删除", color = Color.White, fontSize = 16.sp)
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        val paths = item.mediaPaths.lineSequence().filter(String::isNotBlank).toList()
        val imagePath = paths.firstOrNull { path ->
            File(path).extension.lowercase() in setOf("jpg", "jpeg", "png", "webp", "gif")
        }
        val videoPath = paths.firstOrNull { path ->
            File(path).extension.lowercase() in setOf("mp4", "mov", "avi", "3gp", "mkv")
        }
        val isVideo = item.type == "MEDIA" && videoPath != null
        val isFile = item.type == "FILE"

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (selecting) {
                Checkbox(selected, onCheckedChange = { onClick() })
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    item.title.ifBlank { item.content.lineSequence().firstOrNull().orEmpty() }
                        .ifBlank { typeLabel(item.type) },
                    color = Color(0xFF191919),
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isFile && item.content.isNotBlank() && item.content != item.title) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.content.replace('|', ' '),
                        color = Color(0xFF888888),
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        (if (item.sourceName.isNotBlank()) "来自: ${item.sourceName}" else typeLabel(
                            item.type
                        )),
                        color = Color(0xFFB1B1B1),
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        formatFavoriteDate(item.updatedAt),
                        color = Color(0xFFB1B1B1),
                        fontSize = 12.sp
                    )
                }
            }
            if (imagePath != null || videoPath != null) {
                Spacer(Modifier.width(16.dp))
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = File(imagePath ?: videoPath!!),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    if (isVideo) {
                        Icon(
                            painter = painterResource(R.drawable.ic_play_filled),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            } else if (isFile) {
                Spacer(Modifier.width(16.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_file_filled),
                    contentDescription = null,
                    tint = Color(0xFFC1C1C1),
                    modifier = Modifier.size(60.dp)
                )
            }
        }
    }
}

private fun formatFavoriteDate(timestamp: Long): String {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = timestamp }

    val diff = now.timeInMillis - target.timeInMillis

    return when {
        diff < 60 * 1000 -> "刚刚"
        isSameDay(now, target) -> SimpleDateFormat("HH:mm", Locale.CHINA).format(target.time)
        isYesterday(now, target) -> "昨天"
        isSameWeek(now, target) -> getWeekday(target)
        else -> SimpleDateFormat("M月d日", Locale.CHINA).format(target.time)
    }
}

private fun isSameDay(c1: Calendar, c2: Calendar) =
    c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(
        Calendar.DAY_OF_YEAR
    )

private fun isYesterday(c1: Calendar, c2: Calendar): Boolean {
    val yesterday = Calendar.getInstance().apply {
        timeInMillis = c1.timeInMillis
        add(Calendar.DAY_OF_YEAR, -1)
    }
    return isSameDay(yesterday, c2)
}

private fun isSameWeek(c1: Calendar, c2: Calendar): Boolean {
    val weekAgo = Calendar.getInstance().apply {
        timeInMillis = c1.timeInMillis
        add(Calendar.DAY_OF_YEAR, -7)
    }
    return c2.after(weekAgo) && !isSameDay(c1, c2)
}

private fun getWeekday(calendar: Calendar): String {
    return when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> "星期日"
        Calendar.MONDAY -> "星期一"
        Calendar.TUESDAY -> "星期二"
        Calendar.WEDNESDAY -> "星期三"
        Calendar.THURSDAY -> "星期四"
        Calendar.FRIDAY -> "星期五"
        Calendar.SATURDAY -> "星期六"
        else -> ""
    }
}

private fun typeLabel(type: String) = when (type) {
    "VOICE" -> "语音"
    "LOCATION" -> "位置"
    "MEDIA" -> "图片与视频"
    "LINK" -> "链接"
    "FILE" -> "文件"
    else -> "笔记"
}
