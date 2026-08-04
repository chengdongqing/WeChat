package top.chengdongqing.wechat.feature.contacts.ui.picker

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.checkbox.WeCheckBox
import top.chengdongqing.wechat.core.designsystem.components.contact.ContactListItem
import top.chengdongqing.wechat.core.designsystem.components.contact.GroupTitle
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.indexer.AlphabetIndexer
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoading
import top.chengdongqing.wechat.core.designsystem.modifier.onTap
import top.chengdongqing.wechat.core.designsystem.overscroll.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.model.ContactResult
import top.chengdongqing.wechat.core.model.LocalAiAssistant
import top.chengdongqing.wechat.core.model.toResult
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
fun ContactPicker(
    count: Int,
    excludeSelf: Boolean = false,
    onCancel: () -> Unit,
    viewModel: ContactPickerViewModel = hiltViewModel(),
    onSelect: (contacts: Array<ContactResult>) -> Unit
) {
    LaunchedEffect(excludeSelf) { viewModel.setExcludeSelf(excludeSelf) }
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val overscrollEffect = rememberBounceOverscrollEffect()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contactState by viewModel.contactState.collectAsStateWithLifecycle()
    val (groups, indexMap) = contactState

    Scaffold(
        topBar = {
            TopBar(uiState, onBack = onCancel) {
                val selectedContacts = uiState.selectedContacts.toResult().toTypedArray()
                onSelect(selectedContacts)
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

                    else -> {
                        item {
                            Column(
                                modifier = Modifier.background(WeTheme.colorScheme.surface)
                            ) {
                                GroupChatEntry()
                                WeDivider()
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // 联系人分组列表
                        groups.forEach { (initial, contacts) ->
                            item(
                                key = initial,
                                contentType = "Initial"
                            ) {
                                GroupTitle(initial, background = WeTheme.colorScheme.surface)
                            }

                            itemsIndexed(
                                items = contacts,
                                key = { _, contact -> contact.id },
                                contentType = { _, _ -> "ContactItem" }
                            ) { index, contact ->
                                val isSelected = viewModel.isSelected(contact)

                                Column(
                                    modifier = Modifier.background(WeTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.onTap {
                                            if (uiState.selectedCount >= count && !isSelected) {
                                                context.showToast(
                                                    resources.getString(
                                                        R.string.msg_max_select_limit,
                                                        count
                                                    )
                                                )
                                                return@onTap
                                            }
                                            viewModel.toggleSelection(contact)
                                        },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Spacer(modifier = Modifier.width(16.dp))
                                        WeCheckBox(isSelected)
                                        ContactListItem(
                                            displayName = contact.displayName,
                                            avatarModel = if (contact.id == LocalAiAssistant.ID) {
                                                DesignR.drawable.img_logo
                                            } else {
                                                contact.avatarPath
                                            },
                                            note = contact.note
                                        )
                                    }

                                    if (index < contacts.lastIndex) {
                                        WeDivider(modifier = Modifier.padding(start = 108.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
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
            .clickable {}
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.group_chat_select_title),
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
        "${stringResource(R.string.action_done)}$suffix"
    }

    WeTopAppBar(
        title = stringResource(R.string.contact_select_title),
        onBack = onBack
    ) {
        WeButton(
            text = buttonText,
            size = ButtonSize.Small,
            enabled = isEnabled,
            onClick = onOk
        )
    }
}
