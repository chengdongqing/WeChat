package top.chengdongqing.wechat.data.model

import java.io.File

data class MediaResource(
    val file: File,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val thumbBase64: String?
)