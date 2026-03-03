package top.chengdongqing.wechat.features.contacts.ui.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.checkbox.WeCheckBox
import top.chengdongqing.wechat.core.designsystem.components.contact.AlphabetIndexer
import top.chengdongqing.wechat.core.designsystem.components.contact.ContactGroupTitle
import top.chengdongqing.wechat.core.designsystem.components.contact.ContactListItem
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoading
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.util.weClickable
import top.chengdongqing.wechat.core.util.showToast

@Composable
fun ContactPicker(
    count: Int,
    onCancel: () -> Unit,
    viewModel: ContactPickerViewModel = hiltViewModel(),
    onSelect: (chatIds: Set<String>, isGroupChat: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val overscrollEffect = rememberBounceOverscrollEffect()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contactState by viewModel.contactState.collectAsStateWithLifecycle()
    val (groups, indexMap) = contactState

    Scaffold(
        topBar = {
            TopBar(uiState, onBack = onCancel) {
                onSelect(uiState.selectedContactIds, false)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WeTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.overscroll(overscrollEffect),
                overscrollEffect = overscrollEffect
            ) {
                item {
                    Column(
                        modifier = Modifier.background(White)
                    ) {
                        GroupChatEntry()
                        WeDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                when {
                    uiState.isLoading -> {
                        // 加载中
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                WeLoading()
                            }
                        }
                    }

                    groups.isEmpty() -> {
                        // 空状态
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无联系人",
                                    color = WeTheme.colorScheme.textSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    else -> {
                        // 联系人分组列表
                        groups.forEach { (initial, contacts) ->
                            item(
                                key = initial,
                                contentType = "Initial"
                            ) {
                                ContactGroupTitle(initial, background = White)
                            }

                            itemsIndexed(
                                items = contacts,
                                key = { _, contact -> contact.id },
                                contentType = { _, _ -> "ContactItem" }
                            ) { index, contact ->
                                val isSelected = viewModel.isContactSelected(contact.id)

                                Column(
                                    modifier = Modifier.background(WeTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.weClickable {
                                            println("---uiState.selectedCount:${uiState.selectedCount}, count:$count")

                                            if (uiState.selectedCount >= count && !isSelected) {
                                                context.showToast("你最多只能选择${count}个")
                                                return@weClickable
                                            }
                                            viewModel.toggleContactSelection(contact.id)
                                        },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Spacer(modifier = Modifier.width(16.dp))
                                        WeCheckBox(isSelected)
                                        ContactListItem(contact)
                                    }

                                    if (index < contacts.size - 1) {
                                        WeDivider(modifier = Modifier.padding(start = 108.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 右侧字母索引栏
            if (!uiState.isLoading && groups.isNotEmpty()) {
                AlphabetIndexer(groups) { initial ->
                    indexMap[initial]?.let { targetIndex ->
                        scope.launch {
                            listState.scrollToItem(targetIndex)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupChatEntry() {
    Row(
        modifier = Modifier
            .height(60.dp)
            .fillMaxWidth()
            .background(White)
            .clickable {}
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "选择群聊",
            fontSize = 15.sp,
            color = WeTheme.colorScheme.textPrimary
        )
    }
}

@Composable
private fun TopBar(
    uiState: ContactPickerUiState,
    onBack: () -> Unit,
    onOk: () -> Unit
) {
    val isEnabled = uiState.selectedCount > 0
    val buttonText = run {
        val suffix = if (isEnabled) "(${uiState.selectedCount})" else ""
        "完成$suffix"
    }

    WeTopBar(title = "选择联系人", onBack = onBack) {
        WeButton(
            text = buttonText,
            size = ButtonSize.Small,
            enabled = isEnabled,
            onClick = onOk
        )
    }
}