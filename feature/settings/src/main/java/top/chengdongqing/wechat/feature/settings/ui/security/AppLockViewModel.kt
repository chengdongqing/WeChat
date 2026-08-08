package top.chengdongqing.wechat.feature.settings.ui.security

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import top.chengdongqing.wechat.core.security.AppLockManager

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val appLockManager: AppLockManager
) : ViewModel() {
    var isEnabled by mutableStateOf(appLockManager.isEnabled)
        private set

    fun verify(pin: String): Boolean = appLockManager.verify(pin)

    val isTemporarilyLocked: Boolean
        get() = appLockManager.isTemporarilyLocked

    fun refresh() {
        isEnabled = appLockManager.isEnabled
    }

    fun save(pin: String) {
        appLockManager.setPin(pin)
        isEnabled = true
    }

    fun disable() {
        appLockManager.clear()
        isEnabled = false
    }
}
