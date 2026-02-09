package top.chengdongqing.wechat.features.me.ui.profile

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.data.manager.FileManager
import top.chengdongqing.wechat.data.model.UserProfile
import top.chengdongqing.wechat.features.contacts.domain.usecase.QRCodeResult
import top.chengdongqing.wechat.features.contacts.domain.usecase.QRCodeUseCase
import top.chengdongqing.wechat.features.me.repository.ProfileRepository
import javax.inject.Inject

/**
 * 个人资料页面 UI 状态
 */
data class ProfileUiState(
    val profile: UserProfile? = null,
    val myQRCode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 个人资料 ViewModel
 *
 * 职责：
 * - 加载和展示当前用户资料
 * - 提供资料更新功能
 * - 管理头像 URI 转换
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val fileManager: FileManager,
    private val qrCodeUseCase: QRCodeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ProfileUiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadProfile()
        generateQRCode()
    }

    /**
     * 加载用户资料
     */
    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            profileRepository.getCurrentProfile()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "加载资料失败: ${error.message}"
                        )
                    }
                }
                .collect { profile ->
                    _uiState.update {
                        it.copy(
                            profile = profile,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    /**
     * 生成我的二维码
     */
    private fun generateQRCode() {
        viewModelScope.launch {
            qrCodeUseCase.generateMyQRCode().fold(
                onSuccess = { qrCode ->
                    _uiState.update { it.copy(myQRCode = qrCode) }
                },
                onFailure = { e ->
                    _eventFlow.emit(ProfileUiEvent.ShowError("生成二维码失败: ${e.message}"))
                }
            )
        }
    }

    /**
     * 处理扫描到的二维码
     */
    fun handleScannedQRCode(qrContent: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = qrCodeUseCase.handleScannedQRCode(qrContent)) {
                is QRCodeResult.AddFriend -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(ProfileUiEvent.NavigateToContactDetail(result.contact.id))
                }

                is QRCodeResult.OpenUrl -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(ProfileUiEvent.OpenUrl(result.url))
                }

                is QRCodeResult.ShowText -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(ProfileUiEvent.NavigateToPlainText(result.text))
                }

                is QRCodeResult.JoinGroup -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(ProfileUiEvent.ShowError("群聊功能开发中"))
                }

                is QRCodeResult.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(ProfileUiEvent.ShowError(result.message))
                }
            }
        }
    }

    /**
     * 统一更新入口
     */
    fun updateField(field: ProfileField) {
        viewModelScope.launch {
            // 前置验证逻辑
            val validationError = validateInput(field)
            if (validationError != null) {
                _eventFlow.emit(ProfileUiEvent.ShowError(validationError))
                return@launch
            }

            // 开启 Loading
            _uiState.update { it.copy(isLoading = true) }

            // 执行具体业务逻辑
            val result = performUpdate(field)

            // 处理结果
            _uiState.update { it.copy(isLoading = false) }
            result.onSuccess {
                _eventFlow.emit(ProfileUiEvent.UpdateSuccess)
            }.onFailure { e ->
                _eventFlow.emit(ProfileUiEvent.ShowError(e.message ?: "更新失败"))
            }
        }
    }

    private suspend fun performUpdate(field: ProfileField): Result<Unit> {
        return when (field) {
            is ProfileField.Nickname -> profileRepository.updateProfile(nickname = field.value.trim())
            is ProfileField.Gender -> profileRepository.updateProfile(gender = field.value)
            is ProfileField.Signature -> profileRepository.updateProfile(signature = field.value.trim())
            is ProfileField.Avatar -> handleAvatarUpdate(field.uri)
        }
    }

    private suspend fun handleAvatarUpdate(uri: Uri): Result<Unit> {
        val current = _uiState.value.profile ?: return Result.failure(Exception("资料未加载"))
        return try {
            current.avatarPath?.let { fileManager.deleteAvatar(it) }
            val newPath = fileManager.saveAvatar(uri, current.id).getOrThrow()
            profileRepository.updateProfile(avatarPath = newPath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validateInput(field: ProfileField): String? = when (field) {
        is ProfileField.Nickname -> {
            if (field.value.isBlank()) "昵称不能为空"
            else if (!UserProfile.isValidName(field.value)) "昵称格式不正确"
            else null
        }

        else -> null // 其他字段暂不设强制验证
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

sealed class ProfileField {
    data class Nickname(val value: String) : ProfileField()
    data class Gender(val value: top.chengdongqing.wechat.data.model.Gender) : ProfileField()
    data class Signature(val value: String) : ProfileField()
    data class Avatar(val uri: Uri) : ProfileField()
}

sealed class ProfileUiEvent {
    object UpdateSuccess : ProfileUiEvent()
    data class ShowError(val message: String) : ProfileUiEvent()
    data class NavigateToContactDetail(val contactId: String) : ProfileUiEvent()
    data class OpenUrl(val url: String) : ProfileUiEvent()
    data class NavigateToPlainText(val text: String) : ProfileUiEvent()
}

@Composable
fun ProfileEventEffect(
    viewModel: ProfileViewModel,
    onSuccess: () -> Unit = {},
    onError: (error: String) -> Unit = {},
) {
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is ProfileUiEvent.UpdateSuccess -> onSuccess()
                is ProfileUiEvent.ShowError -> onError(event.message)
                else -> {}
            }
        }
    }
}