package top.chengdongqing.wechat.data.model

import java.io.File

data class MediaResource(
    val file: File,
    val filename: String,
    val mimeType: String,
    val size: Long,
    val width: Int = 0,
    val height: Int = 0,
    val duration: Long = 0,
    val thumbBase64: String? = null
)