package top.chengdongqing.wechat.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import top.chengdongqing.wechat.core.model.ContactResult

/**
 * Cross-feature contract for selecting contacts.
 *
 * The requesting feature receives only selected contact data. The app host injects
 * the UI implementation, so Chat never depends on Contacts' Activity or Compose UI.
 */
interface ContactPickerLauncher {
    @Composable
    fun rememberLauncher(
        excludeSelf: Boolean = false,
        onResult: (Array<ContactResult>) -> Unit
    ): (maxSelection: Int) -> Unit
}

val LocalContactPickerLauncher = staticCompositionLocalOf<ContactPickerLauncher> {
    object : ContactPickerLauncher {
        @Composable
        override fun rememberLauncher(
            excludeSelf: Boolean,
            onResult: (Array<ContactResult>) -> Unit
        ): (maxSelection: Int) -> Unit = {
            error("ContactPickerLauncher is not provided by the app host")
        }
    }
}
