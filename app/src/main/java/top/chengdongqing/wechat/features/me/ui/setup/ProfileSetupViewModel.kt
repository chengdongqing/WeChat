package top.chengdongqing.wechat.features.me.ui.setup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileSetupState(
    val name: String = "",
    val avatarUri: Uri? = null,
    val isSaving: Boolean = false,
    val isValid: Boolean = false
)

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
//    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _setupState = MutableStateFlow(ProfileSetupState())
    val setupState = _setupState.asStateFlow()

    fun setNickname(name: String) {
        _setupState.update { it.copy(name = name) }
    }

    fun setAvatar(avatarUri: Uri) {
        _setupState.update { it.copy(avatarUri = avatarUri) }
    }

    fun completeSetup() {
        viewModelScope.launch {
//            val profile = User(
//                name = _setupState.value.name,
//                avatarPath = saveAvatar(_setupState.value.avatarUri)
//            )
//
//            profileRepository.createProfile(profile)
        }
    }
}