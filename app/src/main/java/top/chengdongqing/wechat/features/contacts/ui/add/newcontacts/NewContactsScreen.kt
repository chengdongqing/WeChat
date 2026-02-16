package top.chengdongqing.wechat.features.contacts.ui.add.newcontacts

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonType
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.WeContextMenu
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.rememberContextMenuState
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.weContextMenu
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.menulistitem.MenuListItem
import top.chengdongqing.wechat.core.designsystem.components.searchbar.WeSearchBar
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.rememberBounceOverscrollEffect
import top.chengdongqing.wechat.data.database.entity.RequestDirection
import top.chengdongqing.wechat.data.database.entity.RequestStatus
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequest
import kotlin.time.Duration.Companion.days

@Composable
fun NewContactsScreen(
    onBack: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToVerify: (requestId: String) -> Unit,
    viewModel: NewContactsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val overscrollEffect = rememberBounceOverscrollEffect()

    // 进入页面时标记所有为已读
    LaunchedEffect(Unit) {
        viewModel.markAllAsRead()
    }

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
                onNavigateToAdd = onNavigateToAdd,
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
                        placeholder = "搜索 账号/名字",
                        backgroundColor = WeTheme.colorScheme.surface,
                        onChange = { viewModel.onSearchQueryChange(it) }
                    )
                }
            }

            // 功能菜单
            if (uiState.searchQuery.isEmpty()) {
                item {
                    MenuListItem(
                        label = "添加手机联系人",
                        iconResId = R.drawable.ic_call_filled,
                        iconColor = WeTheme.colorScheme.primary
                    )
                }
            }

            // 近三天
            if (recent.isNotEmpty()) {
                renderRequestSection(
                    title = "近三天",
                    list = recent,
                    viewModel = viewModel,
                    onItemClick = { onNavigateToVerify(it) }
                )
            }
            // 三天前
            if (older.isNotEmpty()) {
                renderRequestSection(
                    title = "三天前",
                    list = older,
                    viewModel = viewModel,
                    onItemClick = { onNavigateToVerify(it) }
                )
            }
        }
    }
}

@Composable
private fun NewFriendsTopBar(
    onBack: () -> Unit,
    onNavigateToAdd: () -> Unit,
    pendingCount: Int
) {
    WeTopBar(
        title = if (pendingCount > 0) "新的朋友($pendingCount)" else "新的朋友",
        onBack = onBack,
        actions = {
            ActionText("添加朋友", onClick = onNavigateToAdd)
        }
    )
}

private fun LazyListScope.renderRequestSection(
    title: String,
    list: List<FriendRequest>,
    viewModel: NewContactsViewModel,
    onItemClick: (String) -> Unit,
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
                showDivider = index < list.size - 1,
                onClick = { onItemClick(request.id) }
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
    viewModel: NewContactsViewModel,
    showDivider: Boolean = true,
    onClick: () -> Unit,
) {
    val contextMenuState = rememberContextMenuState()
    val isOutgoing = request.direction.isOutgoing

    Column(
        modifier = Modifier
            .background(WeTheme.colorScheme.surface)
            .weContextMenu { position ->
                contextMenuState.show(position, listOf("删除"), 0)
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
            RequestAvatar(request.peerAvatarPath)
            // 信息主体 (昵称 & 留言)
            RequestContent(
                nickname = request.peerNickname,
                message = (if (isOutgoing) "我：" else "") + request.greetingMessage,
                modifier = Modifier.weight(1f)
            )
            // 状态处理器 (按钮或文字)
            RequestStatusHandler(
                request = request,
                onActionClick = onClick
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
        model = url ?: R.drawable.img_avatar_placeholder,
        contentDescription = "用户头像",
        error = painterResource(R.drawable.img_avatar_placeholder),
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(6.dp))
    )
}

@Composable
private fun RequestContent(
    nickname: String,
    message: String,
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
        if (message.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = message,
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
    onActionClick: () -> Unit
) {
    when (request.status) {
        RequestStatus.Pending -> {
            if (request.direction == RequestDirection.Incoming) {
                WeButton(
                    text = "查看",
                    type = ButtonType.Plain,
                    size = ButtonSize.Small,
                    onClick = onActionClick
                )
            } else {
                StatusText("等待验证")
            }
        }

        RequestStatus.Accepted -> StatusText("已添加")
        RequestStatus.Rejected -> StatusText("已拒绝")
        RequestStatus.Expired -> StatusText("已过期")
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