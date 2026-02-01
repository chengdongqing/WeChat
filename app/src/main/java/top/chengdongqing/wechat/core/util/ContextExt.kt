package top.chengdongqing.wechat.core.util

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * 显示提示框
 */
fun Context.showToast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

/**
 * 清除之前产生的所有缓存
 */
fun Context.clearAllCache() {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // 清理内部缓存 (/data/user/0/包名/cache)
            deleteDirContent(cacheDir)
            // 清理外部缓存 (/sdcard/Android/data/包名/cache)
            deleteDirContent(externalCacheDir)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * 删除目录下的内容
 */
private fun deleteDirContent(dir: File?): Boolean {
    return dir != null && if (dir.exists() && dir.isDirectory) {
        dir.listFiles()?.forEach { child ->
            child.deleteRecursively()
        }
        true
    } else {
        false
    }
}