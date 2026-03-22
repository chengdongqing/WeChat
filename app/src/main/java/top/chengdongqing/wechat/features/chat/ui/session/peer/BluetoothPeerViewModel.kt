package top.chengdongqing.wechat.features.chat.ui.session.peer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
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
import top.chengdongqing.wechat.features.profile.domain.repository.ProfileRepository
import javax.inject.Inject

@HiltViewModel
@SuppressLint("MissingPermission")
class BluetoothPeerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val bluetoothBondManager: BtBondManager,
    private val profileRepository: ProfileRepository
) : PeerDeviceViewModel() {

    private val _uiState = MutableStateFlow(PeerDeviceUiState())
    override val uiState = _uiState.asStateFlow()

    private val nativeDeviceMap = mutableMapOf<String, BluetoothDevice>() // key为mac地址

    private val myUserId: String
        get() = profileRepository.requireUserId()
    private val bluetoothAdapter by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    override fun startScan() {
        loadPairedDevices()
        if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
        bluetoothAdapter.startDiscovery()
        _uiState.update { it.copy(isScanning = true, error = null) }
    }

    @SuppressLint("MissingPermission")
    override fun stopScan() {
        if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
        _uiState.update { it.copy(isScanning = false) }
    }

    override fun connectDevice(device: PeerDevice, userId: String, onSuccess: () -> Unit) {
        val btDevice = nativeDeviceMap[device.id] ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(connectingDeviceId = device.id, error = null) }
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
            val existing = state.nearbyDevices.indexOfFirst { it.id == device.id }
            val updated = state.nearbyDevices.toMutableList()
            if (existing >= 0) updated[existing] = device else updated.add(device)
            state.copy(nearbyDevices = updated)
        }
    }

    fun onClassicDeviceFound(device: BluetoothDevice, rssi: Int) {
        if (device.name == null) return
        nativeDeviceMap[device.address] = device
        addNearbyDevice(
            PeerDevice.Bluetooth(
                id = device.address,
                name = device.name ?: device.address,
                isPaired = device.bondState == BluetoothDevice.BOND_BONDED,
                signalStrength = rssi
            )
        )
    }

    fun onDiscoveryFinished() {
        _uiState.update { it.copy(isScanning = false) }
    }

    private fun loadPairedDevices() {
        val bonded = bluetoothAdapter.bondedDevices?.map { device ->
            nativeDeviceMap[device.address] = device
            PeerDevice.Bluetooth(
                id = device.address,
                name = device.name ?: device.address,
                isPaired = true
            )
        } ?: emptyList()
        _uiState.update { it.copy(pairedDevices = bonded) }
    }

    override fun reset() {
        nativeDeviceMap.clear()
        _uiState.value = PeerDeviceUiState()
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}