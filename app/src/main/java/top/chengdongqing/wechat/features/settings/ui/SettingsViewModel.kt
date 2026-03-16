package top.chengdongqing.wechat.features.settings.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.data.network.service.P2PService
import top.chengdongqing.wechat.data.network.service.createNetworkServiceIntent
import top.chengdongqing.wechat.features.settings.domain.usecase.LogoutUseCase

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val logout: LogoutUseCase,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _logoutResult = MutableSharedFlow<Result<Unit>>()
    val logoutResult = _logoutResult.asSharedFlow()

    fun exit() {
        viewModelScope.launch {
            _logoutResult.emit(logout())

            // 停止所有后台服务
            runCatching {
                val intent = context.createNetworkServiceIntent(P2PService.ACTION_STOP_SERVICE)
                context.startService(intent)
            }
        }
    }
}