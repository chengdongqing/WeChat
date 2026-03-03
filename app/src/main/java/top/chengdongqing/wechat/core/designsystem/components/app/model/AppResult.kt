package top.chengdongqing.wechat.core.designsystem.components.app.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppResult(
    val fileName: String,
    val filePath: String,
    val fileSize: Long
) : Parcelable

fun List<AppItem>.toResult() = map { app ->
    AppResult(
        fileName = "${app.name}-v${app.versionName}.apk",
        filePath = app.apkPath,
        fileSize = app.apkSize
    )
}