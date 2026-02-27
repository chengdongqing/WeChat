package top.chengdongqing.wechat.features.contacts.ui.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.WeContextMenu
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.rememberContextMenuState
import top.chengdongqing.wechat.core.designsystem.components.contextmenu.weContextMenu
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.features.contacts.ui.list.ContactItem

@Composable
fun ContactListItem(
    contact: ContactItem,
    onNavigateToDetail: () -> Unit,
    onNavigateToProfileEdit: () -> Unit
) {
    val contextMenuState = rememberContextMenuState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(WeTheme.colorScheme.surface)
            .weContextMenu(
                onClick = onNavigateToDetail,
                onLongClick = { position ->
                    if (!contact.isSelf) {
                        contextMenuState.show(position, listOf("设置朋友资料"), 0)
                    }
                }
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = contact.avatarPath,
            contentDescription = null,
            error = painterResource(R.drawable.img_avatar_placeholder),
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = contact.name,
                color = WeTheme.colorScheme.textPrimary,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            contact.note?.let {
                Text(
                    text = it,
                    color = WeTheme.colorScheme.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    WeContextMenu(contextMenuState) { _, _ ->
        onNavigateToProfileEdit()
    }
}