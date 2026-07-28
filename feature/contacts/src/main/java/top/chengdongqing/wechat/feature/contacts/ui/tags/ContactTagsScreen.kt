package top.chengdongqing.wechat.feature.contacts.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.database.entity.ContactEntity
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.checkbox.WeCheckBox
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

private val WeChatGreen = Color(0xFF07C160)

@Composable
fun ContactTagsScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: ContactTagsViewModel = hiltViewModel()
) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            WeTopAppBar(title = "标签", onBack = onBack) {
                IconButton(onClick = onCreate) {
                    Icon(
                        painterResource(R.drawable.ic_plus_outlined),
                        contentDescription = "新建标签",
                        tint = WeTheme.colorScheme.textPrimary
                    )
                }
            }
        },
        containerColor = WeTheme.colorScheme.background
    ) { padding ->
        if (tags.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("暂无标签", color = WeTheme.colorScheme.textSecondary, fontSize = 16.sp)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onCreate,
                    colors = ButtonDefaults.buttonColors(containerColor = WeChatGreen)
                ) { Text("新建标签") }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                item {
                    Text(
                        "通过标签可以更方便地查找和管理联系人",
                        color = WeTheme.colorScheme.textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                items(tags, key = { it.id }) { tag ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(WeTheme.colorScheme.surface)
                            .clickable { onEdit(tag.id) }
                            .padding(horizontal = 16.dp, vertical = 17.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tag.name, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Text(
                            "${tag.memberCount}",
                            color = WeTheme.colorScheme.textSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            painterResource(R.drawable.ic_right_outlined),
                            null,
                            tint = WeTheme.colorScheme.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    WeDivider(Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}

@Composable
fun ContactTagEditorScreen(
    tagId: String?,
    onBack: () -> Unit,
    viewModel: ContactTagEditorViewModel = hiltViewModel {
        factory: ContactTagEditorViewModel.Factory -> factory.create(tagId)
    }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        viewModel.events.collect {
            when (it) {
                TagEditorEvent.Saved, TagEditorEvent.Deleted -> onBack()
                is TagEditorEvent.Error -> error = it.message
            }
        }
    }
    Scaffold(
        topBar = {
            WeTopAppBar(
                title = if (tagId == null) "新建标签" else "编辑标签",
                onBack = onBack,
                backText = "取消"
            ) {
                Text(
                    "完成",
                    color = if (state.name.isBlank()) Color.Gray else WeChatGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(
                        enabled = state.name.isNotBlank() && !state.saving,
                        onClick = viewModel::save
                    ).padding(10.dp)
                )
            }
        },
        containerColor = WeTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "标签名字",
                color = WeTheme.colorScheme.textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 6.dp)
            )
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                singleLine = true,
                modifier = Modifier.fillMaxWidth().background(WeTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp)
            )
            Text(
                "标签成员（${state.selectedIds.size}）",
                color = WeTheme.colorScheme.textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp, 18.dp, 16.dp, 8.dp)
            )
            ContactCheckList(
                contacts = state.contacts,
                selectedIds = state.selectedIds,
                onToggle = viewModel::toggle,
                modifier = Modifier.weight(1f)
            )
            if (tagId != null) {
                Text(
                    "删除标签",
                    color = Color(0xFFFA5151),
                    fontSize = 17.sp,
                    modifier = Modifier.fillMaxWidth().background(WeTheme.colorScheme.surface)
                        .clickable { confirmDelete = true }.padding(18.dp)
                )
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除标签？") },
            text = { Text("删除标签不会删除标签中的联系人。") },
            confirmButton = {
                TextButton(onClick = viewModel::delete) { Text("删除", color = Color(0xFFFA5151)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
        )
    }
    error?.let {
        AlertDialog(
            onDismissRequest = { error = null },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("确定") } }
        )
    }
}

@Composable
fun ContactTagPickerScreen(
    contactId: String,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    viewModel: ContactTagPickerViewModel = hiltViewModel {
        factory: ContactTagPickerViewModel.Factory -> factory.create(contactId)
    }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            WeTopAppBar(title = "设置标签", onBack = onBack, backText = "取消") {
                Text(
                    "完成",
                    color = WeChatGreen,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable {
                        scope.launch { viewModel.save(); onBack() }
                    }.padding(10.dp)
                )
            }
        },
        containerColor = WeTheme.colorScheme.background
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Row(
                    Modifier.fillMaxWidth().background(WeTheme.colorScheme.surface)
                        .clickable(onClick = onCreate).padding(17.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("+", color = WeChatGreen, fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("新建标签", color = WeChatGreen, fontSize = 16.sp)
                }
                Spacer(Modifier.height(8.dp))
            }
            items(state.tags, key = { it.id }) { tag ->
                Row(
                    Modifier.fillMaxWidth().background(WeTheme.colorScheme.surface)
                        .clickable { viewModel.toggle(tag.id) }.padding(17.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tag.name, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    WeCheckBox(tag.id in state.selectedIds)
                }
                WeDivider(Modifier.padding(start = 16.dp))
            }
        }
    }
}

@Composable
private fun ContactCheckList(
    contacts: List<ContactEntity>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier) {
        items(contacts, key = { it.id }) { contact ->
            Row(
                Modifier.fillMaxWidth().background(WeTheme.colorScheme.surface)
                    .clickable { onToggle(contact.id) }.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = contact.avatarPath ?: R.drawable.img_avatar_placeholder,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Text(contact.displayName, fontSize = 16.sp, modifier = Modifier.weight(1f))
                WeCheckBox(contact.id in selectedIds)
            }
            WeDivider(Modifier.padding(start = 70.dp))
        }
    }
}
