package top.chengdongqing.wechat.core.location.map.amap

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import top.chengdongqing.wechat.core.location.map.MapController

fun createMapController(
    context: Context,
    isDarkTheme: Boolean,
    scope: CoroutineScope
): MapController {
    return LiteAMapController(context, isDarkTheme, scope)
}