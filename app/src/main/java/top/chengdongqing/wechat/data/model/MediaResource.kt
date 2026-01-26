package top.chengdongqing.wechat.data.model

import java.io.File

data class MediaResource(
    val file: File,
    val filename: String,
    val mimeType: String,
    val size: Long,
    val thumbBase64: String?
)