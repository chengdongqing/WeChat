package top.chengdongqing.wechat.features.chat.ui.session.peer

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.data.network.connection.wifi.WiFiDirectConnector
import top.chengdongqing.wechat.data.network.service.chat.WiFiDirectChatHandler
import top.chengdongqing.wechat.features.chat.domain.model.PeerDevice
import top.chengdongqing.wechat.features.chat.domain.model.PeerDeviceUiState
import top.chengdongqing.wechat.features.chat.domain.model.WiFiDirectRole
import javax.inject.Inject

@HiltViewModel
class WiFiDirectPeerViewModel @Inject constructor(
    private val wifiDirectConnector: WiFiDirectConnector,
    private val wifiDirectChatModule: WiFiDirectChatHandler,
    @param:ApplicationContext private val context: Context
) : PeerDeviceViewModel() {

    private val _uiState = MutableStateFlow(PeerDeviceUiState())
    override val uiState = _uiState.asStateFlow()

    private val p2pManager by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    }
    private val channel by lazy {
        p2pManager.initialize(context, Looper.getMainLooper(), null)
    }

    fun startAsOwner() {
        viewModelScope.launch {
            _uiState.update { it.copy(role = WiFiDirectRole.Owner, error = null) }
            wifiDirectChatModule.startAsOwner()
        }
    }

    fun startAsClient() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    role = WiFiDirectRole.Client,
                    isScanning = true,
                    error = null
                )
            }
            wifiDirectChatModule.startAsClient()
            startScan()
        }
    }

    override fun startScan() {
        _uiState.update { it.copy(isScanning = true, error = null) }
        // 先停止旧的扫描，完成后再启动，避免状态残留
        p2pManager.cancelConnect(channel, null)
        p2pManager.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = discoverPeers()
            override fun onFailure(reason: Int) = discoverPeers()
        })
    }

    @SuppressLint("MissingPermission")
    private fun discoverPeers() {
        p2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {}

            override fun onFailure(reason: Int) {
                val reason = when (reason) {
                    WifiP2pManager.ERROR -> "内部错误（检查位置权限）"
                    WifiP2pManager.P2P_UNSUPPORTED -> "设备不支持 WiFi Direct"
                    WifiP2pManager.BUSY -> "系统忙，请稍后重试"
                    else -> "未知错误: $reason"
                }
                _uiState.update { it.copy(isScanning = false, error = "扫描失败：$reason") }
            }
        })
    }

    override fun stopScan() {
        p2pManager.stopPeerDiscovery(channel, null)
        _uiState.update { it.copy(isScanning = false) }
    }

    override fun connectDevice(device: PeerDevice, userId: String, onSuccess: () -> Unit) {
        val p2pDevice = (device as? PeerDevice.WiFiDirect)?.device ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(connectingDeviceId = device.id, error = null) }
            runCatching {
                wifiDirectConnector.connect(
                    p2pManager = p2pManager,
                    channel = channel,
                    device = p2pDevice
                )
            }.onSuccess {
                _uiState.update { it.copy(connectingDeviceId = null) }
                onSuccess()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        connectingDeviceId = null,
                        error = e.message ?: "连接失败"
                    )
                }
            }
        }
    }

    override fun addNearbyDevice(device: PeerDevice) {
        _uiState.update { state ->
            val updated = state.nearbyDevices.toMutableList()
            val index = updated.indexOfFirst { it.id == device.id }
            if (index >= 0) updated[index] = device else updated.add(device)
            state.copy(nearbyDevices = updated)
        }
    }

    fun onPeersChanged(devices: List<WifiP2pDevice>) {
        val peers = devices.map { d ->
            PeerDevice.WiFiDirect(
                id = d.deviceAddress,
                name = d.deviceName,
                isPaired = d.status == WifiP2pDevice.CONNECTED,
                device = d,
            )
        }
        _uiState.update { it.copy(nearbyDevices = peers, isScanning = false) }
    }

    override fun reset() {
        _uiState.value = PeerDeviceUiState()
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}