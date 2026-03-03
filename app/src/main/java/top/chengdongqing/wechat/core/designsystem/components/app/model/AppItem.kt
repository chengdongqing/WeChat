package top.chengdongqing.wechat.core.designsystem.components.app.model

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable

@Immutable
data class AppItem(
    val name: String,
    val icon: Drawable,
    val packageName: String,
    val versionName: String,
    val lastModified: Long,
    val apkPath: String,
    val apkSize: Long
)