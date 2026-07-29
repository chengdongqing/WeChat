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
import androidx.compose.material3.Text
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
import java.util.Date
import java.util.Locale

private val FavoritesBackground = Color(0xFFF1F1F1)
private val CategoryBlue = Color(0xFF526C9A)

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
                .height(54.dp)
                .padding(horizontal = 12.dp),
            onChange = { viewModel.query.value = it }
        )
        Spacer(Modifier.height(18.dp))
        FavoriteCategories(selectedType) { viewModel.selectedType.value = it }
        Spacer(Modifier.height(14.dp))

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
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            "" to "最近使用",
            "RICH_TEXT" to "笔记",
            "VOICE" to "语音",
            "LOCATION" to "位置",
            "MEDIA" to "图片与视频"
        ).forEach { (type, label) ->
            Text(
                label,
                color = if (selected == type) CategoryBlue else Color(0xFF5E6D86),
                fontSize = 16.sp,
                fontWeight = if (selected == type) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier
                    .clickable { onSelect(type) }
                    .padding(vertical = 8.dp)
            )
        }
        Icon(
            painterResource(R.drawable.ic_arrow_down_outlined),
            contentDescription = "更多分类",
            tint = Color(0xFFBBBBBB),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun FavoriteCard(
    item: FavoriteEntity,
    selected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val paths = item.mediaPaths.lineSequence().filter(String::isNotBlank).toList()
    val imagePath = paths.firstOrNull { path ->
        File(path).extension.lowercase() in setOf("jpg", "jpeg", "png", "webp", "gif")
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(18.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (selecting) {
            Checkbox(selected, onCheckedChange = { onClick() })
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier
            .weight(1f)
            .height(if (imagePath != null) 116.dp else 128.dp)) {
            Text(
                item.title.ifBlank { item.content.lineSequence().firstOrNull().orEmpty() }
                    .ifBlank { typeLabel(item.type) },
                color = Color(0xFF171717),
                fontSize = 18.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.content.isNotBlank() && item.content != item.title) {
                Spacer(Modifier.height(7.dp))
                Text(
                    item.content.replace('|', ' '),
                    color = Color(0xFF777777),
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    item.sourceName.ifBlank { typeLabel(item.type) },
                    color = Color(0xFFAAAAAA),
                    fontSize = 13.sp
                )
                Text(
                    formatFavoriteDate(item.updatedAt),
                    color = Color(0xFFAAAAAA),
                    fontSize = 13.sp
                )
            }
        }
        imagePath?.let {
            Spacer(Modifier.width(14.dp))
            AsyncImage(
                model = File(it),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

private fun formatFavoriteDate(timestamp: Long): String =
    SimpleDateFormat("M月d日", Locale.CHINA).format(Date(timestamp))

private fun typeLabel(type: String) = when (type) {
    "VOICE" -> "语音"
    "LOCATION" -> "位置"
    "MEDIA" -> "图片与视频"
    else -> "笔记"
}
