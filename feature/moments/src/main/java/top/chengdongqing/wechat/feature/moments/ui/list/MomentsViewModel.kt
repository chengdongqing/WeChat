package top.chengdongqing.wechat.feature.moments.ui.list

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.chengdongqing.wechat.core.data.repository.ProfileRepository
import top.chengdongqing.wechat.feature.moments.data.MomentsLanSync
import top.chengdongqing.wechat.feature.moments.data.MomentsRepository
import javax.inject.Inject

@HiltViewModel
class MomentsViewModel @Inject constructor(
    private val repository: MomentsRepository,
    profileRepository: ProfileRepository,
    lanSync: MomentsLanSync
) : ViewModel() {
    val state = repository.state
    val profile = profileRepository.requireProfile()

    init {
        lanSync.start()
    }

    fun publish(content: String, images: List<Uri>) = repository.publish(content, images)
    fun publishVideo(content: String, video: Uri) = repository.publishVideo(content, video)
    fun toggleLike(id: String) = repository.toggleLike(id)
    fun comment(id: String, text: String) = repository.comment(id, text)
    fun delete(id: String) = repository.delete(id)
    fun setCover(uri: Uri) = repository.setCover(uri)
    fun setCoverFromUrl(url: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.setCoverFromUrl(url)
            withContext(Dispatchers.Main) {
                onComplete(success)
            }
        }
    }
}
