package top.chengdongqing.wechat.ui.contacts.list

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.ui.components.divider.WeDivider
import top.chengdongqing.wechat.ui.components.loading.WeLoading
import top.chengdongqing.wechat.ui.contacts.list.components.AlphabetIndexer
import top.chengdongqing.wechat.ui.contacts.list.components.ContactListItem
import top.chengdongqing.wechat.ui.contacts.list.components.TopFunctionList
import top.chengdongqing.wechat.ui.theme.WeChatTheme
import top.chengdongqing.wechat.ui.utils.rememberBounceOverscrollEffect

@Composable
fun ContactListScreen(
    viewModel: ContactListViewModel = hiltViewModel(),
    onNavigateToDetail: (contactId: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val overscrollEffect = rememberBounceOverscrollEffect()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WeChatTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.overscroll(overscrollEffect),
            overscrollEffect = overscrollEffect
        ) {
            // 顶部固定功能项
            item { TopFunctionList() }

            if (state.isLoading) {
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
            } else {
                // 联系人分组列表
                state.groups.forEach { (initial, contacts) ->
                    item { ContactHeader(initial) }

                    itemsIndexed(
                        items = contacts,
                        key = { _, contact -> contact.id },
                        contentType = { _, _ -> "ContactItem" } // 告诉 LazyColumn 哪些项是同一种布局，提高复用效率
                    ) { index, contact ->
                        Column(
                            modifier = Modifier.background(WeChatTheme.colorScheme.surface)
                        ) {
                            ContactListItem(contact) {
                                onNavigateToDetail(contact.id)
                            }

                            if (index < contacts.size - 1) {
                                WeDivider(modifier = Modifier.padding(start = 68.dp))
                            }
                        }
                    }
                }

                item { ContactFooter(state.totalCount) }
            }
        }

        // 右侧字母索引栏
        if (!state.isLoading) {
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
private fun ContactHeader(initial: Char) {
    Text(
        text = initial.toString(),
        modifier = Modifier
            .fillMaxWidth()
            .background(WeChatTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = Color.Gray,
        fontSize = 14.sp
    )
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
            color = WeChatTheme.colorScheme.textSecondary
        )
    }
}