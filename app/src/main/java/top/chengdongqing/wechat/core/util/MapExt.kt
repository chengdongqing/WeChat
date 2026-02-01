package top.chengdongqing.wechat.core.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.location.Location
import android.net.Uri
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.amap.api.maps.model.BitmapDescriptor
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.services.core.LatLonPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.data.model.MapType
import java.net.URLEncoder

/**
 * 调用地图软件导航到指定位置
 */
fun Context.navigateToLocation(
    mapType: MapType,
    location: LatLng,
    name: String
) {
    val uri: Uri = when (mapType) {
        MapType.AMap -> {
            "amapuri://route/plan?dlat=${location.latitude}&dlon=${location.longitude}&dname=$name&t=0".toUri()
        }

        MapType.Baidu -> {
            val encodedName = URLEncoder.encode(name, "UTF-8")
            "baidumap://map/direction?destination=latlng:${location.latitude},${location.longitude}|name:$encodedName&coord_type=gcj02&mode=driving".toUri()
        }

        MapType.Tencent -> {
            "qqmap://map/routeplan?to=$name&tocoord=${location.latitude},${location.longitude}&type=drive".toUri()
        }

        MapType.Google -> {
            "google.navigation:q=${location.latitude},${location.longitude}".toUri()
        }
    }

    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // 新任务栈标记，防止干扰当前应用回退栈
    }

    runCatching { startActivity(intent) }
        .onFailure { showToast("未安装${mapType.appName}地图") }
}

/**
 * 将指定的图片资源转为地图支持的bitmap
 * 支持指定宽高、旋转角度
 */
suspend fun createBitmapDescriptor(
    context: Context,
    @DrawableRes iconId: Int,
    width: Int? = null,
    height: Int? = null,
    rotationAngle: Float? = null
): BitmapDescriptor? = withContext(Dispatchers.IO) {
    val drawable = ContextCompat.getDrawable(context, iconId) ?: return@withContext null
    val originalWidth = width ?: drawable.intrinsicWidth
    val originalHeight = height ?: drawable.intrinsicHeight
    val bitmap = createBitmap(originalWidth, originalHeight)
    val canvas = Canvas(bitmap)

    // 旋转
    rotationAngle?.let {
        val pivotX = originalWidth / 2f
        val pivotY = originalHeight / 2f
        canvas.save() // 保存画布当前的状态
        canvas.rotate(it, pivotX, pivotY) // 应用旋转
    }

    drawable.setBounds(0, 0, originalWidth, originalHeight)
    drawable.draw(canvas)

    // 如果旋转了画布，现在恢复到之前保存的状态
    rotationAngle?.let {
        canvas.restore()
    }

    BitmapDescriptorFactory.fromBitmap(bitmap)
}

// 判断位置是否加载完成
fun Location.isLoaded() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    this.isComplete
} else {
    this.latitude != 0.0 && this.longitude != 0.0
}

fun LatLonPoint.toLatLng() = LatLng(latitude, longitude)
fun LatLng.toLatLonPoint() = LatLonPoint(latitude, longitude)
fun Location.toLatLng() = LatLng(latitude, longitude)

/**
 * 格式化距离
 *
 * @param meters 米数
 */
fun formatDistance(meters: Int, decimals: Int = 1): String {
    return if (meters >= 1000) {
        "%.${decimals}fkm".format(meters / 1000f)
    } else {
        "${meters}m"
    }
}