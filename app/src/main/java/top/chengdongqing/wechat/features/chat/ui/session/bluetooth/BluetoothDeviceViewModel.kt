package top.chengdongqing.wechat.features.chat.ui.session.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.data.network.connection.bluetooth.BluetoothBondManager
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject

data class BluetoothDeviceUiState(
    val isScanning: Boolean = false,
    val pairedDevices: List<ScannedDevice> = emptyList(),
    val nearbyDevices: List<ScannedDevice> = emptyList(),
    val connectingDeviceAddress: String? = null,
    val error: String? = null
)

data class ScannedDevice(
    val device: BluetoothDevice,
    val name: String,
    val rssi: Int = 0,
    val isPaired: Boolean = false
)

@HiltViewModel
class BluetoothDeviceViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val bluetoothBondManager: BluetoothBondManager,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BluetoothDeviceUiState())
    val uiState = _uiState.asStateFlow()

    private val bluetoothAdapter by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private var scanCallback: ScanCallback? = null

    fun startScan() {
        // 开始扫描附近设备
        startClassicDiscovery()

        // 已配对设备直接展示
        loadPairedDevices()
    }

    @SuppressLint("MissingPermission")
    private fun startClassicDiscovery() {
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        bluetoothAdapter.startDiscovery()  // 触发经典蓝牙扫描
        _uiState.update { it.copy(isScanning = true, error = null) }
    }

    @SuppressLint("MissingPermission")
    private fun loadPairedDevices() {
        val bonded = bluetoothAdapter.bondedDevices?.map { device ->
            ScannedDevice(
                device = device,
                name = device.name ?: device.address,
                isPaired = true
            )
        } ?: emptyList()
        _uiState.update { it.copy(pairedDevices = bonded) }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanCallback?.let {
            bluetoothAdapter.bluetoothLeScanner?.stopScan(it)
            scanCallback = null
        }
        _uiState.update { it.copy(isScanning = false) }
    }

    fun connectDevice(scanned: ScannedDevice, userId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    connectingDeviceAddress = scanned.device.address,
                    error = null
                )
            }
            val myUserId = profileRepository.getProfile()?.id ?: run {
                _uiState.update {
                    it.copy(
                        connectingDeviceAddress = null,
                        error = "未找到个人资料"
                    )
                }
                return@launch
            }
            runCatching {
                bluetoothBondManager.bondAndConnect(
                    userId = userId,
                    device = scanned.device,
                    myUserId = myUserId
                )
            }.onSuccess {
                _uiState.update { it.copy(connectingDeviceAddress = null) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(connectingDeviceAddress = null, error = e.message ?: "连接失败")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun addDiscoveredDevice(device: BluetoothDevice, rssi: Int) {
        val scanned = ScannedDevice(
            device = device,
            name = device.name ?: device.address,
            rssi = rssi,
            isPaired = device.bondState == BluetoothDevice.BOND_BONDED
        )
        _uiState.update { state ->
            val existing = state.nearbyDevices.indexOfFirst { it.device.address == device.address }
            val updated = state.nearbyDevices.toMutableList()
            if (existing >= 0) updated[existing] = scanned else updated.add(scanned)
            state.copy(nearbyDevices = updated)
        }
    }

    fun onDiscoveryFinished() {
        _uiState.update { it.copy(isScanning = false) }
    }

    @SuppressLint("MissingPermission")
    override fun onCleared() {
        super.onCleared()
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
    }
}