package top.chengdongqing.wechat.features.contacts.ui.list.components

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

@Composable
fun TopFunctionList(onNavigateToNewFriends: () -> Unit) {
    val functions = listOf(
        TopFunction(
            title = "新的朋友",
            iconResId = R.drawable.ic_add_friends_filled,
            containerColor = Color(0xFFFA9D3B),
            onClick = onNavigateToNewFriends
        ),
        TopFunction(
            title = "群聊",
            iconResId = R.drawable.ic_group_chat_filled,
            containerColor = Color(0xFF07C160),
            onClick = {}
        ),
        TopFunction(
            title = "标签",
            iconResId = R.drawable.ic_tag_filled,
            containerColor = Color(0xFF2782D7),
            onClick = {}
        ),
        TopFunction(
            title = "公众号",
            iconResId = R.drawable.ic_officical_account_filled,
            containerColor = Color(0xFF2782D7),
            onClick = {}
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeTheme.colorScheme.surface)
    ) {
        functions.forEachIndexed { index, function ->
            TopFunctionItem(function)
            if (index < functions.size - 1) {
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
                painter = painterResource(id = function.iconResId),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = function.title,
            fontSize = 16.sp,
            color = WeTheme.colorScheme.textPrimary
        )
    }
}

private data class TopFunction(
    val title: String,
    @get:DrawableRes val iconResId: Int,
    val containerColor: Color,
    val onClick: () -> Unit
)