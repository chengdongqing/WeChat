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

@HiltViewModel
class RadarScanViewModel @Inject constructor(
    private val radarRepository: RadarDiscoveryRepository,
    private val contactP2PRepository: ContactP2PRepository,
    profileRepository: ProfileRepository
) : ViewModel() {

    val radarUsers = radarRepository.nearbyUsers
        .map { beacons -> beacons.toRadarUsers() }
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

private fun List<RadarBeacon>.toRadarUsers(): List<RadarUser> {
    val result = mutableListOf<RadarUser>()

    val rings = when {
        this.size <= 4 -> listOf(RingConfig(radius = 0.75f, slotCount = this.size))
        else -> listOf(
            RingConfig(radius = 0.72f, slotCount = 4),
            RingConfig(radius = 1.0f, slotCount = this.size - 4)
        )
    }

    var processedCount = 0
    rings.forEach { ring ->
        val countInThisRing = ring.slotCount
        repeat(countInThisRing) { i ->
            if (processedCount >= this.size) return@repeat

            val initialRotation = 45.0
            val angle = (360.0 / countInThisRing) * i + initialRotation

            result.add(
                RadarUser(
                    id = this[processedCount].userId,
                    nickname = this[processedCount].nickname,
                    avatarUrl = this[processedCount].avatarUrl,
                    angle = angle % 360.0,
                    distance = ring.radius
                )
            )
            processedCount++
        }
    }
    return result
}

private data class RingConfig(val radius: Float, val slotCount: Int)

data class RadarUser(
    val id: String,
    val nickname: String,
    val avatarUrl: String?,
    val angle: Double, // 0..360
    val distance: Float // 0..1 (0代表中心，1代表雷达边缘)
)