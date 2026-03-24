package top.chengdongqing.wechat.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.feature.settings.domain.usecase.LogoutUseCase

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val logout: LogoutUseCase
) : ViewModel() {

    private val _logoutResult = MutableSharedFlow<Result<Unit>>()
    val logoutResult = _logoutResult.asSharedFlow()

    fun exit() {
        viewModelScope.launch {
            _logoutResult.emit(logout())
        }
    }
}