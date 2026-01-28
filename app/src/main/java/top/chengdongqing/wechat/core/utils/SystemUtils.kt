package top.chengdongqing.wechat.core.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Rect
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
 * 寻找Activity
 */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
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

@Composable
fun rememberStatusBarHeight(): Dp {
    val density = LocalDensity.current
    val statusBars = WindowInsets.statusBars

    return remember {
        with(density) {
            statusBars.getTop(this).toDp()
        }
    }
}

@Composable
fun rememberKeyboardHeight(
    minHeightThreshold: Dp = 100.dp
): Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    val minHeightPx = with(density) { minHeightThreshold.roundToPx() }

    var height by rememberSaveable(stateSaver = DpSaver) {
        mutableStateOf(0.dp)
    }

    val stableHeight by remember {
        derivedStateOf { height }
    }

    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            view.getWindowVisibleDisplayFrame(rect)

            val windowInsets = ViewCompat.getRootWindowInsets(view)
            val systemBarsInsets = windowInsets?.getInsets(
                WindowInsetsCompat.Type.systemBars()
            ) ?: Insets.NONE

            // 计算键盘高度
            val rootHeight = view.rootView.height
            val visibleBottom = rect.bottom
            val systemBottom = systemBarsInsets.bottom
            val keyboardHeightPx = rootHeight - visibleBottom - systemBottom

            when {
                // 键盘打开
                keyboardHeightPx > minHeightPx -> {
                    val newHeight = with(density) { keyboardHeightPx.toDp() }
                    if (newHeight != height) {
                        height = newHeight
                    }
                }

                // 键盘关闭
                keyboardHeightPx <= 0 && height > 0.dp -> {
                    height = 0.dp
                }
            }
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(listener)

        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    return stableHeight
}

private val DpSaver = Saver<Dp, Float>(
    save = { it.value },
    restore = { it.dp }
)