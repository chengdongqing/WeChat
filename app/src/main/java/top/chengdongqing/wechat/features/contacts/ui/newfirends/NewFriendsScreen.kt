package top.chengdongqing.wechat.features.contacts.ui.newfirends

import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonSize
import top.chengdongqing.wechat.core.designsystem.components.button.ButtonType
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.menulistitem.MenuListItem
import top.chengdongqing.wechat.core.designsystem.components.searchbar.WeSearchBar
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.weClickable

@Composable
fun NewFriendsScreen(
    onBack: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToVerify: () -> Unit
) {
    Scaffold(
        topBar = {
            NewFriendsTopBar(onBack, onNavigateToAdd)
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

            item { SectionTitle("近三天") }
            itemsIndexed(items = List(3) { it }) { index, _ ->
                FriendRequestItem(
                    name = "海盐芝士不加糖",
                    message = "我是James",
                    onClick = onNavigateToVerify,
                    showDivider = index < 2
                )
            }
            item { SectionTitle("三天前") }
            items(5) {
                FriendRequestItem(
                    name = "微信好友",
                    message = "来自搜索账号",
                    onClick = onNavigateToVerify,
                    showDivider = true
                )
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
    name: String,
    message: String,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Column(modifier = Modifier.background(WeTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weClickable(onClick = onClick) // 微信列表整行可点
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.img_avatar),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
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
            WeButton(
                text = "查看",
                type = ButtonType.Plain,
                size = ButtonSize.Small,
                onClick = onClick
            )
        }
        if (showDivider) {
            WeDivider(modifier = Modifier.padding(start = 76.dp)) // 微信分割线通常不切断头像
        }
    }
}

@Composable
private fun NewFriendsTopBar(
    onBack: () -> Unit,
    onNavigateToAdd: () -> Unit
) {
    WeTopBar(
        title = "新的朋友",
        onBack = onBack,
        actions = {
            Text(
                text = "添加朋友",
                color = WeTheme.colorScheme.textPrimary,
                fontSize = 16.sp,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .weClickable(onClick = onNavigateToAdd)
            )
        })
}