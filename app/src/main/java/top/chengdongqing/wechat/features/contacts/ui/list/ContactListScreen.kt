package top.chengdongqing.wechat.features.contacts.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.components.contact.AlphabetIndexer
import top.chengdongqing.wechat.core.designsystem.components.contact.ContactGroupTitle
import top.chengdongqing.wechat.core.designsystem.components.contact.ContactListItem
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.WeContextMenu
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.rememberContextMenuState
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.weContextMenu
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoading
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.features.contacts.ui.list.components.TopFunctionList

@Composable
fun ContactListScreen(
    onNavigateToNewFriends: () -> Unit,
    onNavigateToDetail: (contactId: String) -> Unit,
    onNavigateToProfileEdit: (contactId: String) -> Unit,
    viewModel: ContactListViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val overscrollEffect = rememberBounceOverscrollEffect()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.overscroll(overscrollEffect),
            overscrollEffect = overscrollEffect
        ) {
            // 顶部功能列表
            item {
                TopFunctionList(
                    pendingCount = state.unreadCount,
                    onNavigateToNewFriends = onNavigateToNewFriends
                )
            }

            when {
                state.isLoading -> {
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

                state.groups.isEmpty() -> {
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
                    state.groups.forEach { (initial, contacts) ->
                        item(
                            key = initial,
                            contentType = "Initial"
                        ) {
                            ContactGroupTitle(initial)
                        }

                        itemsIndexed(
                            items = contacts,
                            key = { _, contact -> contact.id },
                            contentType = { _, _ -> "ContactItem" } // 告诉 LazyColumn 哪些项是同一种布局，提高复用效率
                        ) { index, contact ->
                            val contextMenuState = rememberContextMenuState()

                            Column(
                                modifier = Modifier.background(WeTheme.colorScheme.surface)
                            ) {
                                ContactListItem(
                                    contact = contact,
                                    modifier = Modifier.weContextMenu(
                                        onClick = { onNavigateToDetail(contact.id) },
                                        onLongClick = { position ->
                                            if (!contact.isSelf) {
                                                contextMenuState.show(
                                                    position,
                                                    listOf("设置朋友资料"),
                                                    0
                                                )
                                            }
                                        }
                                    )
                                )

                                WeContextMenu(contextMenuState) { _, _ ->
                                    onNavigateToProfileEdit(contact.id)
                                }

                                if (index < contacts.size - 1) {
                                    WeDivider(modifier = Modifier.padding(start = 68.dp))
                                }
                            }
                        }
                    }

                    // 底部统计
                    item {
                        ContactFooter(state.totalCount)
                    }
                }
            }
        }

        // 右侧字母索引栏
        if (!state.isLoading && state.groups.isNotEmpty()) {
            AlphabetIndexer(state.groups) { initial ->
                state.indexMap[initial]?.let { targetIndex ->
                    scope.launch {
                        listState.scrollToItem(targetIndex)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactFooter(count: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 30.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${count}个朋友",
            color = WeTheme.colorScheme.textSecondary
        )
    }
}