package top.chengdongqing.wechat.features.chat.ui.session.peer

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import top.chengdongqing.wechat.features.chat.domain.model.PeerDevice
import top.chengdongqing.wechat.features.chat.domain.model.PeerDeviceUiState

abstract class PeerDeviceViewModel : ViewModel() {

    abstract val uiState: StateFlow<PeerDeviceUiState>

    abstract fun startScan()

    abstract fun stopScan()

    abstract fun connectDevice(device: PeerDevice, userId: String, onSuccess: () -> Unit)

    abstract fun addNearbyDevice(device: PeerDevice)
}