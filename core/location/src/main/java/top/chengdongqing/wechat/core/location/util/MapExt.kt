package top.chengdongqing.wechat.core.location.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.designsystem.util.showToast
import top.chengdongqing.wechat.core.location.model.GeoPoint
import top.chengdongqing.wechat.core.location.model.MapType

/**
 * 调用外部地图应用进行导航
 *
 * @param mapType 指定打开的地图类型
 * @param location 目的地坐标
 * @param name 目的地名称描述
 */
fun Context.navigateToLocation(mapType: MapType, location: GeoPoint, name: String) {
    runCatching {
        val uri = mapType.buildUri(location.latitude, location.longitude, name)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }.onFailure {
        showToast("未安装${getString(mapType.labelRes)}地图")
    }
}

/**
 * 将指定图片资源渲染为 Bitmap
 *
 * @param iconId Drawable 资源 ID
 * @param width 目标宽度，null 时使用原始宽度
 * @param height 目标高度，null 时使用原始高度
 * @param rotationAngle 旋转角度，null 时不旋转
 */
suspend fun createIconBitmap(
    context: Context,
    @DrawableRes iconId: Int,
    width: Int? = null,
    height: Int? = null,
    rotationAngle: Float? = null
): Bitmap? = withContext(Dispatchers.IO) {
    val drawable = ContextCompat.getDrawable(context, iconId) ?: return@withContext null
    val w = width ?: drawable.intrinsicWidth
    val h = height ?: drawable.intrinsicHeight
    val bitmap = createBitmap(w, h)
    val canvas = Canvas(bitmap)
    rotationAngle?.let { canvas.save(); canvas.rotate(it, w / 2f, h / 2f) }
    drawable.setBounds(0, 0, w, h)
    drawable.draw(canvas)
    rotationAngle?.let { canvas.restore() }
    bitmap
}

/**
 * 格式化距离
 *
 * @param meters 米数
 * @param decimals 公里时保留小数位数，默认 1 位
 */
fun formatDistance(meters: Int, decimals: Int = 1): String {
    return if (meters >= 1000) "%.${decimals}fkm".format(meters / 1000f) else "${meters}m"
}
