package top.chengdongqing.wechat.core.location.map

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import top.chengdongqing.wechat.core.location.map.amap.AMapController

fun createMapController(
    context: Context,
    isDarkTheme: Boolean,
    scope: CoroutineScope
): MapController {
    return AMapController(context, isDarkTheme, scope)
}