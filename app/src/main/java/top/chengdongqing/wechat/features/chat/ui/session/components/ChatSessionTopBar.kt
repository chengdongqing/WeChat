package top.chengdongqing.wechat.features.chat.ui.session.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.chat.ui.session.ChatSessionViewModel
import top.chengdongqing.wechat.features.chat.ui.session.message.ChatSessionUiState

@Composable
fun ChatSessionTopBar(
    viewModel: ChatSessionViewModel,
    uiState: ChatSessionUiState,
    onBack: () -> Unit,
    onNavigateToInfo: () -> Unit
) {
    val isSelectMode = uiState.isSelectMode
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle(0)

    WeTopBar(
        titleContent = {
            ChatSessionTitle(viewModel, uiState)
        },
        backText = if (isSelectMode) "取消" else null,
        onBack = {
            if (isSelectMode) {
                viewModel.exitSelectMode()
            } else {
                onBack()
            }
        },
        unreadCount = unreadCount
    ) {
        if (!isSelectMode) {
            ActionIcon(iconResId = R.drawable.ic_more_outlined, description = "更多") {
                onNavigateToInfo()
            }
        }
    }
}

@Composable
private fun ChatSessionTitle(
    viewModel: ChatSessionViewModel,
    uiState: ChatSessionUiState,
) {
    val isE2EActive by viewModel.isE2EActive.collectAsStateWithLifecycle()
    val statusColor = if (uiState.isOnline) {
        WeTheme.colorScheme.primary
    } else {
        WeTheme.colorScheme.divider
    }
    val statusDesc = if (uiState.isOnline) "在线" else "离线"
    val title = when {
        !uiState.isSelectMode -> uiState.title
        viewModel.selectedCount > 0 -> "已选择${viewModel.selectedCount}条消息"
        else -> "选择消息"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.7f),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 标题
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = WeTheme.colorScheme.textPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (!uiState.isSelf) {
                // 免打扰
                if (uiState.isMuted) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mute_outlined),
                        contentDescription = "免打扰",
                        modifier = Modifier.size(16.dp),
                        tint = WeTheme.colorScheme.textSecondary
                    )
                }
                // 通过听筒播放
                if (!uiState.isSpeakerOn) {
                    Icon(
                        painter = painterResource(R.drawable.ic_ear_outlined),
                        contentDescription = "通过听筒播放语音",
                        modifier = Modifier.size(16.dp),
                        tint = WeTheme.colorScheme.textSecondary
                    )
                }
                // 加密锁图标
                if (isE2EActive) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock_filled),
                        contentDescription = "已加密",
                        modifier = Modifier.size(16.dp),
                        tint = WeTheme.colorScheme.textSecondary
                    )
                }
                // 在线状态小圆点
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .semantics { contentDescription = statusDesc }
                        .background(statusColor, CircleShape)
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.4f),
                            CircleShape
                        )
                )
            }
        }
    }
}