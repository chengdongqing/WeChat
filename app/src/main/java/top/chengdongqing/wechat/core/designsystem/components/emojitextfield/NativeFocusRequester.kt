package top.chengdongqing.wechat.core.designsystem.components.emojitextfield

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import java.lang.ref.WeakReference

/**
 * 对接原生输入框的焦点管理器
 */
class NativeFocusRequester {
    private var editTextRef: WeakReference<AppCompatEditText>? = null

    internal fun bind(editText: AppCompatEditText) {
        editTextRef = WeakReference(editText)
    }

    val selectionStart: Int
        get() = editTextRef?.get()?.selectionStart ?: 0

    fun setSelection(index: Int) {
        editTextRef?.get()?.setSelection(index)
    }

    fun requestFocus(showKeyboard: Boolean = true) {
        editTextRef?.get()?.let { view ->
            view.requestFocus()

            if (showKeyboard) {
                val imm =
                    view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    fun clearFocus() {
        editTextRef?.get()?.clearFocus()
    }

    fun post(action: () -> Unit) {
        editTextRef?.get()?.post { action() }
    }
}