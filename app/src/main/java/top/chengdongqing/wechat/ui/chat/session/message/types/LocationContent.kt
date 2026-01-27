package top.chengdongqing.wechat.ui.chat.session.message.types

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.fallback
import com.amap.api.maps.model.LatLng
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.utils.randomUUID
import top.chengdongqing.wechat.data.model.ChatMessage
import top.chengdongqing.wechat.data.model.LocationPreviewItem
import top.chengdongqing.wechat.data.model.MessageContent
import top.chengdongqing.wechat.ui.chat.session.message.MessageItem
import top.chengdongqing.wechat.ui.components.WeDivider
import top.chengdongqing.wechat.ui.components.location.preview.previewLocation
import top.chengdongqing.wechat.ui.theme.WeChatTheme
import top.chengdongqing.wechat.ui.utils.rememberWindowFractionWidth
import top.chengdongqing.wechat.ui.utils.weClickable

@Composable
fun LocationContent(content: MessageContent.Location) {
    val context = LocalContext.current
    val targetWidth = rememberWindowFractionWidth(0.6f)

    Column(
        modifier = Modifier
            .width(targetWidth)
            .weClickable {
                val location = LocationPreviewItem(
                    latLng = LatLng(
                        content.latitude,
                        content.longitude
                    ),
                    address = content.address,
                    poiName = content.poiName
                )
                context.previewLocation(location)
            }
    ) {
        // 位置基础信息
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = content.poiName,
                color = WeChatTheme.colorScheme.textPrimary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = content.address,
                color = WeChatTheme.colorScheme.textSecondary,
                fontSize = 12.sp,
                lineHeight = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        WeDivider()

        // 位置快照图片
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(content.snapshotUrl)
                .fallback(R.drawable.img_location_placeholder)
                .build(),
            contentDescription = "Location",
            modifier = Modifier
                .fillMaxWidth()
                .height(95.dp),
            contentScale = ContentScale.Crop
        )
    }
}

@Preview
@Composable
private fun Prev() {
    val content = MessageContent.Location(
        latitude = 2323.1212,
        longitude = 23454.45,
        address = "汉拿山了看大家发的是的；快乐十分的首付款角度看",
        poiName = "上海市及那段时间发的老师反馈了；但是开发了看电视",
        snapshotUrl = ""
    )
    val message = ChatMessage(
        id = randomUUID(),
        content = content,
        isFromMe = false,
        timestamp = System.currentTimeMillis()
    )
    MessageItem(message)
}