package top.chengdongqing.wechat.ui.components.location.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.MarkerOptions
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.util.createBitmapDescriptor
import top.chengdongqing.wechat.core.util.navigateToLocation
import top.chengdongqing.wechat.data.model.LocationPreviewItem
import top.chengdongqing.wechat.data.model.MapType
import top.chengdongqing.wechat.ui.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.ui.components.actionsheet.rememberActionSheetState
import top.chengdongqing.wechat.ui.components.location.AMap
import top.chengdongqing.wechat.ui.components.location.rememberAMapState
import top.chengdongqing.wechat.ui.theme.Black
import top.chengdongqing.wechat.ui.theme.WeChatTheme
import top.chengdongqing.wechat.ui.theme.White
import top.chengdongqing.wechat.ui.util.weClickable

@Composable
fun WeLocationPreview(location: LocationPreviewItem, onBack: () -> Unit) {
    val context = LocalContext.current
    val state = rememberAMapState()
    val map = state.map

    LaunchedEffect(state) {
        // 设置地图视野
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location.latLng, location.zoom))

        // 添加定位标记
        val marker = MarkerOptions().apply {
            position(location.latLng)
            icon(
                createBitmapDescriptor(
                    context,
                    R.drawable.ic_location_marker,
                    120,
                    120
                )
            )
        }
        map.addMarker(marker)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            AMap(modifier = Modifier.fillMaxSize(), state)
            TopBar(onBack)
        }
        BottomBar(location)
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Black.copy(alpha = 0.4f), Color.Transparent)
                )
            )
            .statusBarsPadding()
            .padding(bottom = 20.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_back_circle_filled),
            contentDescription = "返回",
            modifier = Modifier
                .offset(x = 14.dp, y = 16.dp)
                .size(26.dp)
                .weClickable { onBack() },
            tint = White
        )
    }
}

@Composable
private fun BottomBar(location: LocationPreviewItem) {
    val context = LocalContext.current
    val actionSheet = rememberActionSheetState()
    val mapOptions = remember {
        listOf(
            ActionSheetItem("高德地图"),
            ActionSheetItem("百度地图"),
            ActionSheetItem("腾讯地图"),
            ActionSheetItem("谷歌地图"),
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeChatTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = location.poiName,
                color = WeChatTheme.colorScheme.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))
            location.address?.let {
                Text(
                    text = it,
                    color = WeChatTheme.colorScheme.textSecondary,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(WeChatTheme.colorScheme.background)
                    .clickable {
                        actionSheet.show(mapOptions) { index ->
                            context.navigateToLocation(
                                MapType.ofIndex(index)!!,
                                location.latLng,
                                location.poiName
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Navigation,
                    contentDescription = null,
                    tint = WeChatTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "导航", color = WeChatTheme.colorScheme.textSecondary, fontSize = 14.sp)
        }
    }
}