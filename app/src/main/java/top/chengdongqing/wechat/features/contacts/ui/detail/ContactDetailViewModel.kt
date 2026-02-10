package top.chengdongqing.wechat.features.contacts.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.R
import top.chengdongqing.wechat.core.util.randomUUID
import top.chengdongqing.wechat.data.model.Gender
import top.chengdongqing.wechat.features.contacts.data.repository.ContactP2PRepository
import top.chengdongqing.wechat.features.contacts.domain.model.Contact

data class ContactDetailUiState(
    val contact: Contact,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 联系人详情ViewModel
 *
 * 负责管理联系人详情页面的业务逻辑和状态
 *
 * 主要功能：
 * - 加载联系人详细信息
 * - 处理用户操作（发消息、通话等）
 * - 管理UI状态和错误处理
 */
@HiltViewModel(assistedFactory = ContactDetailViewModel.Factory::class)
class ContactDetailViewModel @AssistedInject constructor(
    @Assisted private val contactId: String,
    private val contactP2PRepository: ContactP2PRepository  // 注入
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(contactId: String): ContactDetailViewModel
    }

    private val _uiState = MutableStateFlow(
        ContactDetailUiState(
            contact = Contact(
                id = contactId,
                nickname = "加载中...",
            ),
            isLoading = true
        )
    )
    val uiState: StateFlow<ContactDetailUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    init {
        loadContactDetails()
    }

    /**
     * 加载联系人详细信息
     */
    private fun loadContactDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // 从缓存获取（刚扫码获取的数据）
                val contact = ContactP2PRepository.getContactFromCache(contactId)

                if (contact != null) {
                    _uiState.update {
                        it.copy(
                            contact = contact,
                            isLoading = false
                        )
                    }
                } else {
                    // 如果缓存没有，说明不是通过扫码进入的
                    // 可以尝试从数据库或其他方式加载
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "未找到联系人信息"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "加载联系人信息失败：${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 处理联系人操作
     */
    fun handleAction(action: ContactAction) {
        viewModelScope.launch {
            when (action) {
                ContactAction.SendMessage -> {
                    _navigationEvent.emit(NavigationEvent.NavigateToChat(contactId))
                }

                ContactAction.VoiceVideoCall -> {
                    _navigationEvent.emit(NavigationEvent.NavigateToCall(contactId))
                }

                ContactAction.ViewMoments -> {
                    _navigationEvent.emit(NavigationEvent.NavigateToMoments(contactId))
                }

                ContactAction.ViewProfile -> {
                    _navigationEvent.emit(NavigationEvent.NavigateToProfile(contactId))
                }

                ContactAction.ShowMore -> {
                    _navigationEvent.emit(NavigationEvent.ShowMoreOptions(contactId))
                }

                ContactAction.DeleteContact -> {

                }

                ContactAction.AddToContacts -> {
                    _navigationEvent.emit(NavigationEvent.NavigateToRequestAdd(contactId))
                }
            }
        }
    }

    /**
     * 更新联系人标签
     */
    fun updateTags(tags: List<String>) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    contact = state.contact.copy(tags = tags)
                )
            }
            // contactRepository.updateContactTags(contactId, tags)
        }
    }

    /**
     * 更新联系人备注
     */
    fun updateRemark(remark: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    contact = state.contact.copy(note = remark)
                )
            }
            // contactRepository.updateContactRemark(contactId, remark)
        }
    }

    /**
     * 刷新联系人信息
     */
    fun refresh() {
        loadContactDetails()
    }

    private fun createSampleContact(): Contact {
        return Contact(
            id = "wxid_${randomUUID().take(12)}",
            nickname = "海盐芝士不加糖",
            gender = Gender.Male,
            avatarPath = "",
            remarkName = "老舅",
            tags = listOf("朋友"),
            note = "在林拉高速上认识的摩友",
            momentPhotos = listOf(
                R.drawable.img_splash,
                R.drawable.img_location_placeholder,
                R.drawable.img_radar_bg
            )
        )
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * 导航事件
 */
sealed class NavigationEvent {
    data class NavigateToChat(val contactId: String) : NavigationEvent()
    data class NavigateToCall(val contactId: String) : NavigationEvent()
    data class NavigateToMoments(val contactId: String) : NavigationEvent()
    data class NavigateToProfile(val contactId: String) : NavigationEvent()
    data class NavigateToRequestAdd(val contactId: String) : NavigationEvent()
    data class ShowMoreOptions(val contactId: String) : NavigationEvent()
}