package top.chengdongqing.wechat.core.designsystem.components.informationbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import top.chengdongqing.wechat.core.designsystem.theme.GreenPrimary
import top.chengdongqing.wechat.core.designsystem.theme.LinkColor
import top.chengdongqing.wechat.core.designsystem.util.weClickable

enum class InformationBarType(
    val backgroundColor: Color,
    val iconColor: Color = Color.White,
    val textColor: Color = Color.White,
    val linkColor: Color = Color.White,
    val closeIconColor: Color = Color.White
) {
    WarnStrong(backgroundColor = Color(0xFFFA5151)),
    Info(backgroundColor = Color(0f, 0f, 0f, 0.3f)),
    TipsStrong(backgroundColor = Color(0xFFFA9D3B)),
    TipsWeak(
        backgroundColor = Color.White,
        iconColor = Color(0f, 0f, 0f, 0.55f),
        textColor = Color(0f, 0f, 0f, 0.55f),
        linkColor = LinkColor,
        closeIconColor = Color(0f, 0f, 0f, 0.55f)
    ),
    Success(backgroundColor = GreenPrimary)
}

@Composable
fun WeInformationBar(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    message: String,
    type: InformationBarType = InformationBarType.Success,
    linkText: String? = null,
    autoClose: Boolean = false,
    onLink: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null
) {
    // 自动关闭
    LaunchedEffect(visible, autoClose, message) {
        if (visible && autoClose) {
            delay(5000)
            onClose?.invoke()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(type.backgroundColor)
                .padding(16.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (type == InformationBarType.Success) Icons.Outlined.Check else Icons.Outlined.Info,
                contentDescription = null,
                tint = type.iconColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = message, fontSize = 14.sp, color = type.textColor)
            Spacer(modifier = Modifier.weight(1f))
            linkText?.let {
                Text(
                    text = it,
                    fontSize = 14.sp,
                    color = type.linkColor,
                    modifier = Modifier.weClickable {
                        onLink?.invoke()
                    }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            onClose?.let {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = null,
                    tint = type.closeIconColor,
                    modifier = Modifier.weClickable {
                        it()
                    }
                )
            }
        }
    }
}