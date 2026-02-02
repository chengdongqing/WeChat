package top.chengdongqing.wechat.ui.chat.session.message.content

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.fallback
import com.amap.api.maps.model.LatLng
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.data.model.LocationPreviewItem
import top.chengdongqing.wechat.data.model.MessageContent
import top.chengdongqing.wechat.ui.components.divider.WeDivider
import top.chengdongqing.wechat.ui.components.location.preview.previewLocation
import top.chengdongqing.wechat.ui.theme.WeTheme
import top.chengdongqing.wechat.ui.util.rememberScreenFractionWidth

@Composable
fun LocationContent(content: MessageContent.Location) {
    val context = LocalContext.current
    val targetWidth = rememberScreenFractionWidth(0.6f)

    Column(
        modifier = Modifier
            .width(targetWidth)
            .clickable {
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
                color = WeTheme.colorScheme.textPrimary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = content.address,
                color = WeTheme.colorScheme.textSecondary,
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
                .data(content.snapshotUri)
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