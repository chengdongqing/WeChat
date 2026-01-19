package top.chengdongqing.wechat.core.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * 将 Base64 字符串解码为 Bitmap
 */
fun String.base64ToBitmap(): Bitmap? {
    if (isNullOrBlank()) return null

    return try {
        val byteArray = Base64.decode(this, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Bitmap 转 base64字符串
 */
fun Bitmap.toBase64(quality: Int = 80): String {
    val outputStream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    return Base64.encodeToString(
        outputStream.toByteArray(),
        Base64.DEFAULT
    )
}