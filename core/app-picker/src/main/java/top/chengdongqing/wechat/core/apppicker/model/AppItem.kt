package top.chengdongqing.wechat.core.apppicker.model

import androidx.compose.runtime.Immutable

@Immutable
data class AppItem(
    val name: String,
    val packageName: String,
    val versionName: String,
    val lastModified: Long,
    val apkPath: String
)
