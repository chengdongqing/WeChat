package top.chengdongqing.wechat.features.chat.ui.session.peer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.chengdongqing.wechat.data.network.connection.bluetooth.BtBondManager
import top.chengdongqing.wechat.features.chat.domain.model.PeerDevice
import top.chengdongqing.wechat.features.chat.domain.model.PeerDeviceUiState
import top.chengdongqing.wechat.features.me.domain.repository.ProfileRepository
import javax.inject.Inject

@HiltViewModel
class BluetoothPeerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val bluetoothBondManager: BtBondManager,
    private val profileRepository: ProfileRepository
) : PeerDeviceViewModel() {

    private val _uiState = MutableStateFlow(PeerDeviceUiState())
    override val uiState = _uiState.asStateFlow()

    private val bluetoothAdapter by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private var scanCallback: ScanCallback? = null

    @SuppressLint("MissingPermission")
    override fun startScan() {
        loadPairedDevices()

        val scanner = bluetoothAdapter.bluetoothLeScanner ?: run {
            _uiState.update { it.copy(isScanning = false, error = "蓝牙扫描不可用") }
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanCallback = object : ScanCallback() {
            @SuppressLint("MissingPermission")
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                addNearbyDevice(
                    PeerDevice.Bluetooth(
                        id = device.address,
                        name = device.name ?: device.address,
                        isPaired = device.bondState == BluetoothDevice.BOND_BONDED,
                        signalStrength = result.rssi,
                        device = device
                    )
                )
            }

            override fun onScanFailed(errorCode: Int) {
                _uiState.update { it.copy(isScanning = false, error = "扫描失败: $errorCode") }
            }
        }

        scanner.startScan(emptyList(), settings, scanCallback!!)
        _uiState.update { it.copy(isScanning = true, error = null) }
    }

    @SuppressLint("MissingPermission")
    override fun stopScan() {
        scanCallback?.let {
            bluetoothAdapter.bluetoothLeScanner?.stopScan(it)
            scanCallback = null
        }
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        _uiState.update { it.copy(isScanning = false) }
    }

    override fun connectDevice(device: PeerDevice, userId: String, onSuccess: () -> Unit) {
        val btDevice = (device as? PeerDevice.Bluetooth)?.device ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(connectingDeviceId = device.id, error = null) }
            val myUserId = profileRepository.getProfile()?.id ?: run {
                _uiState.update { it.copy(connectingDeviceId = null, error = "未找到个人资料") }
                return@launch
            }
            runCatching {
                bluetoothBondManager.bondAndConnect(
                    userId = userId,
                    device = btDevice,
                    myUserId = myUserId
                )
            }.onSuccess {
                _uiState.update { it.copy(connectingDeviceId = null) }
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

    @SuppressLint("MissingPermission")
    fun onClassicDeviceFound(device: BluetoothDevice, rssi: Int) {
        addNearbyDevice(
            PeerDevice.Bluetooth(
                id = device.address,
                name = device.name ?: device.address,
                isPaired = device.bondState == BluetoothDevice.BOND_BONDED,
                signalStrength = rssi,
                device = device
            )
        )
    }

    fun onDiscoveryFinished() {
        _uiState.update { it.copy(isScanning = false) }
    }

    @SuppressLint("MissingPermission")
    private fun loadPairedDevices() {
        val bonded = bluetoothAdapter.bondedDevices?.map { device ->
            PeerDevice.Bluetooth(
                id = device.address,
                name = device.name ?: device.address,
                isPaired = true,
                device = device
            )
        } ?: emptyList()
        _uiState.update { it.copy(pairedDevices = bonded) }
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}