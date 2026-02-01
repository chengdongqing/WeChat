package top.chengdongqing.wechat.core.utils

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.ui.utils.DpSaver
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

/**
 * 键盘高度测量模式
 */
enum class KeyboardHeightMode {
    Auto,   // 自动检测
    View,   // 视图测量
    Ime     // IME insets
}

/**
 * 记忆键盘高度（完整版）
 */
@Composable
fun rememberKeyboardHeight(
    mode: KeyboardHeightMode = KeyboardHeightMode.Auto,
    minHeightThreshold: Dp = 100.dp
): Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    val ime = WindowInsets.ime

    val actualMode = remember(mode, view) {
        if (mode == KeyboardHeightMode.Auto) {
            if (isViewInPopup(view)) {
                KeyboardHeightMode.Ime
            } else {
                KeyboardHeightMode.View
            }
        } else {
            mode
        }
    }

    return when (actualMode) {
        KeyboardHeightMode.Ime -> {
            // Popup 模式：实时获取 IME 高度
            with(density) {
                ime.getBottom(density).toDp()
            }
        }

        KeyboardHeightMode.View -> {
            // 普通模式：使用视图测量
            rememberViewKeyboardHeight(view, density, minHeightThreshold)
        }

        KeyboardHeightMode.Auto -> error("Should not reach here")
    }
}

/**
 * 检测 View 是否在 Popup/Dialog 中
 */
private fun isViewInPopup(view: View): Boolean {
    var parent = view.parent
    while (parent != null) {
        val className = parent.javaClass.name
        if (className.contains("Popup", ignoreCase = true) ||
            className.contains("Dialog", ignoreCase = true)
        ) {
            return true
        }
        parent = parent.parent
    }
    return false
}

/**
 * 使用 View 测量键盘高度
 */
@Composable
private fun rememberViewKeyboardHeight(
    view: View,
    density: Density,
    minHeightThreshold: Dp
): Dp {
    val minHeightPx = with(density) { minHeightThreshold.roundToPx() }

    var height by rememberSaveable(stateSaver = DpSaver) {
        mutableStateOf(0.dp)
    }

    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            view.getWindowVisibleDisplayFrame(rect)

            val windowInsets = ViewCompat.getRootWindowInsets(view)
            val systemBarsInsets = windowInsets?.getInsets(
                WindowInsetsCompat.Type.systemBars()
            ) ?: Insets.NONE

            val keyboardHeightPx = view.rootView.height - rect.bottom - systemBarsInsets.bottom

            when {
                keyboardHeightPx > minHeightPx -> {
                    val newHeight = with(density) { keyboardHeightPx.toDp() }
                    if (newHeight != height) {
                        height = newHeight
                    }
                }

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

    return height
}