package top.chengdongqing.wechat.features.me.ui.profile

import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.core.file.FileManager
import top.chengdongqing.wechat.core.file.MediaStoreManager
import top.chengdongqing.wechat.data.model.MessageType
import top.chengdongqing.wechat.features.contacts.domain.usecase.QRCodeResult
import top.chengdongqing.wechat.features.contacts.domain.usecase.QRCodeUseCase
import top.chengdongqing.wechat.features.me.domain.model.UserProfile
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject

/**
 * 个人资料 ViewModel
 *
 * 职责：
 * - 管理用户资料的加载与更新
 * - 处理二维码生成与扫描
 * - 协调导航事件
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val fileManager: FileManager,
    private val mediaStoreManager: MediaStoreManager,
    private val qrCodeUseCase: QRCodeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ProfileUiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadProfile()
    }

    /**
     * 加载用户资料
     */
    private fun loadProfile() {
        viewModelScope.launch {
            profileRepository.observeProfile()
                .catch { error ->
                    handleError("加载资料失败: ${error.message}")
                }
                .collect { profile ->
                    _uiState.update {
                        it.copy(
                            profile = profile,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    /**
     * 生成我的二维码
     */
    fun generateQRCode() {
        viewModelScope.launch {
            qrCodeUseCase.generateMyQRCode().fold(
                onSuccess = { qrCode ->
                    _uiState.update { it.copy(qrCode = qrCode) }
                },
                onFailure = { e ->
                    _eventFlow.emit(ProfileUiEvent.ShowError("生成二维码失败: ${e.message}"))
                }
            )
        }
    }

    /**
     * 处理扫描到的二维码
     *
     * @param qrContent 二维码字符串
     */
    fun handleScannedQRCode(qrContent: String) {
        viewModelScope.launch {
            setLoading(true)

            val event = when (val result = qrCodeUseCase.handleScannedQRCode(qrContent)) {
                is QRCodeResult.AddFriend -> ProfileUiEvent.NavigateToContactDetail(result.contact.id)
                is QRCodeResult.OpenUrl -> ProfileUiEvent.OpenUrl(result.url)
                is QRCodeResult.ShowText -> ProfileUiEvent.NavigateToPlainText(result.text)
                is QRCodeResult.JoinGroup -> ProfileUiEvent.ShowError("群聊功能开发中")
                is QRCodeResult.Error -> ProfileUiEvent.ShowError(result.message)
            }

            setLoading(false)
            emitEvent(event)
        }
    }

    /**
     * 更新用户资料字段
     *
     * @param field 要更新的字段
     */
    fun updateField(field: ProfileField) {
        viewModelScope.launch {
            // 验证输入
            validateInput(field)?.let { error ->
                _eventFlow.emit(ProfileUiEvent.ShowError(error))
                return@launch
            }

            // 执行更新
            setLoading(true)
            val result = performUpdate(field)
            setLoading(false)

            // 处理结果
            result.fold(
                onSuccess = { emitEvent(ProfileUiEvent.UpdateSuccess) },
                onFailure = { error ->
                    emitEvent(ProfileUiEvent.ShowError(error.message ?: "更新失败"))
                }
            )
        }
    }

    /**
     * 执行具体的更新操作
     */
    private suspend fun performUpdate(field: ProfileField): Result<Unit> {
        return when (field) {
            is ProfileField.Nickname -> profileRepository.updateProfile(nickname = field.value.trim())
            is ProfileField.Gender -> profileRepository.updateProfile(gender = field.value)
            is ProfileField.Signature -> profileRepository.updateProfile(signature = field.value.trim())
            is ProfileField.Avatar -> updateAvatar(field.uri)
        }
    }

    /**
     * 更新头像
     *
     * 1. 删除旧头像
     * 2. 保存新头像
     * 3. 更新资料
     */
    private suspend fun updateAvatar(uri: Uri): Result<Unit> {
        val profile = _uiState.value.profile
            ?: return Result.failure(Exception("资料未加载"))

        return try {
            // 删除旧头像
            profile.avatarPath?.let { fileManager.deleteAvatar(it) }

            // 保存新头像
            val newPath = fileManager.saveAvatar(uri, profile.id).getOrThrow()

            // 更新资料
            profileRepository.updateProfile(avatarPath = newPath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 验证输入
     *
     * @return 错误信息，null 表示验证通过
     */
    private fun validateInput(field: ProfileField): String? {
        return when (field) {
            is ProfileField.Nickname -> when {
                field.value.isBlank() -> "昵称不能为空"
                !UserProfile.isValidName(field.value) -> "昵称格式不正确"
                else -> null
            }

            else -> null // 其他字段暂不强制验证
        }
    }

    /**
     * 保存图片
     */
    suspend fun saveImage(uri: Uri): Boolean {
        return mediaStoreManager.saveMedia(
            messageType = MessageType.Image,
            sourceUri = uri
        ) != null
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ========== 辅助方法 ==========

    private fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    private fun handleError(message: String) {
        _uiState.update {
            it.copy(isLoading = false, error = message)
        }
    }

    private suspend fun emitEvent(event: ProfileUiEvent) {
        _eventFlow.emit(event)
    }
}

/**
 * 个人资料页面 UI 状态
 */
data class ProfileUiState(
    val profile: UserProfile? = null,
    val qrCode: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 资料字段封装
 */
sealed class ProfileField {
    data class Nickname(val value: String) : ProfileField()
    data class Gender(val value: top.chengdongqing.wechat.features.me.domain.model.Gender) :
        ProfileField()

    data class Signature(val value: String) : ProfileField()
    data class Avatar(val uri: Uri) : ProfileField()
}

/**
 * UI 事件
 */
sealed class ProfileUiEvent {
    object UpdateSuccess : ProfileUiEvent()
    data class ShowError(val message: String) : ProfileUiEvent()
    data class NavigateToContactDetail(val contactId: String) : ProfileUiEvent()
    data class OpenUrl(val url: String) : ProfileUiEvent()
    data class NavigateToPlainText(val text: String) : ProfileUiEvent()
}

/**
 * 处理资料更新事件的副作用
 */
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

/**
 * 处理 ProfileViewModel 的导航事件
 *
 * 适用于所有需要处理 Profile 导航的页面
 *
 * @param viewModel ProfileViewModel 实例
 * @param snackbarHostState Snackbar 宿主状态
 * @param onNavigateToContactDetail 导航到联系人详情回调
 * @param onNavigateToPlainText 导航到纯文本页面回调
 * @param onNavigateToWebView 导航到 WebView 回调
 */
@Composable
fun HandleProfileNavigationEvents(
    viewModel: ProfileViewModel,
    snackbarHostState: SnackbarHostState,
    onNavigateToContactDetail: (String) -> Unit,
    onNavigateToPlainText: (String) -> Unit,
    onNavigateToWebView: (String) -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ProfileUiEvent.NavigateToContactDetail -> {
                    onNavigateToContactDetail(event.contactId)
                }

                is ProfileUiEvent.NavigateToPlainText -> {
                    onNavigateToPlainText(event.text)
                }

                is ProfileUiEvent.OpenUrl -> {
                    onNavigateToWebView(event.url)
                }

                is ProfileUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                else -> {} // 其他事件由具体页面处理
            }
        }
    }
}