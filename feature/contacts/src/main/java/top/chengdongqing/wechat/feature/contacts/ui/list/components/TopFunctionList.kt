package top.chengdongqing.wechat.feature.contacts.ui.list.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.badge.WeBadge
import top.chengdongqing.wechat.core.designsystem.components.badge.toBadgeText
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun TopFunctionList(
    pendingCount: Int,
    onNavigateToNewFriends: () -> Unit,
    onNavigateToGroups: () -> Unit,
    onNavigateToTags: () -> Unit
) {
    val functions = remember(pendingCount) {
        listOf(
            TopFunction(
                title = R.string.contacts_menu_new_friends,
                icon = R.drawable.ic_add_friends_filled,
                badge = pendingCount,
                containerColor = Color(0xFFFA9D3B),
                onClick = onNavigateToNewFriends
            ),
            TopFunction(
                title = R.string.contacts_menu_group_chat,
                icon = R.drawable.ic_group_chat_filled,
                containerColor = Color(0xFF07C160),
                onClick = onNavigateToGroups
            ),
            TopFunction(
                title = R.string.contacts_menu_tags,
                icon = R.drawable.ic_tag_filled,
                containerColor = Color(0xFF2782D7),
                onClick = onNavigateToTags
            ),
            TopFunction(
                title = R.string.contacts_menu_official_accounts,
                icon = R.drawable.ic_officical_account_filled,
                containerColor = Color(0xFF2782D7),
                onClick = {}
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeTheme.colorScheme.surface)
    ) {
        functions.forEachIndexed { index, function ->
            TopFunctionItem(function)
            if (index < functions.lastIndex) {
                WeDivider(modifier = Modifier.padding(start = 68.dp))
            }
        }
    }
}

@Composable
private fun TopFunctionItem(function: TopFunction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = function.onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(function.containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = function.icon),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        WeBadge(
            visible = function.badge > 0,
            content = function.badge.toBadgeText(),
            alignment = Alignment.CenterEnd,
            size = 20.dp,
            offset = DpOffset(0)
        ) {
            Text(
                text = stringResource(function.title),
                color = WeTheme.colorScheme.textPrimary,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class TopFunction(
    @get:StringRes val title: Int,
    @get:DrawableRes val icon: Int,
    val badge: Int = 0,
    val containerColor: Color,
    val onClick: () -> Unit
)
