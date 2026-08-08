package top.chengdongqing.wechat.core.apppicker.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppResult(
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val mimeType: String
) : Parcelable
