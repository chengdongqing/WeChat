package top.chengdongqing.wechat.features.contacts.ui.add.radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.data.network.model.RadarBeacon
import top.chengdongqing.wechat.features.contacts.domain.model.Contact
import top.chengdongqing.wechat.features.contacts.domain.repository.ContactP2PRepository
import top.chengdongqing.wechat.features.contacts.domain.repository.RadarDiscoveryRepository
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import kotlin.random.Random

@HiltViewModel
class RadarScanViewModel @Inject constructor(
    private val radarRepository: RadarDiscoveryRepository,
    private val contactP2PRepository: ContactP2PRepository,
    profileRepository: ProfileRepository
) : ViewModel() {

    val radarUsers = radarRepository.nearbyUsers
        .map { beacons -> beacons.map { it.toRadarUser() } }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val myProfile = profileRepository.getCurrentProfile()

    /** 正在加载中的用户ID，用于在头像上显示 loading */
    private val _loadingUserId = MutableStateFlow<String?>(null)
    val loadingUserId = _loadingUserId.asStateFlow()

    /** 加载失败的错误信息 */
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    /** 加载成功后待跳转的联系人，消费后置 null */
    private val _navigateToContact = MutableStateFlow<Contact?>(null)
    val navigateToContact = _navigateToContact.asStateFlow()

    init {
        radarRepository.startDiscovery()
    }

    /**
     * 点击雷达头像，通过 BLE 拉取对方资料，成功后触发页面跳转。
     * 同一时间只允许一个用户在加载中，重复点击同一用户会被忽略。
     */
    fun onUserClicked(user: RadarUser) {
        if (_loadingUserId.value != null) return

        viewModelScope.launch {
            _loadingUserId.value = user.id
            _error.value = null

            val contact = contactP2PRepository.fetchPeerContactViaBle(user.id)
            if (contact != null) {
                _navigateToContact.value = contact
            } else {
                _error.value = "获取 ${user.nickname} 的资料失败，请重试"
            }

            _loadingUserId.value = null
        }
    }

    fun onNavigateConsumed() {
        _navigateToContact.value = null
    }

    fun onErrorConsumed() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        radarRepository.stopDiscovery()
    }
}

private fun RadarBeacon.toRadarUser(): RadarUser {
    // 用 userId hashCode 作种子，保证坐标稳定不抖动
    val seed = userId.hashCode().toLong()
    val random = Random(seed)

    return RadarUser(
        id = userId,
        nickname = nickname,
        avatarUrl = avatarUrl,
        angle = random.nextDouble() * 360.0,
        distance = 0.6f + random.nextFloat() * 0.5f  // 保持在 0.4~0.9 之间，不紧贴中心也不超出边界
    )
}

data class RadarUser(
    val id: String,
    val nickname: String,
    val avatarUrl: String?,
    val angle: Double, // 0..360
    val distance: Float // 0..1 (0代表中心，1代表雷达边缘)
)