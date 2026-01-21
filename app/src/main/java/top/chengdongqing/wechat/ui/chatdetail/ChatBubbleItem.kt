package top.chengdongqing.wechat.ui.chatdetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.R

@Composable
fun ChatBubbleItem(
    isFromMe: Boolean,
    text: String,
    avatarRes: Int = R.drawable.img_avatar
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        if (!isFromMe) {
            Avatar(avatarRes)
            Spacer(modifier = Modifier.width(8.dp))
        }

        // 气泡容器
        Column(horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start) {
            Surface(
                color = if (isFromMe) Color(0xFF95EC69) else Color.White,
                shape = ChatBubbleShape(isFromMe = isFromMe),
                modifier = Modifier.widthIn(max = 260.dp) // 限制最大宽度
            ) {
                Text(
                    text = text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = TextStyle(fontSize = 16.sp, color = Color.Black)
                )
            }
        }

        if (isFromMe) {
            Spacer(modifier = Modifier.width(8.dp))
            Avatar(avatarRes)
        }
    }
}

@Composable
private fun Avatar(resId: Int) {
    Image(
        painter = painterResource(resId),
        contentDescription = null,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(4.dp))
    )
}

class ChatBubbleShape(
    private val isFromMe: Boolean,
    private val cornerRadius: Dp = 8.dp,
    private val arrowWidth: Dp = 6.dp,
    private val arrowHeight: Dp = 10.dp
) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val radius = with(density) { cornerRadius.toPx() }
        val aW = with(density) { arrowWidth.toPx() }
        val aH = with(density) { arrowHeight.toPx() }

        if (isFromMe) {
            // 右侧气泡：小三角在右边
            path.addRoundRect(RoundRect(0f, 0f, size.width - aW, size.height, CornerRadius(radius)))
            path.moveTo(size.width - aW, 20f) // 三角起始高度
            path.lineTo(size.width, 25f)
            path.lineTo(size.width - aW, 30f)
        } else {
            // 左侧气泡：小三角在左边
            path.addRoundRect(RoundRect(aW, 0f, size.width, size.height, CornerRadius(radius)))
            path.moveTo(aW, 20f)
            path.lineTo(0f, 25f)
            path.lineTo(aW, 30f)
        }
        path.close()
        return Outline.Generic(path)
    }
}