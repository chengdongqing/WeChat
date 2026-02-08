package top.chengdongqing.wechat.features.contacts.ui.newfirends.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.LinkColor
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.core.designsystem.util.weClickable

enum class FriendActionType(val title: String) {
    Apply("申请添加朋友"),
    Verify("通过朋友验证")
}

@Composable
internal fun FriendHandleBase(
    type: FriendActionType,
    contactId: String,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    var greetingText by remember { mutableStateOf(if (type == FriendActionType.Apply) "我是..." else "") }
    var remarkText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            WeTopBar(
                title = type.title,
                containerColor = WeTheme.colorScheme.surface,
                onBack = onBack
            )
        },
        containerColor = WeTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 26.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (type == FriendActionType.Apply) {
                        ListItem("打招呼内容") {
                            BasicTextField(
                                value = greetingText,
                                onValueChange = { greetingText = it },
                                maxLines = 3,
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    color = WeTheme.colorScheme.textPrimary
                                ),
                                cursorBrush = SolidColor(WeTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    ListItem("备注") {
                        BasicTextField(
                            value = remarkText,
                            onValueChange = { remarkText = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = WeTheme.colorScheme.textPrimary
                            ),
                            cursorBrush = SolidColor(WeTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    ListItem("标签") { LinkedRow("添加标签") {} }
                    ListItem("备忘") { LinkedRow("添加备忘") {} }
                    ListItem("朋友权限") { LinkedRow("设置朋友权限") {} }
                    Spacer(modifier = Modifier.height(40.dp))
                }

                // 底部遮罩
                BottomMask()
            }

            // 确认按钮
            ConfirmButton(
                text = if (type == FriendActionType.Apply) "发送" else "完成",
                onClick = onComplete
            )
        }
    }
}

@Composable
private fun BoxScope.BottomMask() {
    Spacer(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(12.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.8f)
                    )
                )
            )
    )
}

@Composable
private fun ListItem(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = label,
            color = WeTheme.colorScheme.textSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 12.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(WeTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun LinkedRow(label: String, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .weClickable { onClick() }
    ) {
        Text(text = label, color = LinkColor, fontSize = 16.sp)
        Icon(
            painter = painterResource(R.drawable.ic_right_outlined),
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ConfirmButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
            .navigationBarsPadding()
            .padding(bottom = 32.dp, top = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        WeButton(text = text, onClick = onClick)
    }
}