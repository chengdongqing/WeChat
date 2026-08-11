package top.chengdongqing.wechat.feature.contacts.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.designsystem.components.contact.ContactListItem
import top.chengdongqing.wechat.core.designsystem.components.contact.GroupTitle
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.WeContextMenu
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.rememberContextMenuState
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.weContextMenu
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.indexer.AlphabetIndexer
import top.chengdongqing.wechat.core.designsystem.components.loading.WeLoading
import top.chengdongqing.wechat.core.designsystem.overscroll.rememberBouncedOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.theme.LocalAppearanceSetting
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.model.AppLanguage
import top.chengdongqing.wechat.core.model.LocalAiAssistant
import top.chengdongqing.wechat.feature.contacts.R
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
fun ContactListScreen(
    onNewFriends: () -> Unit,
    onGroups: () -> Unit,
    onTags: () -> Unit,
    onDetail: (contactId: String) -> Unit,
    onProfileEdit: (contactId: String) -> Unit,
    viewModel: ContactListViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val overscrollEffect = rememberBouncedOverscrollEffect()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WeTheme.colorScheme.background)
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
                    onNewFriends = onNewFriends,
                    onGroups = onGroups,
                    onTags = onTags
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

                else -> {
                    // 联系人分组列表
                    state.groups.forEach { (initial, contacts) ->
                        item(
                            key = initial,
                            contentType = "Initial"
                        ) {
                            GroupTitle(initial)
                        }

                        items(
                            items = contacts,
                            key = { it.id },
                            contentType = { "ContactItem" }
                        ) { contact ->
                            val contextMenuState = rememberContextMenuState(
                                itemWidthDp = when (LocalAppearanceSetting.current.appLanguage) {
                                    AppLanguage.English -> 160.dp
                                    else -> 140.dp
                                }
                            )
                            val actionLabel = stringResource(R.string.contacts_action_edit_profile)

                            Column(
                                modifier = Modifier.background(WeTheme.colorScheme.surface)
                            ) {
                                ContactListItem(
                                    displayName = contact.displayName,
                                    avatarModel = if (contact.id == LocalAiAssistant.ID) {
                                        DesignR.drawable.img_logo
                                    } else {
                                        contact.avatarPath
                                    },
                                    note = contact.note,
                                    modifier = Modifier.weContextMenu(
                                        onClick = {
                                            onDetail(contact.id)
                                        },
                                        onLongClick = { position ->
                                            if (!contact.isSelf && contact.id != LocalAiAssistant.ID) {
                                                contextMenuState.show(
                                                    position,
                                                    listOf(actionLabel),
                                                    0
                                                )
                                            }
                                        }
                                    )
                                )

                                WeContextMenu(contextMenuState) { _, _ ->
                                    onProfileEdit(contact.id)
                                }

                                WeDivider(modifier = Modifier.padding(start = 68.dp))
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
            text = pluralStringResource(R.plurals.contacts_count, count, count),
            color = WeTheme.colorScheme.textSecondary
        )
    }
}
