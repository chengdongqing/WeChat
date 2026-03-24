package top.chengdongqing.wechat.feature.chat.ui.session.peer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.chengdongqing.wechat.core.data.model.ConnectionMode
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.divider.WeDivider
import top.chengdongqing.wechat.core.designsystem.components.popup.WePopup
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.util.LaunchedUpdateEffect
import top.chengdongqing.wechat.feature.chat.domain.model.PeerDevice
import top.chengdongqing.wechat.feature.chat.ui.session.peer.components.DeviceItem
import top.chengdongqing.wechat.feature.chat.ui.session.peer.components.EmptyView
import top.chengdongqing.wechat.feature.chat.ui.session.peer.components.OwnerWaitingView
import top.chengdongqing.wechat.feature.chat.ui.session.peer.components.ScanningIndicator
import top.chengdongqing.wechat.feature.chat.ui.session.peer.components.WiFiDirectRoleSelector
import top.chengdongqing.wechat.feature.chat.ui.session.peer.util.PeerScanEffect

@Composable
fun PeerDeviceOverlay(
    visible: Boolean,
    userId: String,
    mode: ConnectionMode,
    onConnected: () -> Unit,
    onClose: () -> Unit,
) {
    val viewModel: PeerDeviceViewModel = when (mode) {
        ConnectionMode.Bluetooth -> hiltViewModel<BluetoothPeerViewModel>()
        ConnectionMode.WiFiDirect -> hiltViewModel<WiFiDirectPeerViewModel>()
        else -> return
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedUpdateEffect(visible) {
        if (!visible) {
            viewModel.reset()
        }
    }

    WePopup(
        visible = visible,
        padding = PaddingValues(vertical = 16.dp),
        title = when (mode) {
            ConnectionMode.Bluetooth -> stringResource(R.string.conn_title_select_bluetooth_device)
            ConnectionMode.WiFiDirect -> stringResource(R.string.conn_title_select_wifi_direct_device)
        },
        onClose = onClose
    ) {
        PeerScanEffect(mode, viewModel)

        LazyColumn(modifier = Modifier.heightIn(min = 300.dp)) {
            peerDeviceListContent(
                mode = mode,
                state = state,
                viewModel = viewModel,
                userId = userId,
                onConnected = onConnected,
                onClose = onClose,
            )
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

private fun LazyListScope.peerDeviceListContent(
    mode: ConnectionMode,
    state: PeerDeviceUiState,
    viewModel: PeerDeviceViewModel,
    userId: String,
    onConnected: () -> Unit,
    onClose: () -> Unit,
) {
    if (mode == ConnectionMode.WiFiDirect) {
        val wfdViewModel = viewModel as WiFiDirectPeerViewModel
        when (state.role) {
            WiFiDirectRole.None -> {
                item {
                    WiFiDirectRoleSelector(
                        onCreateGroup = wfdViewModel::startAsOwner,
                        onJoinGroup = wfdViewModel::startAsClient,
                    )
                }
                return
            }

            WiFiDirectRole.Owner -> {
                item { OwnerWaitingView() }
                return
            }

            else -> Unit
        }
    }

    item { ScanningIndicator(isScanning = state.isScanning) }

    state.error?.let { error ->
        item {
            Text(
                text = error,
                color = WeTheme.colorScheme.danger,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    val connectDevice = { device: PeerDevice ->
        viewModel.connectDevice(device, userId) { onConnected(); onClose() }
    }

    if (state.pairedDevices.isNotEmpty()) {
        item { SectionHeader(stringResource(R.string.conn_section_paired_devices)) }
        items(state.pairedDevices, key = { "bonded_${it.id}" }) { device ->
            DeviceItem(
                device = device,
                isConnecting = state.connectingDeviceId == device.id,
                onClick = { connectDevice(device) },
            )
            WeDivider(modifier = Modifier.padding(start = 64.dp))
        }
    }

    if (state.nearbyDevices.isNotEmpty()) {
        item { SectionHeader(stringResource(R.string.conn_section_nearby_devices)) }
        items(state.nearbyDevices, key = { it.id }) { device ->
            DeviceItem(
                device = device,
                isConnecting = state.connectingDeviceId == device.id,
                onClick = { connectDevice(device) },
            )
            WeDivider(modifier = Modifier.padding(start = 64.dp))
        }
    }

    if (state.pairedDevices.isEmpty() && state.nearbyDevices.isEmpty() && !state.isScanning) {
        item { EmptyView(mode) }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        color = WeTheme.colorScheme.textSecondary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}