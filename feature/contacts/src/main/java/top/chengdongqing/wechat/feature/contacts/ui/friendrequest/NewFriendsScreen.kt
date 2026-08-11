package top.chengdongqing.wechat.feature.contacts.ui.friendrequest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonType
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.WeContextMenu
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.rememberContextMenuState
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.weContextMenu
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.menu.WeMenuListItem
import top.chengdongqing.wechat.core.designsystem.components.searchbar.WeSearchBar
import top.chengdongqing.wechat.core.designsystem.overscroll.rememberBouncedOverscrollEffect
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.model.FriendRequest
import top.chengdongqing.wechat.core.model.FriendRequestStatus
import top.chengdongqing.wechat.feature.contacts.R
import kotlin.time.Duration.Companion.days
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
fun NewFriendsScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onVerify: (requestId: String) -> Unit,
    viewModel: NewFriendsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val overscrollEffect = rememberBouncedOverscrollEffect()
    val resources = LocalResources.current

    // 按时间分组
    val (recent, older) = remember(uiState.filteredRequests) {
        uiState.filteredRequests.partition { request ->
            System.currentTimeMillis() - request.timestamp < 3.days.inWholeMilliseconds
        }
    }

    Scaffold(
        topBar = {
            NewFriendsTopBar(
                onBack = onBack,
                onAdd = onAdd,
                pendingCount = uiState.pendingCount
            )
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .overscroll(overscrollEffect),
            overscrollEffect = overscrollEffect
        ) {
            // 搜索栏
            stickyHeader {
                // 搜索框
                Box(
                    modifier = Modifier
                        .background(WeTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    WeSearchBar(
                        value = uiState.searchQuery,
                        placeholder = stringResource(R.string.new_contacts_search_placeholder),
                        backgroundColor = WeTheme.colorScheme.surface,
                        onChange = { viewModel.updateQuery(it) }
                    )
                }
            }

            // 功能菜单
            if (uiState.searchQuery.isEmpty()) {
                item {
                    WeMenuListItem(
                        label = stringResource(R.string.new_contacts_menu_add_phone),
                        icon = DesignR.drawable.ic_call_filled,
                        iconColor = WeTheme.colorScheme.primary
                    )
                }
            }

            // 近三天
            if (recent.isNotEmpty()) {
                renderRequestSection(
                    title = resources.getString(R.string.new_contacts_section_recent),
                    list = recent,
                    viewModel = viewModel,
                    onVerify = { onVerify(it) }
                )
            }
            // 三天前
            if (older.isNotEmpty()) {
                renderRequestSection(
                    title = resources.getString(R.string.new_contacts_section_older),
                    list = older,
                    viewModel = viewModel,
                    onVerify = { onVerify(it) }
                )
            }
        }
    }
}

@Composable
private fun NewFriendsTopBar(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    pendingCount: Int
) {
    WeTopAppBar(
        title = if (pendingCount > 0) {
            stringResource(R.string.new_contacts_title_with_count, pendingCount)
        } else {
            stringResource(R.string.new_contacts_title)
        },
        onBack = onBack,
        actions = {
            TextButton(
                text = stringResource(R.string.new_contacts_action_add),
                onClick = onAdd
            )
        }
    )
}

private fun LazyListScope.renderRequestSection(
    title: String,
    list: List<FriendRequest>,
    viewModel: NewFriendsViewModel,
    onVerify: (String) -> Unit
) {
    if (list.isNotEmpty()) {
        item { SectionTitle(title) }
        itemsIndexed(
            items = list,
            key = { _, request -> request.id }
        ) { index, request ->
            FriendRequestItem(
                request = request,
                viewModel = viewModel,
                showDivider = index < list.lastIndex,
                onVerify = { onVerify(request.id) }
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = WeTheme.colorScheme.textSecondary,
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun FriendRequestItem(
    request: FriendRequest,
    viewModel: NewFriendsViewModel,
    showDivider: Boolean = true,
    onVerify: () -> Unit
) {
    val contextMenuState = rememberContextMenuState()
    val isOutgoing = request.isFromMe
    val resources = LocalResources.current

    Column(
        modifier = Modifier
            .background(WeTheme.colorScheme.surface)
            .weContextMenu { position ->
                contextMenuState.show(
                    position = position,
                    options = listOf(resources.getString(DesignR.string.action_delete)),
                    listIndex = 0
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 头像组件
            RequestAvatar(request.avatarPath)
            // 信息主体 (昵称 & 留言)
            RequestContent(
                nickname = request.nickname,
                message = request.greeting.takeIf { it.isNotBlank() }?.let {
                    (if (isOutgoing) {
                        stringResource(R.string.new_contacts_outgoing_prefix)
                    } else {
                        ""
                    }) + request.greeting
                },
                modifier = Modifier.weight(1f)
            )
            // 状态处理器 (按钮或文字)
            RequestStatusHandler(
                request = request,
                onVerify = onVerify
            )
        }

        if (showDivider) {
            WeDivider(modifier = Modifier.padding(start = 76.dp))
        }
    }

    WeContextMenu(contextMenuState) { _, _ ->
        viewModel.delete(request.id)
    }
}

@Composable
private fun RequestAvatar(url: String?) {
    AsyncImage(
        model = url ?: DesignR.drawable.img_avatar_placeholder,
        contentDescription = stringResource(R.string.new_contacts_avatar_desc),
        error = painterResource(DesignR.drawable.img_avatar_placeholder),
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(6.dp))
    )
}

@Composable
private fun RequestContent(
    nickname: String,
    message: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = nickname,
            color = WeTheme.colorScheme.textPrimary,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        message?.let {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = it,
                color = WeTheme.colorScheme.textSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RequestStatusHandler(
    request: FriendRequest,
    onVerify: () -> Unit
) {
    when (request.status) {
        FriendRequestStatus.Pending -> {
            if (!request.isFromMe) {
                WeButton(
                    text = stringResource(DesignR.string.action_view),
                    type = ButtonType.Plain,
                    size = ButtonSize.Small,
                    onClick = onVerify
                )
            } else {
                StatusText(stringResource(R.string.new_contacts_status_pending))
            }
        }

        FriendRequestStatus.Accepted -> StatusText(stringResource(R.string.new_contacts_status_accepted))
        FriendRequestStatus.Rejected -> StatusText(stringResource(R.string.new_contacts_status_rejected))
        FriendRequestStatus.Expired -> StatusText(stringResource(R.string.new_contacts_status_expired))
    }
}

@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = WeTheme.colorScheme.textSecondary
    )
}
