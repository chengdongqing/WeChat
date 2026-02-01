package top.chengdongqing.wechat.ui.contacts.list.components

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
import top.chengdongqing.wechat.ui.components.divider.WeDivider
import top.chengdongqing.wechat.ui.theme.WeChatTheme

@Composable
fun TopFunctionList() {
    val functions = listOf(
        TopFunction("新的朋友", R.drawable.ic_add_friends_filled, Color(0xFFFA9D3B)),
        TopFunction("群聊", R.drawable.ic_group_chat_filled, Color(0xFF07C160)),
        TopFunction("标签", R.drawable.ic_tag_filled, Color(0xFF2782D7)),
        TopFunction("公众号", R.drawable.ic_officical_account_filled, Color(0xFF2782D7)),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeChatTheme.colorScheme.surface)
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
            .clickable { }
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
            color = WeChatTheme.colorScheme.textPrimary
        )
    }
}

private data class TopFunction(
    val title: String,
    @get:DrawableRes val iconResId: Int,
    val containerColor: Color
)