package top.chengdongqing.wechat.ui.chatdetail

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.utils.UpdatedEffect
import top.chengdongqing.wechat.core.utils.weClickable
import top.chengdongqing.wechat.ui.components.button.ButtonSize
import top.chengdongqing.wechat.ui.components.button.WeButton
import top.chengdongqing.wechat.ui.theme.GreenPrimary
import top.chengdongqing.wechat.ui.theme.WeChatTheme

@Composable
fun ChatBottomBar(
    text: String,
    onTextChange: (String) -> Unit,
    isVoiceMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    onSend: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    // 切换到文本模式后自动弹出键盘
    LaunchedEffectFocusRequest(focusRequester, !isVoiceMode)

    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .background(Color(0xFFF7F7F7))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // 语音/文字切换按钮
            ActionIcon(
                iconResId = if (isVoiceMode) R.drawable.ic_keyboard_outline else R.drawable.ic_voice_outline,
                description = "切换模式"
            ) {
                onModeChange(!isVoiceMode)
            }

            // 输入框区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .defaultMinSize(minHeight = 40.dp)
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (isVoiceMode) {
                    Text(
                        "按住 说话",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                } else {
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(fontSize = 16.sp),
                        cursorBrush = SolidColor(GreenPrimary)
                    )
                }
            }

            // 表情按钮
            ActionIcon(iconResId = R.drawable.ic_sticker_outline, description = "显示表情")
            // 动态切换 加号 或 发送按钮
            AnimatedContent(targetState = text.isNotEmpty(), label = "SendButton") { isNotEmpty ->
                if (isNotEmpty) {
                    Box(modifier = Modifier.height(40.dp), contentAlignment = Alignment.Center) {
                        WeButton("发送", size = ButtonSize.SMALL, onClick = onSend)
                    }
                } else {
                    ActionIcon(
                        iconResId = R.drawable.ic_plus_circle_outline,
                        description = "更多操作"
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionIcon(
    modifier: Modifier = Modifier,
    @DrawableRes iconResId: Int,
    description: String? = null,
    tint: Color = WeChatTheme.colorScheme.textPrimary,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .weClickable(onClick = { onClick?.invoke() }),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = description,
            modifier = Modifier.size(30.dp),
            tint = tint
        )
    }
}

@Composable
private fun LaunchedEffectFocusRequest(
    focusRequester: FocusRequester,
    trigger: Boolean
) {
    UpdatedEffect(trigger) {
        if (trigger) {
            focusRequester.requestFocus()
        }
    }
}