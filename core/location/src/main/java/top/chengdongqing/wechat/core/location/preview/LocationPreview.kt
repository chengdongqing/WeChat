package top.chengdongqing.wechat.core.location.preview

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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetItem
import top.chengdongqing.wechat.core.designsystem.components.actionsheet.ActionSheetManager
import top.chengdongqing.wechat.core.designsystem.modifier.onTap
import top.chengdongqing.wechat.core.designsystem.theme.Black
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.theme.White
import top.chengdongqing.wechat.core.location.WeMap
import top.chengdongqing.wechat.core.location.model.LocationPreviewInfo
import top.chengdongqing.wechat.core.location.model.MapType
import top.chengdongqing.wechat.core.location.rememberMapController
import top.chengdongqing.wechat.core.location.util.createIconBitmap
import top.chengdongqing.wechat.core.location.util.navigateToLocation
import top.chengdongqing.wechat.core.util.showToast
import top.chengdongqing.wechat.core.designsystem.R as DesignR

@Composable
fun WeLocationPreview(location: LocationPreviewInfo, onBack: () -> Unit) {
    val context = LocalContext.current
    val mapController = rememberMapController()

    LaunchedEffect(mapController) {
        mapController.moveTo(location.coordinate, location.zoomLevel)
        val icon = createIconBitmap(context, DesignR.drawable.ic_location_marker, 120, 120)
        mapController.addMarker(location.coordinate, icon)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            WeMap(
                modifier = Modifier.fillMaxSize(),
                controller = mapController
            )
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
            painter = painterResource(DesignR.drawable.ic_back_circle_filled),
            contentDescription = stringResource(DesignR.string.action_back),
            modifier = Modifier
                .offset(x = 14.dp, y = 16.dp)
                .size(26.dp)
                .onTap { onBack() },
            tint = White
        )
    }
}

@Composable
private fun BottomBar(location: LocationPreviewInfo) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val installedTypes = remember(context) {
        MapType.entries.filter { mapType ->
            runCatching {
                context.packageManager.getPackageInfo(mapType.packageName, 0)
            }.isSuccess
        }
    }
    val mapOptions = remember(installedTypes) {
        installedTypes.map { ActionSheetItem(it.labelRes) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WeTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = location.name,
                color = WeTheme.colorScheme.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))
            location.address?.let {
                Text(
                    text = it,
                    color = WeTheme.colorScheme.textSecondary,
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
                    .background(WeTheme.colorScheme.background)
                    .clickable {
                        if (mapOptions.isEmpty()) {
                            context.showToast(resources.getString(DesignR.string.map_no_navigation_app))
                        } else {
                            ActionSheetManager.show(mapOptions) { index ->
                                installedTypes.getOrNull(index)?.let { mapType ->
                                    context.navigateToLocation(
                                        mapType,
                                        location.coordinate,
                                        location.name
                                    )
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Navigation,
                    contentDescription = null,
                    tint = WeTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(DesignR.string.location_navigate),
                color = WeTheme.colorScheme.textSecondary,
                fontSize = 14.sp
            )
        }
    }
}
