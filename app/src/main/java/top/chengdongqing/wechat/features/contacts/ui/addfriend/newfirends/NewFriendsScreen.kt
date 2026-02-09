package top.chengdongqing.wechat.features.contacts.ui.addfriend.newfirends

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.menulistitem.MenuListItem
import top.chengdongqing.wechat.core.designsystem.components.searchbar.WeSearchBar
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.data.database.entity.RequestStatus
import top.chengdongqing.wechat.features.contacts.domain.model.FriendRequest
import kotlin.time.Duration.Companion.days

@Composable
fun NewFriendsScreen(
    onBack: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToVerify: (requestId: String) -> Unit,
    viewModel: NewFriendsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
        ) {
            // 搜索栏
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    WeSearchBar(
                        value = "",
                        placeholder = "搜索 账号/名字",
                        backgroundColor = WeTheme.colorScheme.surface,
                        onChange = {}
                    )
                }
            }

            // 功能菜单
            item {
                MenuListItem(
                    label = "添加手机联系人",
                    iconResId = R.drawable.ic_voice_call_filled,
                    iconColor = WeTheme.colorScheme.primary,
                    onClick = { }
                )
            }

            // 按时间分组
            val (recent, older) = uiState.requests.partition { request ->
                System.currentTimeMillis() - request.timestamp < 3.days.inWholeMilliseconds
            }

            // 近三天
            if (recent.isNotEmpty()) {
                item { SectionTitle("近三天") }
                itemsIndexed(
                    items = recent,
                    key = { _, request -> request.requestId }
                ) { index, request ->
                    FriendRequestItem(
                        request = request,
                        onClick = { onNavigateToVerify(request.requestId) },
                        showDivider = index < recent.size - 1
                    )
                }
            }

            // 三天前
            if (older.isNotEmpty()) {
                item { SectionTitle("三天前") }
                itemsIndexed(
                    items = older,
                    key = { _, request -> request.requestId }
                ) { index, request ->
                    FriendRequestItem(
                        request = request,
                        onClick = { onNavigateToVerify(request.requestId) },
                        showDivider = index < recent.size - 1
                    )
                }
            }

            // 空状态
            if (uiState.requests.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无好友申请",
                            color = WeTheme.colorScheme.textSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
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
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Column(modifier = Modifier.background(WeTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 头像
            AsyncImage(
                model = request.fromAvatarPath,
                contentDescription = null,
                placeholder = painterResource(R.drawable.img_avatar),
                error = painterResource(R.drawable.img_avatar),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                // 昵称
                Text(
                    text = request.fromNickname,
                    color = WeTheme.colorScheme.textPrimary,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 打招呼内容
                if (request.greetingMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = request.greetingMessage,
                        color = WeTheme.colorScheme.textSecondary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 状态按钮
            when (request.status) {
                RequestStatus.PENDING -> {
                    WeButton(
                        text = "查看",
                        type = ButtonType.Plain,
                        size = ButtonSize.Small,
                        onClick = onClick
                    )
                }

                RequestStatus.ACCEPTED -> {
                    Text(
                        text = "已添加",
                        color = WeTheme.colorScheme.textSecondary,
                        fontSize = 14.sp
                    )
                }

                RequestStatus.REJECTED -> {
                    Text(
                        text = "已拒绝",
                        color = WeTheme.colorScheme.textSecondary,
                        fontSize = 14.sp
                    )
                }

                RequestStatus.EXPIRED -> {
                    Text(
                        text = "已过期",
                        color = WeTheme.colorScheme.textSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (showDivider) {
            WeDivider(modifier = Modifier.padding(start = 76.dp))
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