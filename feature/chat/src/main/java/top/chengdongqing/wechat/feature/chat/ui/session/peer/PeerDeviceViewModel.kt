package top.chengdongqing.wechat.feature.chat.ui.session.peer

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import top.chengdongqing.wechat.feature.chat.domain.model.PeerDevice

abstract class PeerDeviceViewModel : ViewModel() {

    abstract val uiState: StateFlow<PeerDeviceUiState>

    abstract fun startScan()

    abstract fun stopScan()

    abstract fun connectDevice(device: PeerDevice, userId: String, onSuccess: () -> Unit)

    abstract fun addNearbyDevice(device: PeerDevice)

    abstract fun reset()
}