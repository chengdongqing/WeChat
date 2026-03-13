package top.chengdongqing.wechat.features.chat.ui.session.peer

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.data.database.dao.ConnectionInfoDao
import top.chengdongqing.wechat.data.database.entity.ConnectionInfoEntity
import top.chengdongqing.wechat.data.network.connection.ConnectionMode
import top.chengdongqing.wechat.data.network.connection.wifi.WiFiDirectConnector
import top.chengdongqing.wechat.data.network.service.modules.WiFiDirectChatModule
import top.chengdongqing.wechat.features.chat.domain.model.PeerDevice
import top.chengdongqing.wechat.features.chat.domain.model.PeerDeviceUiState
import top.chengdongqing.wechat.features.chat.domain.model.WiFiDirectRole
import javax.inject.Inject

@HiltViewModel
class WiFiDirectPeerViewModel @Inject constructor(
    private val connectionInfoDao: ConnectionInfoDao,
    private val wifiDirectConnector: WiFiDirectConnector,
    private val wifiDirectChatModule: WiFiDirectChatModule,
    @param:ApplicationContext private val context: Context
) : PeerDeviceViewModel() {

    companion object {
        private const val TAG = "WiFiDirectPeerViewModel"
    }

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

        // 先清理旧状态
        p2pManager.cancelConnect(channel, null)
        p2pManager.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // 停止完成后再开始新的扫描
                discoverPeers()
            }

            override fun onFailure(reason: Int) {
                // 停止失败也继续，可能本来就没在扫描
                discoverPeers()
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun discoverPeers() {
        p2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "P2P 扫描已启动")
            }

            override fun onFailure(reason: Int) {
                val reasonText = when (reason) {
                    WifiP2pManager.ERROR -> "内部错误（检查位置权限）"
                    WifiP2pManager.P2P_UNSUPPORTED -> "设备不支持 WiFi Direct"
                    WifiP2pManager.BUSY -> "系统忙，请稍后重试"
                    else -> "未知错误: $reason"
                }
                _uiState.update { it.copy(isScanning = false, error = "扫描失败：$reasonText") }
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

                val info = ConnectionInfoEntity(
                    userId = userId,
                    connectionMode = ConnectionMode.WiFiDirect,
                    ipAddress = "192.168.49.1",
                    port = 8888,
                    isOnline = true,
                    lastSeen = System.currentTimeMillis(),
                    priority = 0
                )
                connectionInfoDao.insertOrUpdate(info)
                println("已保存连接信息: $info")

                onSuccess()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(connectingDeviceId = null, error = e.message ?: "连接失败")
                }
            }
        }
    }

    override fun addNearbyDevice(device: PeerDevice) {
        _uiState.update { state ->
            val existing = state.nearbyDevices.indexOfFirst { it.id == device.id }
            val updated = state.nearbyDevices.toMutableList()
            if (existing >= 0) updated[existing] = device else updated.add(device)
            state.copy(nearbyDevices = updated)
        }
    }

    fun onPeersChanged(devices: List<WifiP2pDevice>) {
        val peers = devices.map { device ->
            PeerDevice.WiFiDirect(
                id = device.deviceAddress,
                name = device.deviceName,
                isPaired = device.status == WifiP2pDevice.CONNECTED,
                device = device
            )
        }
        _uiState.update { it.copy(nearbyDevices = peers, isScanning = false) }
    }
}