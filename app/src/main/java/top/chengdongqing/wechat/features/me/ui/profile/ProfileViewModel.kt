package top.chengdongqing.wechat.features.me.ui.profile

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
//    private val profileRepository: ProfileRepository
) : ViewModel() {

//    val profile = profileRepository.getProfile()
//        .stateIn(viewModelScope, SharingStarted.Lazily, null)
//
//    fun updateName(name: String) {
//        viewModelScope.launch {
//            profile.value?.let { current ->
//                profileRepository.updateProfile(
//                    current.copy(nickname = name)
//                )
//            }
//        }
//    }
}