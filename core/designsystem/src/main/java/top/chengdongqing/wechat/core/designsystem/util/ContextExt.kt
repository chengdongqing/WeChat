package top.chengdongqing.wechat.core.designsystem.util

import android.content.Context
import android.widget.Toast

/**
 * 显示提示框
 */
fun Context.showToast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
