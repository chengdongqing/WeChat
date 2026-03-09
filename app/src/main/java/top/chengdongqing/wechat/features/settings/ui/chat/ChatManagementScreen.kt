package top.chengdongqing.wechat.features.settings.ui.chat

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.dialog.rememberDialogState
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.Danger
import top.chengdongqing.wechat.core.designsystem.theme.LinkColor
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.weClickable

@Composable
fun ChatManagementScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            WeTopBar(title = "聊天记录管理", onBack = onBack)
        },
        containerColor = WeTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ChatHandleCard(
                    icon = R.drawable.ic_import_export,
                    title = "导入与导出",
                    description = "从其它设备导入聊天记录，或将聊天记录导出到其它设备。",
                    onClick = { }
                )
                ChatHandleCard(
                    icon = R.drawable.ic_backup_restore,
                    title = "备份与恢复",
                    description = "把手机里的聊天记录备份到其他存储设备，以便在丢失或使用新手机时恢复聊天记录。",
                    onClick = { }
                )
            }

            ClearChatButton()
        }
    }
}

@Composable
private fun BoxScope.ClearChatButton() {
    val dialog = rememberDialogState()

    Text(
        text = "清空全部聊天记录",
        color = LinkColor,
        fontSize = 13.sp,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 40.dp)
            .weClickable {
                dialog.show(
                    title = "确定清空全部聊天记录吗？",
                    content = "被清空的聊天记录将无法恢复，请再次确认是否清空所有个人和群的聊天记录",
                    okText = "清空",
                    okColor = Danger,
                    onOk = {}
                )
            }
    )
}

@Composable
private fun ChatHandleCard(
    @DrawableRes icon: Int,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = WeTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = WeTheme.colorScheme.textSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    color = WeTheme.colorScheme.textPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = WeTheme.colorScheme.textSecondary,
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_right_outlined),
                contentDescription = null,
                tint = WeTheme.colorScheme.textSecondary,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(24.dp)
                    .offset(x = 8.dp)
            )
        }
    }
}