package top.chengdongqing.wechat.core.location

import android.Manifest
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import top.chengdongqing.wechat.core.designsystem.theme.LocalIsDarkTheme
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.showToast
import top.chengdongqing.wechat.core.location.map.MapController
import top.chengdongqing.wechat.core.location.map.amap.AMapController
import top.chengdongqing.wechat.core.location.model.GeoPoint

/**
 * 创建并记住地图控制器
 *
 * 此处是整个模块中唯一知道具体 SDK 实现类的地方。
 * 替换地图 SDK 时只需修改此处的返回值即可。
 */
@Composable
fun rememberMapController(): MapController {
    val context = LocalContext.current
    val isDarkTheme = LocalIsDarkTheme.current
    val scope = rememberCoroutineScope()
    return remember { AMapController(context, isDarkTheme, scope) }
}

/**
 * 地图渲染容器
 *
 * 通过 [MapController] 接口与具体 SDK 完全解耦，不引入任何 AMap 类型。
 *
 * @param controller 由 [rememberMapController] 创建，持有具体 SDK 实例
 * @param overlay 地图上叠加的控件，默认显示定位回中按钮
 */
@Composable
fun WeMap(
    modifier: Modifier = Modifier,
    controller: MapController = rememberMapController(),
    overlay: @Composable BoxScope.(MapController) -> Unit = { LocationControl(it) }
) {
    val context = LocalContext.current
    val mapSaveState = rememberSaveable { Bundle() }

    LifecycleEffect(controller, mapSaveState)
    PermissionHandler {
        if (mapSaveState.isEmpty) controller.enableMyLocation(context)
    }

    Box(modifier) {
        AndroidView(
            factory = { controller.view },
            modifier = Modifier.fillMaxSize()
        )
        overlay(controller)
    }
}

/**
 * 定位回中控件，点击后将地图视野移至当前设备位置
 */
@Composable
fun BoxScope.LocationControl(controller: MapController, onClick: ((GeoPoint) -> Unit)? = null) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .offset(x = 12.dp, y = (-36).dp)
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(WeTheme.colorScheme.surface)
            .clickable {
                val location = controller.currentLocation
                if (location != null) {
                    controller.moveTo(location)
                    onClick?.invoke(location)
                } else {
                    context.showToast("定位中...")
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.MyLocation,
            contentDescription = stringResource(R.string.location_current),
            tint = WeTheme.colorScheme.textPrimary,
            modifier = Modifier.size(26.dp)
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionHandler(onGranted: () -> Unit) {
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION) {
        if (it) onGranted()
    }

    LaunchedEffect(Unit) {
        if (permissionState.status.isGranted) {
            onGranted()
        } else {
            permissionState.launchPermissionRequest()
        }
    }
}

@Composable
private fun LifecycleEffect(controller: MapController, mapSaveState: Bundle) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> controller.onCreate(mapSaveState)
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause(mapSaveState)
                else -> {}
            }
        }
        val callbacks = object : ComponentCallbacks2 {
            override fun onConfigurationChanged(config: Configuration) {}

            // 系统极度缺内存，无条件清理
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onLowMemory() = controller.onLowMemory()

            // 可精细控制，分等级
            override fun onTrimMemory(level: Int) = controller.onTrimMemory(level)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        context.registerComponentCallbacks(callbacks)

        onDispose {
            controller.onPause(mapSaveState)
            controller.onDestroy()
            lifecycleOwner.lifecycle.removeObserver(observer)
            context.unregisterComponentCallbacks(callbacks)
        }
    }
}
