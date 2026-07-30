package top.chengdongqing.wechat.feature.contacts.ui.friendrequest.components

import androidx.annotation.StringRes
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.appbar.topbar.WeTopAppBar
import top.chengdongqing.wechat.core.designsystem.components.button.WeButton
import top.chengdongqing.wechat.core.designsystem.components.loading.LoadingDialog
import top.chengdongqing.wechat.core.designsystem.modifier.onTap
import top.chengdongqing.wechat.core.designsystem.theme.LinkBlue
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme

enum class FriendActionType(@get:StringRes val titleRes: Int) {
    Apply(R.string.contact_action_apply),
    Verify(R.string.contact_action_verify);

    val isApply: Boolean get() = this == Apply
}

@Composable
fun ContactHandleBase(
    type: FriendActionType,
    greeting: String = "",
    onGreetingChange: (String) -> Unit = {},
    remark: String = "",
    onRemarkChange: (String) -> Unit = {},
    note: String,
    onNoteChange: (String) -> Unit = {},
    isLoading: Boolean = false,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    Scaffold(
        topBar = {
            WeTopAppBar(
                title = stringResource(type.titleRes),
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
                    if (type.isApply) {
                        FieldItem(
                            label = stringResource(R.string.contact_label_greeting),
                            value = greeting,
                            onValueChange = onGreetingChange,
                            singleLine = false
                        )
                    }
                    FieldItem(
                        label = stringResource(R.string.contact_label_remark),
                        value = remark,
                        onValueChange = onRemarkChange
                    )
                    ListItem(stringResource(R.string.contact_label_tag)) {
                        LinkedRow(stringResource(R.string.contact_action_add_tag)) {}
                    }
                    FieldItem(
                        label = stringResource(R.string.contact_label_note),
                        value = note,
                        onValueChange = onNoteChange
                    )
                    ListItem(stringResource(R.string.contact_label_permission)) {
                        LinkedRow(stringResource(R.string.contact_action_set_permission)) {}
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }

                // 底部遮罩
                BottomMask()
            }

            // 确认按钮
            ConfirmButton(
                text = if (type.isApply) {
                    stringResource(R.string.action_send)
                } else {
                    stringResource(R.string.action_done)
                },
                onClick = onComplete
            )
        }
    }

    LoadingDialog(isLoading)
}

@Composable
private fun FieldItem(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true
) {
    ListItem(label) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = WeTheme.colorScheme.textPrimary
            ),
            cursorBrush = SolidColor(WeTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        )
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
                        WeTheme.colorScheme.surface
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
            .onTap { onClick() }
    ) {
        Text(text = label, color = LinkBlue, fontSize = 16.sp)
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
            .background(WeTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(bottom = 32.dp, top = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        WeButton(text = text, onClick = onClick)
    }
}